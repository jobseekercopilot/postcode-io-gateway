package com.jobseekercopilot.postcodeiogateway.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jobseekercopilot.postcodeiogateway.config.ExternalProviderProperties;
import com.jobseekercopilot.postcodeiogateway.config.ProviderResilienceProperties;
import com.jobseekercopilot.postcodeiogateway.model.PostcodeLocation;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class PostcodeIoApiClientTest {
    private static final String SUCCESS_BODY = """
            {"status":200,"result":{"postcode":"LS1 1UR","country":"England","region":"Yorkshire and The Humber","latitude":53.7965,"longitude":-1.5478}}
            """;
    private static final String OUTCODE_SUCCESS_BODY = """
            {"status":200,"result":{"outcode":"LS1","country":["England"],"region":["Yorkshire and The Humber"],"latitude":53.7976,"longitude":-1.5480,"future_field":"ignored"},"future_envelope_field":true}
            """;
    private static final String PLACE_SUCCESS_BODY = """
            {"status":200,"result":[
              {"code":"place-1","name_1":"St Albans","outcode":"AL1","region":"East of England","district_borough":"St Albans","latitude":51.7527,"longitude":-0.3394,"future_field":"ignored"},
              {"code":"place-2","name_1":"St Albans Road","outcode":"WD24","region":"East of England","county_unitary":"Hertfordshire","latitude":51.68,"longitude":-0.4}
            ],"future_envelope_field":true}
            """;

    @Test
    void searchesPlacesWithEncodedBoundedQueryAndMapsRequiredFields() {
        AtomicReference<String> rawPath = new AtomicReference<>();
        AtomicReference<String> rawQuery = new AtomicReference<>();
        PostcodeIoApiClient client = client(request -> {
            rawPath.set(request.url().getRawPath());
            rawQuery.set(request.url().getRawQuery());
            return response(HttpStatus.OK, PLACE_SUCCESS_BODY);
        }, properties(), new SimpleMeterRegistry(), new AtomicLong());

        var results = client.searchPlaces("St Albans", 2).block();

        assertThat(rawPath).hasValue("/places");
        assertThat(rawQuery.get()).isEqualTo("q=St%20Albans&limit=2");
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getId()).isEqualTo("place-1");
        assertThat(results.get(0).getName()).isEqualTo("St Albans");
        assertThat(results.get(0).getPostcode()).isEqualTo("AL1");
        assertThat(results.get(1).getAdminDistrict()).isEqualTo("Hertfordshire");
    }

    @Test
    void acceptsAnEmptyPlaceResultAndRejectsIncompatibleOrExcessiveResults() {
        ProviderResilienceProperties properties = properties();
        properties.setMaxRetries(0);
        PostcodeIoApiClient emptyClient = client(request -> response(HttpStatus.OK,
                "{\"status\":200,\"result\":[]}"), properties,
                new SimpleMeterRegistry(), new AtomicLong());
        assertThat(emptyClient.searchPlaces("Nowhere", 10).block()).isEmpty();

        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        PostcodeIoApiClient malformedClient = client(request -> response(HttpStatus.OK,
                "{\"status\":200,\"result\":[{\"code\":\"place-1\",\"name_1\":\"Leeds\"}]}"),
                properties, meters, new AtomicLong());
        assertThatThrownBy(() -> malformedClient.searchPlaces("Leeds", 1).block())
                .isInstanceOf(ProviderResponseException.class);
        assertThat(meters.get("postcode.provider.compatibility.failures").counter().count())
                .isEqualTo(1);
    }

    @Test
    void retriesRateLimitedPlaceSearchWithinTheConfiguredBound() {
        AtomicInteger calls = new AtomicInteger();
        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        PostcodeIoApiClient client = client(request -> calls.incrementAndGet() == 1
                ? response(HttpStatus.TOO_MANY_REQUESTS, "{}")
                : response(HttpStatus.OK, "{\"status\":200,\"result\":[]}"),
                properties(), meters, new AtomicLong());

        assertThat(client.searchPlaces("Leeds", 10).block(Duration.ofSeconds(2))).isEmpty();
        assertThat(calls).hasValue(2);
        assertThat(meters.get("postcode.provider.retries").counter().count()).isEqualTo(1);
    }

    @Test
    void appliesPerAttemptTimeoutToPlaceSearchAndStopsAfterBoundedRetries() {
        AtomicInteger calls = new AtomicInteger();
        ProviderResilienceProperties properties = properties();
        properties.setResponseTimeout(Duration.ofMillis(20));
        properties.setTotalTimeout(Duration.ofMillis(250));
        PostcodeIoApiClient client = client(request -> {
            calls.incrementAndGet();
            return Mono.never();
        }, properties, new SimpleMeterRegistry(), new AtomicLong());

        assertThatThrownBy(() -> client.searchPlaces("Leeds", 10).block(Duration.ofSeconds(2)))
                .hasRootCauseInstanceOf(TimeoutException.class);
        assertThat(calls).hasValue(3);
    }

    @Test
    void buildsProviderPathsFromIsolatedEncodedSegments() {
        AtomicReference<String> rawPath = new AtomicReference<>();
        PostcodeIoApiClient client = client(request -> {
            rawPath.set(request.url().getRawPath());
            return response(HttpStatus.OK, SUCCESS_BODY);
        }, properties(), new SimpleMeterRegistry(), new AtomicLong());

        assertThat(client.fetchPostcodeDetails("LS1 1UR").block()).isNotNull();

        assertThat(rawPath).hasValue("/postcodes/LS11UR");
    }

    @Test
    void consumesTheDocumentedOutcodeFixtureAndIgnoresAdditiveFields() {
        AtomicReference<String> rawPath = new AtomicReference<>();
        PostcodeIoApiClient client = client(request -> {
            rawPath.set(request.url().getRawPath());
            return response(HttpStatus.OK, OUTCODE_SUCCESS_BODY);
        }, properties(), new SimpleMeterRegistry(), new AtomicLong());

        PostcodeLocation result = client.fetchPostcodeDetails("LS1").block();

        assertThat(result).isNotNull();
        assertThat(result.getPostcode()).isEqualTo("LS1");
        assertThat(result.getCountry()).isEqualTo("England");
        assertThat(result.getLatitude()).isEqualTo(53.7976);
        assertThat(result.getLongitude()).isEqualTo(-1.5480);
        assertThat(rawPath).hasValue("/outcodes/LS1");
    }

    @Test
    void rejectsAnOutcodeResponseWithoutCanonicalCoordinates() {
        ProviderResilienceProperties properties = properties();
        properties.setMaxRetries(0);
        PostcodeIoApiClient client = client(request -> response(HttpStatus.OK,
                "{\"status\":200,\"result\":{\"outcode\":\"LS1\"}}"),
                properties, new SimpleMeterRegistry(), new AtomicLong());

        assertThatThrownBy(() -> client.fetchPostcodeDetails("LS1").block())
                .isInstanceOf(ProviderResponseException.class);
    }

    @Test
    void rejectsAFullPostcodeResponseWithoutCanonicalCoordinates() {
        ProviderResilienceProperties properties = properties();
        properties.setMaxRetries(0);
        PostcodeIoApiClient client = client(request -> response(HttpStatus.OK,
                "{\"status\":200,\"result\":{\"postcode\":\"LS1 1UR\"}}"),
                properties, new SimpleMeterRegistry(), new AtomicLong());

        assertThatThrownBy(() -> client.fetchPostcodeDetails("LS1 1UR").block())
                .isInstanceOf(ProviderResponseException.class);
    }

    @Test
    void rejectsAndSignalsProviderSchemaDrift() {
        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        PostcodeIoApiClient client = client(request -> response(HttpStatus.OK,
                "{\"status\":200,\"result\":{\"renamed_postcode\":\"LS1 1UR\"}}"),
                properties(), meters, new AtomicLong());

        assertThatThrownBy(() -> client.fetchPostcodeDetails("LS1 1UR").block())
                .isInstanceOf(ProviderResponseException.class);
        assertThat(meters.get("postcode.provider.compatibility.failures").counter().count())
                .isEqualTo(3);
    }

    @Test
    void rejectsAProviderResultForADifferentPostcode() {
        ProviderResilienceProperties properties = properties();
        properties.setMaxRetries(0);
        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        PostcodeIoApiClient client = client(request -> response(HttpStatus.OK,
                "{\"status\":200,\"result\":{\"postcode\":\"SW1A 1AA\"}}"),
                properties, meters, new AtomicLong());

        assertThatThrownBy(() -> client.fetchPostcodeDetails("LS1 1UR").block())
                .isInstanceOf(ProviderResponseException.class);
        assertThat(meters.get("postcode.provider.compatibility.failures").counter().count())
                .isEqualTo(1);
    }

    @Test
    void retriesRateLimitResponsesWithinTheConfiguredBound() {
        AtomicInteger calls = new AtomicInteger();
        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        PostcodeIoApiClient client = client(request -> calls.incrementAndGet() < 3
                ? response(HttpStatus.TOO_MANY_REQUESTS, "{}")
                : response(HttpStatus.OK, SUCCESS_BODY), properties(), meters, new AtomicLong());

        PostcodeLocation result = client.fetchPostcodeDetails("LS1 1UR").block(Duration.ofSeconds(2));

        assertThat(result).isNotNull();
        assertThat(result.getPostcode()).isEqualTo("LS1 1UR");
        assertThat(calls).hasValue(3);
        assertThat(meters.get("postcode.provider.retries").counter().count()).isEqualTo(2);
        assertThat(meters.get("postcode.provider.requests").tag("outcome", "success").counter().count())
                .isEqualTo(1);
        assertThat(meters.get("postcode.provider.latency").tag("outcome", "success").timer().count())
                .isEqualTo(1);
    }

    @Test
    void appliesPerAttemptTimeoutAndStopsAfterBoundedRetries() {
        AtomicInteger calls = new AtomicInteger();
        ProviderResilienceProperties properties = properties();
        properties.setResponseTimeout(Duration.ofMillis(20));
        properties.setTotalTimeout(Duration.ofMillis(250));
        PostcodeIoApiClient client = client(request -> {
            calls.incrementAndGet();
            return Mono.never();
        }, properties, new SimpleMeterRegistry(), new AtomicLong());

        assertThatThrownBy(() -> client.fetchPostcodeDetails("LS1 1UR").block(Duration.ofSeconds(2)))
                .hasRootCauseInstanceOf(TimeoutException.class);
        assertThat(calls).hasValue(3);
    }

    @Test
    void opensTheCircuitOnOutageAndAllowsOneRecoveryProbe() {
        AtomicInteger calls = new AtomicInteger();
        AtomicBoolean healthy = new AtomicBoolean();
        AtomicLong nanoTime = new AtomicLong();
        ProviderResilienceProperties properties = properties();
        properties.setMaxRetries(0);
        properties.setCircuitFailureThreshold(2);
        properties.setCircuitOpenDuration(Duration.ofNanos(10));
        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        PostcodeIoApiClient client = client(request -> {
            calls.incrementAndGet();
            return healthy.get()
                    ? response(HttpStatus.OK, successBody("LS4 4UR"))
                    : response(HttpStatus.SERVICE_UNAVAILABLE, "{}");
        }, properties, meters, nanoTime);

        assertThatThrownBy(() -> client.fetchPostcodeDetails("LS1 1UR").block()).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> client.fetchPostcodeDetails("LS2 2UR").block()).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> client.fetchPostcodeDetails("LS3 3UR").block())
                .isInstanceOf(ProviderCircuitOpenException.class);
        assertThat(calls).hasValue(2);
        assertThat(meters.get("postcode.provider.circuit.state").gauge().value()).isEqualTo(2);
        assertThat(meters.get("postcode.provider.circuit.opens").counter().count()).isEqualTo(1);
        assertThat(client.isReady()).isFalse();
        assertThat(meters.get("postcode.provider.requests").tag("outcome", "circuit_open").counter().count())
                .isEqualTo(1);

        nanoTime.addAndGet(11);
        assertThat(client.isReady()).isTrue();
        healthy.set(true);
        assertThat(client.fetchPostcodeDetails("LS4 4UR").block()).isNotNull();
        assertThat(calls).hasValue(3);
        assertThat(meters.get("postcode.provider.circuit.state").gauge().value()).isZero();
    }

    @Test
    void boundsPositiveAndNegativeCachesWithSeparateTtls() {
        AtomicInteger calls = new AtomicInteger();
        AtomicLong nanoTime = new AtomicLong();
        ProviderResilienceProperties properties = properties();
        properties.setSuccessCacheTtl(Duration.ofNanos(20));
        properties.setNegativeCacheTtl(Duration.ofNanos(10));
        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        PostcodeIoApiClient client = client(request -> {
            calls.incrementAndGet();
            return request.url().getPath().contains("MISSING")
                    ? response(HttpStatus.NOT_FOUND, "{}")
                    : response(HttpStatus.OK, SUCCESS_BODY);
        }, properties, meters, nanoTime);

        assertThat(client.fetchPostcodeDetails("LS1 1UR").block()).isNotNull();
        assertThat(client.fetchPostcodeDetails("LS1 1UR").block()).isNotNull();
        assertThatThrownBy(() -> client.fetchPostcodeDetails("MISSING").block())
                .isInstanceOf(ProviderNotFoundException.class);
        assertThatThrownBy(() -> client.fetchPostcodeDetails("MISSING").block())
                .isInstanceOf(ProviderNotFoundException.class);
        assertThat(calls).hasValue(2);

        nanoTime.addAndGet(11);
        assertThatThrownBy(() -> client.fetchPostcodeDetails("MISSING").block())
                .isInstanceOf(ProviderNotFoundException.class);
        assertThat(calls).hasValue(3);
        nanoTime.addAndGet(10);
        assertThat(client.fetchPostcodeDetails("LS1 1UR").block()).isNotNull();
        assertThat(calls).hasValue(4);
        assertThat(meters.get("postcode.provider.cache.accesses").tag("result", "hit").counter().count())
                .isEqualTo(1);
        assertThat(meters.get("postcode.provider.cache.accesses").tag("result", "negative_hit").counter().count())
                .isEqualTo(1);
    }

    @Test
    void rejectsUnsafeResilienceConfiguration() {
        ProviderResilienceProperties properties = properties();
        properties.setMaxRetries(6);

        assertThatThrownBy(() -> client(request -> response(HttpStatus.OK, SUCCESS_BODY),
                properties, new SimpleMeterRegistry(), new AtomicLong()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("max retries");
    }

    @Test
    void createsTheLiveClientWithAnExplicitTrustedHttpOrigin() {
        ExternalProviderProperties provider = new ExternalProviderProperties();
        provider.setBaseUrl("http://provider.test");

        assertThat(new PostcodeIoApiClient(provider, properties(), new SimpleMeterRegistry())).isNotNull();

        provider.setBaseUrl("file:///tmp/provider");
        assertThatThrownBy(() -> new PostcodeIoApiClient(provider, properties(), new SimpleMeterRegistry()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("http or https");

        provider.setBaseUrl("https://api.postcodes.io/untrusted-base-path");
        assertThatThrownBy(() -> new PostcodeIoApiClient(provider, properties(), new SimpleMeterRegistry()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be an origin");
    }

    @Test
    void evictsTheLeastRecentlyUsedEntryAtTheConfiguredBound() {
        AtomicInteger calls = new AtomicInteger();
        ProviderResilienceProperties properties = properties();
        properties.setCacheMaximumEntries(1);
        PostcodeIoApiClient client = client(request -> {
            calls.incrementAndGet();
            String compactPostcode = request.url().getPath().substring("/postcodes/".length());
            String postcode = compactPostcode.substring(0, compactPostcode.length() - 3)
                    + " " + compactPostcode.substring(compactPostcode.length() - 3);
            return response(HttpStatus.OK, successBody(postcode));
        }, properties, new SimpleMeterRegistry(), new AtomicLong());

        assertThat(client.fetchPostcodeDetails("LS1 1UR").block()).isNotNull();
        assertThat(client.fetchPostcodeDetails("LS2 2UR").block()).isNotNull();
        assertThat(client.fetchPostcodeDetails("LS1 1UR").block()).isNotNull();

        assertThat(calls).hasValue(3);
    }

    private static PostcodeIoApiClient client(
            ExchangeFunction exchangeFunction,
            ProviderResilienceProperties properties,
            SimpleMeterRegistry meters,
            AtomicLong nanoTime) {
        WebClient webClient = WebClient.builder()
                .baseUrl("http://provider.test")
                .exchangeFunction(exchangeFunction)
                .build();
        return new PostcodeIoApiClient(webClient, properties, meters, nanoTime::get);
    }

    private static ProviderResilienceProperties properties() {
        ProviderResilienceProperties properties = new ProviderResilienceProperties();
        properties.setConnectTimeout(Duration.ofMillis(20));
        properties.setResponseTimeout(Duration.ofMillis(500));
        properties.setTotalTimeout(Duration.ofSeconds(2));
        properties.setMaxRetries(2);
        properties.setInitialBackoff(Duration.ofMillis(1));
        properties.setMaxBackoff(Duration.ofMillis(2));
        properties.setJitter(0);
        properties.setCircuitFailureThreshold(5);
        properties.setCircuitOpenDuration(Duration.ofSeconds(1));
        properties.setSuccessCacheTtl(Duration.ofSeconds(1));
        properties.setNegativeCacheTtl(Duration.ofSeconds(1));
        properties.setCacheMaximumEntries(10);
        return properties;
    }

    private static Mono<ClientResponse> response(HttpStatus status, String body) {
        return Mono.just(ClientResponse.create(status)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build());
    }

    private static String successBody(String postcode) {
        return "{\"status\":200,\"result\":{\"postcode\":\"" + postcode
                + "\",\"latitude\":53.7965,\"longitude\":-1.5478}}";
    }
}
