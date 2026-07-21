package com.jobseekercopilot.postcodeiogateway.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class ProviderCircuitBreakerTest {
    @Test
    void permitsOnlyOneHalfOpenProbeAndReopensOnFailure() {
        AtomicLong nanoTime = new AtomicLong();
        ProviderCircuitBreaker breaker = new ProviderCircuitBreaker(1, Duration.ofNanos(10), nanoTime::get);

        assertThat(breaker.tryAcquirePermission()).isTrue();
        assertThat(breaker.recordFailure()).isTrue();
        assertThat(breaker.tryAcquirePermission()).isFalse();

        nanoTime.addAndGet(10);
        assertThat(breaker.tryAcquirePermission()).isTrue();
        assertThat(breaker.tryAcquirePermission()).isFalse();
        assertThat(breaker.recordFailure()).isTrue();
        assertThat(breaker.state()).isEqualTo(ProviderCircuitBreaker.State.OPEN);
    }

    @Test
    void recoveryClosesTheCircuitAndResetsConsecutiveFailures() {
        AtomicLong nanoTime = new AtomicLong();
        ProviderCircuitBreaker breaker = new ProviderCircuitBreaker(2, Duration.ofNanos(10), nanoTime::get);

        assertThat(breaker.tryAcquirePermission()).isTrue();
        assertThat(breaker.recordFailure()).isFalse();
        assertThat(breaker.recordSuccess()).isFalse();
        assertThat(breaker.recordFailure()).isFalse();
        assertThat(breaker.recordFailure()).isTrue();

        nanoTime.addAndGet(10);
        assertThat(breaker.tryAcquirePermission()).isTrue();
        assertThat(breaker.recordSuccess()).isTrue();
        assertThat(breaker.state()).isEqualTo(ProviderCircuitBreaker.State.CLOSED);
        assertThat(breaker.tryAcquirePermission()).isTrue();
    }
}
