package com.jobseekercopilot.postcodeiogateway.health;

import com.jobseekercopilot.postcodeiogateway.client.PostcodeProviderClient;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("providerReadinessHealthIndicator")
public class ProviderReadinessHealthIndicator implements HealthIndicator {
    private final PostcodeProviderClient providerClient;

    public ProviderReadinessHealthIndicator(PostcodeProviderClient providerClient) {
        this.providerClient = providerClient;
    }

    @Override
    public Health health() {
        return providerClient.isReady()
                ? Health.up().build()
                : Health.status("OUT_OF_SERVICE").build();
    }
}
