package com.jobseekercopilot.postcodeiogateway.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jobseekercopilot.postcodeiogateway.config.ExternalProviderProperties;
import com.jobseekercopilot.postcodeiogateway.config.ProviderResilienceProperties;
import com.jobseekercopilot.postcodeiogateway.logging.CorrelationIdFilter;
import com.jobseekercopilot.postcodeiogateway.model.PlaceLocation;
import com.jobseekercopilot.postcodeiogateway.model.PostcodeLocation;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.netty.channel.ChannelOption;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeoutException;
import java.util.function.LongSupplier;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.codec.CodecException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

@Component
@ConditionalOnProperty(prefix = "external-provider", name = "mode", havingValue = "LIVE")
public class PostcodeIoApiClient implements PostcodeProviderClient {
    private static final Logger log = LoggerFactory.getLogger(PostcodeIoApiClient.class);

    private final WebClient webClient;
    private final ProviderResilienceProperties properties;
    private final MeterRegistry meterRegistry;
    private final Counter retryCounter;
    private final Counter circuitOpenCounter;
    private final Counter compatibilityFailureCounter;
    private final ProviderCircuitBreaker circuitBreaker;
    private final ProviderCache cache;
    private final LongSupplier nanoTime;

    @Autowired
    public PostcodeIoApiClient(
            ExternalProviderProperties providerProperties,
            ProviderResilienceProperties resilienceProperties,
            MeterRegistry meterRegistry) {
        this(buildWebClient(providerProperties, resilienceProperties),
                resilienceProperties, meterRegistry, System::nanoTime);
    }

    PostcodeIoApiClient(
            WebClient webClient,
            ProviderResilienceProperties resilienceProperties,
            MeterRegistry meterRegistry,
            LongSupplier nanoTime) {
        resilienceProperties.validate();
        this.webClient = webClient;
        this.properties = resilienceProperties;
        this.meterRegistry = meterRegistry;
        this.nanoTime = nanoTime;
        this.retryCounter = Counter.builder("postcode.provider.retries")
                .description("Postcode provider retry attempts")
                .register(meterRegistry);
        this.circuitOpenCounter = Counter.builder("postcode.provider.circuit.opens")
                .description("Postcode provider circuit transitions to open")
                .register(meterRegistry);
        this.compatibilityFailureCounter = Counter.builder("postcode.provider.compatibility.failures")
                .description("Postcode provider responses that violate the consumed contract")
                .register(meterRegistry);
        this.circuitBreaker = new ProviderCircuitBreaker(
                resilienceProperties.getCircuitFailureThreshold(),
                resilienceProperties.getCircuitOpenDuration(),
                nanoTime);
        this.cache = new ProviderCache(resilienceProperties.getCacheMaximumEntries(), nanoTime);
        Gauge.builder("postcode.provider.circuit.state", circuitBreaker,
                        breaker -> switch (breaker.state()) {
                            case CLOSED -> 0;
                            case HALF_OPEN -> 1;
                            case OPEN -> 2;
                        })
                .description("Provider circuit state: 0 closed, 1 half-open, 2 open")
                .register(meterRegistry);
    }

    @Override
    public Mono<PostcodeLocation> fetchPostcodeDetails(String postcode) {
        String clean = postcode.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        boolean outcode = clean.length() <= 4;

        return Mono.defer(() -> {
            ProviderCache.Entry cached = cache.get(clean);
            if (cached != null) {
                cacheMetric(cached.found() ? "hit" : "negative_hit");
                return cached.found()
                        ? Mono.just(cached.location())
                        : Mono.error(new ProviderNotFoundException());
            }
            cacheMetric("miss");

            if (!circuitBreaker.tryAcquirePermission()) {
                recordOutcome("circuit_open", -1);
                return Mono.error(new ProviderCircuitOpenException());
            }

            long startedAt = nanoTime.getAsLong();
            log.info("postcodes.io lookup started lookupType={} postcodePresent={}",
                    outcode ? "OUTCODE" : "POSTCODE", postcode != null && !postcode.isBlank());

            Mono<PostcodeLocation> request = providerRequest(clean, outcode)
                    .timeout(properties.getResponseTimeout());
            if (properties.getMaxRetries() > 0) {
                request = request.retryWhen(Retry.backoff(
                                properties.getMaxRetries(), properties.getInitialBackoff())
                        .maxBackoff(properties.getMaxBackoff())
                        .jitter(properties.getJitter())
                        .filter(this::isRetryable)
                        .doBeforeRetry(signal -> retryCounter.increment())
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()));
            }

            return request.timeout(properties.getTotalTimeout())
                    .doOnNext(location -> cache.put(clean, location, properties.getSuccessCacheTtl()))
                    .doOnSuccess(location -> {
                        circuitBreaker.recordSuccess();
                        recordOutcome("success", startedAt);
                        log.info("postcodes.io lookup completed lookupType={} found=true durationMs={}",
                                outcode ? "OUTCODE" : "POSTCODE", elapsedMillis(startedAt));
                    })
                    .doOnError(error -> {
                        Throwable unwrapped = Exceptions.unwrap(error);
                        if (unwrapped instanceof ProviderNotFoundException) {
                            cache.putNegative(clean, properties.getNegativeCacheTtl());
                            circuitBreaker.recordSuccess();
                            recordOutcome("not_found", startedAt);
                        } else {
                            if (isCircuitFailure(unwrapped) && circuitBreaker.recordFailure()) {
                                circuitOpenCounter.increment();
                            }
                            recordOutcome("failure", startedAt);
                        }
                        log.warn("postcodes.io lookup failed lookupType={} durationMs={} error={}",
                                outcode ? "OUTCODE" : "POSTCODE",
                                elapsedMillis(startedAt),
                                unwrapped.getClass().getSimpleName());
                    });
        });
    }

    @Override
    public Mono<List<PlaceLocation>> searchPlaces(String query, int limit) {
        return Mono.defer(() -> {
            if (!circuitBreaker.tryAcquirePermission()) {
                recordOutcome("circuit_open", -1);
                return Mono.error(new ProviderCircuitOpenException());
            }

            long startedAt = nanoTime.getAsLong();
            log.info("postcodes.io place search started queryPresent={}",
                    query != null && !query.isBlank());

            Mono<List<PlaceLocation>> request = placeSearchRequest(query, limit)
                    .timeout(properties.getResponseTimeout());
            if (properties.getMaxRetries() > 0) {
                request = request.retryWhen(Retry.backoff(
                                properties.getMaxRetries(), properties.getInitialBackoff())
                        .maxBackoff(properties.getMaxBackoff())
                        .jitter(properties.getJitter())
                        .filter(this::isRetryable)
                        .doBeforeRetry(signal -> retryCounter.increment())
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()));
            }

            return request.timeout(properties.getTotalTimeout())
                    .doOnSuccess(locations -> {
                        circuitBreaker.recordSuccess();
                        recordOutcome("success", startedAt);
                        log.info("postcodes.io place search completed resultCount={} durationMs={}",
                                locations.size(), elapsedMillis(startedAt));
                    })
                    .doOnError(error -> {
                        Throwable unwrapped = Exceptions.unwrap(error);
                        if (isCircuitFailure(unwrapped) && circuitBreaker.recordFailure()) {
                            circuitOpenCounter.increment();
                        }
                        recordOutcome("failure", startedAt);
                        log.warn("postcodes.io place search failed durationMs={} error={}",
                                elapsedMillis(startedAt), unwrapped.getClass().getSimpleName());
                    });
        });
    }

    @Override
    public boolean isReady() {
        return circuitBreaker.state() != ProviderCircuitBreaker.State.OPEN;
    }

    private Mono<List<PlaceLocation>> placeSearchRequest(String query, int limit) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.pathSegment("places")
                        .queryParam("q", query)
                        .queryParam("limit", limit)
                        .build())
                .retrieve()
                .bodyToMono(PlaceResponse.class)
                .switchIfEmpty(Mono.error(new ProviderResponseException()))
                .flatMap(response -> compatiblePlaceResponse(response, limit));
    }

    private Mono<List<PlaceLocation>> compatiblePlaceResponse(PlaceResponse response, int limit) {
        if (response.getStatus() == null
                || response.getStatus() != 200
                || response.getResult() == null
                || response.getResult().size() > limit) {
            return incompatibleProviderResponse();
        }
        List<PlaceLocation> locations = new ArrayList<>();
        for (PlaceResult result : response.getResult()) {
            if (result == null
                    || !boundedText(result.getCode(), 128)
                    || !boundedText(result.getName(), 200)
                    || !boundedText(result.getOutcode(), 8)
                    || !boundedText(result.getRegion(), 100)
                    || (StringUtils.hasText(result.getDistrictBorough())
                        && !boundedText(result.getDistrictBorough(), 200))
                    || (StringUtils.hasText(result.getCountyUnitary())
                        && !boundedText(result.getCountyUnitary(), 200))
                    || !boundedCoordinate(result.getLatitude(), -90, 90)
                    || !boundedCoordinate(result.getLongitude(), -180, 180)) {
                return incompatibleProviderResponse();
            }
            String adminDistrict = StringUtils.hasText(result.getDistrictBorough())
                    ? result.getDistrictBorough()
                    : result.getCountyUnitary();
            locations.add(new PlaceLocation(
                    result.getCode(),
                    result.getName(),
                    result.getOutcode(),
                    result.getRegion(),
                    adminDistrict,
                    result.getLatitude(),
                    result.getLongitude()));
        }
        return Mono.just(List.copyOf(locations));
    }

    private static boolean boundedText(String value, int maximumLength) {
        return StringUtils.hasText(value) && value.length() <= maximumLength;
    }

    private static boolean boundedCoordinate(Double value, double minimum, double maximum) {
        return value != null && Double.isFinite(value) && value >= minimum && value <= maximum;
    }

    private Mono<PostcodeLocation> providerRequest(String clean, boolean outcode) {
        if (outcode) {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder.pathSegment("outcodes", clean).build())
                    .retrieve()
                    .onStatus(status -> status.value() == 404, response ->
                            response.releaseBody().then(Mono.error(new ProviderNotFoundException())))
                    .bodyToMono(OutcodeResponse.class)
                    .switchIfEmpty(Mono.error(new ProviderResponseException()))
                    .flatMap(response -> compatibleOutcodeResponse(response, clean));
        }
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.pathSegment("postcodes", clean).build())
                .retrieve()
                .onStatus(status -> status.value() == 404, response ->
                        response.releaseBody().then(Mono.error(new ProviderNotFoundException())))
                .bodyToMono(PostcodeResponse.class)
                .switchIfEmpty(Mono.error(new ProviderResponseException()))
                .flatMap(response -> compatiblePostcodeResponse(response, clean));
    }

    private Mono<PostcodeLocation> compatiblePostcodeResponse(PostcodeResponse response, String expectedPostcode) {
        if (response.getStatus() == null
                || response.getStatus() != 200
                || response.getResult() == null
                || !StringUtils.hasText(response.getResult().getPostcode())
                || !normaliseIdentity(response.getResult().getPostcode()).equals(expectedPostcode)) {
            return incompatibleProviderResponse();
        }
        return Mono.just(response.getResult());
    }

    private Mono<PostcodeLocation> compatibleOutcodeResponse(OutcodeResponse response, String expectedOutcode) {
        if (response.getStatus() == null
                || response.getStatus() != 200
                || response.getResult() == null
                || !StringUtils.hasText(response.getResult().getOutcode())
                || !normaliseIdentity(response.getResult().getOutcode()).equals(expectedOutcode)) {
            return incompatibleProviderResponse();
        }
        return Mono.just(toLocation(response.getResult()));
    }

    private <T> Mono<T> incompatibleProviderResponse() {
        compatibilityFailureCounter.increment();
        return Mono.error(new ProviderResponseException());
    }

    private static String normaliseIdentity(String value) {
        return value.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    private PostcodeLocation toLocation(OutcodeResult result) {
        PostcodeLocation location = new PostcodeLocation();
        location.setPostcode(result.getOutcode());
        if (result.getRegion() != null && !result.getRegion().isEmpty()) {
            location.setRegion(result.getRegion().get(0));
        }
        if (result.getAdminDistrict() != null && !result.getAdminDistrict().isEmpty()) {
            location.setAdminDistrict(result.getAdminDistrict().get(0));
        }
        if (result.getCountry() != null && !result.getCountry().isEmpty()) {
            location.setCountry(result.getCountry().get(0));
        }
        return location;
    }

    private boolean isRetryable(Throwable error) {
        Throwable unwrapped = Exceptions.unwrap(error);
        if (unwrapped instanceof TimeoutException
                || unwrapped instanceof WebClientRequestException
                || unwrapped instanceof CodecException
                || unwrapped instanceof ProviderResponseException) {
            return true;
        }
        if (unwrapped instanceof WebClientResponseException responseException) {
            HttpStatusCode status = responseException.getStatusCode();
            return status.is5xxServerError()
                    || status.value() == 408
                    || status.value() == 425
                    || status.value() == 429;
        }
        return false;
    }

    private boolean isCircuitFailure(Throwable error) {
        return isRetryable(error);
    }

    private void recordOutcome(String outcome, long startedAt) {
        Counter.builder("postcode.provider.requests")
                .description("Logical postcode provider requests")
                .tag("outcome", outcome)
                .register(meterRegistry)
                .increment();
        if (startedAt >= 0) {
            Timer.builder("postcode.provider.latency")
                    .description("Logical postcode provider request latency")
                    .tag("outcome", outcome)
                    .register(meterRegistry)
                    .record(Duration.ofNanos(Math.max(0, nanoTime.getAsLong() - startedAt)));
        }
    }

    private void cacheMetric(String result) {
        Counter.builder("postcode.provider.cache.accesses")
                .description("Postcode provider cache accesses")
                .tag("result", result)
                .register(meterRegistry)
                .increment();
    }

    private long elapsedMillis(long startedAt) {
        return Math.max(0, nanoTime.getAsLong() - startedAt) / 1_000_000;
    }

    private static WebClient buildWebClient(
            ExternalProviderProperties providerProperties,
            ProviderResilienceProperties resilienceProperties) {
        resilienceProperties.validate();
        if (!StringUtils.hasText(providerProperties.getBaseUrl())) {
            throw new IllegalStateException("EXTERNAL_PROVIDER_BASE_URL must be set for LIVE mode.");
        }
        URI baseUri = URI.create(providerProperties.getBaseUrl());
        if (!("http".equalsIgnoreCase(baseUri.getScheme()) || "https".equalsIgnoreCase(baseUri.getScheme()))) {
            throw new IllegalStateException("EXTERNAL_PROVIDER_BASE_URL must use http or https.");
        }
        if (!StringUtils.hasText(baseUri.getHost())
                || baseUri.getUserInfo() != null
                || baseUri.getQuery() != null
                || baseUri.getFragment() != null
                || (StringUtils.hasText(baseUri.getPath()) && !"/".equals(baseUri.getPath()))) {
            throw new IllegalStateException("EXTERNAL_PROVIDER_BASE_URL must be an origin without credentials, path, query or fragment.");
        }
        long connectTimeoutMillis = resilienceProperties.getConnectTimeout().toMillis();
        if (connectTimeoutMillis < 1 || connectTimeoutMillis > Integer.MAX_VALUE) {
            throw new IllegalStateException("Provider connect timeout must be between one millisecond and Integer.MAX_VALUE milliseconds.");
        }
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectTimeoutMillis)
                .responseTimeout(resilienceProperties.getResponseTimeout());
        return WebClient.builder()
                .baseUrl(baseUri.toString())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter((request, next) -> {
                    String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
                    if (StringUtils.hasText(correlationId)) {
                        return next.exchange(ClientRequest.from(request)
                                .header(CorrelationIdFilter.HEADER_NAME, correlationId)
                                .build());
                    }
                    return next.exchange(request);
                })
                .build();
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PostcodeResponse {
        private Integer status;
        private PostcodeLocation result;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class OutcodeResponse {
        private Integer status;
        private OutcodeResult result;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class OutcodeResult {
        private String outcode;
        private List<String> region;
        @JsonProperty("admin_district")
        private List<String> adminDistrict;
        private List<String> country;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PlaceResponse {
        private Integer status;
        private List<PlaceResult> result;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PlaceResult {
        private String code;
        @JsonProperty("name_1")
        private String name;
        private String outcode;
        private String region;
        @JsonProperty("district_borough")
        private String districtBorough;
        @JsonProperty("county_unitary")
        private String countyUnitary;
        private Double latitude;
        private Double longitude;
    }
}
