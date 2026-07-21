package com.jobseekercopilot.postcodeiogateway.logging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CorrelationIdFilterTest {

    @Test
    void redactsPostcodePathValues() {
        assertEquals("/api/postcodes/{postcode}",
                CorrelationIdFilter.safePath("/api/postcodes/LS1%201UR"));
    }

    @Test
    void preservesNonSensitiveOperationalPaths() {
        assertEquals("/actuator/health", CorrelationIdFilter.safePath("/actuator/health"));
    }
}
