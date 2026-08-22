package com.jobseekercopilot.postcodeiogateway.client;

import java.time.Duration;
import java.util.function.LongSupplier;

final class ProviderCircuitBreaker {
    enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    private final int failureThreshold;
    private final long openDurationNanos;
    private final LongSupplier nanoTime;
    private State state = State.CLOSED;
    private int consecutiveFailures;
    private long openedAtNanos;
    private boolean halfOpenRequestInFlight;

    ProviderCircuitBreaker(int failureThreshold, Duration openDuration, LongSupplier nanoTime) {
        if (failureThreshold < 1) {
            throw new IllegalArgumentException("Circuit failure threshold must be at least one.");
        }
        if (openDuration == null || openDuration.isZero() || openDuration.isNegative()) {
            throw new IllegalArgumentException("Circuit open duration must be positive.");
        }
        this.failureThreshold = failureThreshold;
        this.openDurationNanos = openDuration.toNanos();
        this.nanoTime = nanoTime;
    }

    synchronized boolean tryAcquirePermission() {
        if (state == State.OPEN) {
            if (nanoTime.getAsLong() - openedAtNanos < openDurationNanos) {
                return false;
            }
            state = State.HALF_OPEN;
        }
        if (state == State.HALF_OPEN) {
            if (halfOpenRequestInFlight) {
                return false;
            }
            halfOpenRequestInFlight = true;
        }
        return true;
    }

    synchronized boolean recordSuccess() {
        boolean changed = state != State.CLOSED;
        state = State.CLOSED;
        consecutiveFailures = 0;
        halfOpenRequestInFlight = false;
        return changed;
    }

    synchronized boolean recordFailure() {
        halfOpenRequestInFlight = false;
        if (state == State.HALF_OPEN) {
            open();
            return true;
        }
        consecutiveFailures++;
        if (consecutiveFailures >= failureThreshold) {
            boolean changed = state != State.OPEN;
            open();
            return changed;
        }
        return false;
    }

    synchronized State state() {
        if (state == State.OPEN && nanoTime.getAsLong() - openedAtNanos >= openDurationNanos) {
            return State.HALF_OPEN;
        }
        return state;
    }

    private void open() {
        state = State.OPEN;
        openedAtNanos = nanoTime.getAsLong();
        consecutiveFailures = 0;
    }
}
