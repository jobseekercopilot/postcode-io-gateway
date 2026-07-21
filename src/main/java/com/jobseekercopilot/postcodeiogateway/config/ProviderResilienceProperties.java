package com.jobseekercopilot.postcodeiogateway.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "external-provider.resilience")
public class ProviderResilienceProperties {
    private Duration connectTimeout = Duration.ofMillis(500);
    private Duration responseTimeout = Duration.ofSeconds(1);
    private Duration totalTimeout = Duration.ofSeconds(4);
    private int maxRetries = 2;
    private Duration initialBackoff = Duration.ofMillis(100);
    private Duration maxBackoff = Duration.ofMillis(500);
    private double jitter = 0.5;
    private int circuitFailureThreshold = 5;
    private Duration circuitOpenDuration = Duration.ofSeconds(30);
    private Duration successCacheTtl = Duration.ofMinutes(15);
    private Duration negativeCacheTtl = Duration.ofMinutes(1);
    private int cacheMaximumEntries = 1_000;

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getResponseTimeout() {
        return responseTimeout;
    }

    public void setResponseTimeout(Duration responseTimeout) {
        this.responseTimeout = responseTimeout;
    }

    public Duration getTotalTimeout() {
        return totalTimeout;
    }

    public void setTotalTimeout(Duration totalTimeout) {
        this.totalTimeout = totalTimeout;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Duration getInitialBackoff() {
        return initialBackoff;
    }

    public void setInitialBackoff(Duration initialBackoff) {
        this.initialBackoff = initialBackoff;
    }

    public Duration getMaxBackoff() {
        return maxBackoff;
    }

    public void setMaxBackoff(Duration maxBackoff) {
        this.maxBackoff = maxBackoff;
    }

    public double getJitter() {
        return jitter;
    }

    public void setJitter(double jitter) {
        this.jitter = jitter;
    }

    public int getCircuitFailureThreshold() {
        return circuitFailureThreshold;
    }

    public void setCircuitFailureThreshold(int circuitFailureThreshold) {
        this.circuitFailureThreshold = circuitFailureThreshold;
    }

    public Duration getCircuitOpenDuration() {
        return circuitOpenDuration;
    }

    public void setCircuitOpenDuration(Duration circuitOpenDuration) {
        this.circuitOpenDuration = circuitOpenDuration;
    }

    public Duration getSuccessCacheTtl() {
        return successCacheTtl;
    }

    public void setSuccessCacheTtl(Duration successCacheTtl) {
        this.successCacheTtl = successCacheTtl;
    }

    public Duration getNegativeCacheTtl() {
        return negativeCacheTtl;
    }

    public void setNegativeCacheTtl(Duration negativeCacheTtl) {
        this.negativeCacheTtl = negativeCacheTtl;
    }

    public int getCacheMaximumEntries() {
        return cacheMaximumEntries;
    }

    public void setCacheMaximumEntries(int cacheMaximumEntries) {
        this.cacheMaximumEntries = cacheMaximumEntries;
    }

    public void validate() {
        requirePositive(connectTimeout, "Provider connect timeout");
        requirePositive(responseTimeout, "Provider response timeout");
        requirePositive(totalTimeout, "Provider total timeout");
        requirePositive(initialBackoff, "Provider initial retry backoff");
        requirePositive(maxBackoff, "Provider maximum retry backoff");
        requirePositive(circuitOpenDuration, "Provider circuit open duration");
        requirePositive(successCacheTtl, "Provider success cache TTL");
        requirePositive(negativeCacheTtl, "Provider negative cache TTL");
        requireAtMost(connectTimeout, Duration.ofSeconds(10), "Provider connect timeout");
        requireAtMost(responseTimeout, Duration.ofSeconds(30), "Provider response timeout");
        requireAtMost(totalTimeout, Duration.ofSeconds(60), "Provider total timeout");
        requireAtMost(initialBackoff, Duration.ofSeconds(10), "Provider initial retry backoff");
        requireAtMost(maxBackoff, Duration.ofSeconds(10), "Provider maximum retry backoff");
        requireAtMost(circuitOpenDuration, Duration.ofMinutes(10), "Provider circuit open duration");
        requireAtMost(successCacheTtl, Duration.ofHours(24), "Provider success cache TTL");
        requireAtMost(negativeCacheTtl, Duration.ofHours(1), "Provider negative cache TTL");
        if (totalTimeout.compareTo(responseTimeout) < 0) {
            throw new IllegalStateException("Provider total timeout cannot be shorter than the response timeout.");
        }
        if (maxBackoff.compareTo(initialBackoff) < 0) {
            throw new IllegalStateException("Provider maximum retry backoff cannot be shorter than the initial backoff.");
        }
        if (maxRetries < 0 || maxRetries > 5) {
            throw new IllegalStateException("Provider max retries must be between zero and five.");
        }
        if (jitter < 0 || jitter > 1) {
            throw new IllegalStateException("Provider retry jitter must be between zero and one.");
        }
        if (circuitFailureThreshold < 1 || circuitFailureThreshold > 100) {
            throw new IllegalStateException("Provider circuit failure threshold must be between one and 100.");
        }
        if (cacheMaximumEntries < 1 || cacheMaximumEntries > 100_000) {
            throw new IllegalStateException("Provider cache maximum entries must be between one and 100,000.");
        }
    }

    private static void requirePositive(Duration duration, String name) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalStateException(name + " must be positive.");
        }
    }

    private static void requireAtMost(Duration duration, Duration maximum, String name) {
        if (duration.compareTo(maximum) > 0) {
            throw new IllegalStateException(name + " exceeds the safe maximum of " + maximum + ".");
        }
    }
}
