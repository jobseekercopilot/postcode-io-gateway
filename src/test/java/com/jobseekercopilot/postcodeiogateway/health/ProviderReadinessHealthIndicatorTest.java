package com.jobseekercopilot.postcodeiogateway.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jobseekercopilot.postcodeiogateway.client.PostcodeProviderClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;

class ProviderReadinessHealthIndicatorTest {
    @Test
    void reportsOnlyAggregateReadinessFromTheProviderCircuit() {
        PostcodeProviderClient client = mock(PostcodeProviderClient.class);
        ProviderReadinessHealthIndicator indicator = new ProviderReadinessHealthIndicator(client);

        when(client.isReady()).thenReturn(true, false);

        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
        assertThat(indicator.health().getStatus().getCode()).isEqualTo("OUT_OF_SERVICE");
        assertThat(indicator.health().getDetails()).isEmpty();
    }
}
