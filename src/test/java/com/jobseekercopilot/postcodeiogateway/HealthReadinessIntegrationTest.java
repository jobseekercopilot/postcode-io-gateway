package com.jobseekercopilot.postcodeiogateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "deployment.environment-class=TEST",
        "external-provider.mode=FIXTURE"
})
@AutoConfigureTestRestTemplate
class HealthReadinessIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void healthAndProviderReadinessAreAggregateOnly() {
        var health = restTemplate.getForEntity("/actuator/health", Map.class);
        var readiness = restTemplate.getForEntity("/actuator/health/readiness", Map.class);

        assertThat(health.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(health.getBody()).containsEntry("status", "UP").doesNotContainKey("components");
        assertThat(readiness.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(readiness.getBody()).containsEntry("status", "UP").doesNotContainKey("components");
    }

    @Test
    void metricsEndpointRemainsPrivate() {
        assertThat(restTemplate.getForEntity("/actuator/metrics", Map.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
