package com.jobseekercopilot.postcodeiogateway.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jobseekercopilot.postcodeiogateway.config.ExternalProviderProperties;
import com.jobseekercopilot.postcodeiogateway.config.ProviderResilienceProperties;
import com.jobseekercopilot.postcodeiogateway.model.PostcodeLocation;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
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
            {"result":{"postcode":"LS1 1UR","country":"England","region":"Yorkshire and The Humber"}}
            """;

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
                    ? response(HttpStatus.OK, SUCCESS_BODY)
                    : response(HttpStatus.SERVICE_UNAVAILABLE, "{}");
        }, properties, meters, nanoTime);

        assertThatThrownBy(() -> client.fetchPostcodeDetails("LS1 1UR").block()).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> client.fetchPostcodeDetails("LS2 2UR").block()).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> client.fetchPostcodeDetails("LS3 3UR").block())
                .isInstanceOf(ProviderCircuitOpenException.class);
        assertThat(calls).hasValue(2);
        assertThat(meters.get("postcode.provider.circuit.state").gauge().value()).isEqualTo(2);
        assertThat(meters.get("postcode.provider.circuit.opens").counter().count()).isEqualTo(1);
        assertThat(meters.get("postcode.provider.requests").tag("outcome", "circuit_open").counter().count())
                .isEqualTo(1);

        nanoTime.addAndGet(11);
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
            return response(HttpStatus.OK, SUCCESS_BODY);
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
}
