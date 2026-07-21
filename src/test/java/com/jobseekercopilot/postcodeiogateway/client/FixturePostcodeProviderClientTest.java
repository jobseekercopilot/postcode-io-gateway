package com.jobseekercopilot.postcodeiogateway.client;

import com.jobseekercopilot.postcodeiogateway.config.FixtureProperties;
import com.jobseekercopilot.postcodeiogateway.model.PostcodeLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FixturePostcodeProviderClientTest {

    private MockRestServiceServer server;
    private FixturePostcodeProviderClient client;

    @BeforeEach
    void setUp() {
        FixtureProperties properties = new FixtureProperties();
        properties.setSystemDataServiceUrl("http://fixture.test");
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new FixturePostcodeProviderClient(properties, builder);
    }

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    @Test
    void mapsTheOwnedPostcodeFixtureContract() {
        server.expect(once(), requestTo("http://fixture.test/internal/fixtures/postcodes/LS1%201UR"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "postcode": "LS1 1UR",
                          "country": "England",
                          "region": "Yorkshire and The Humber",
                          "adminDistrict": "Leeds",
                          "latitude": 53.8008,
                          "longitude": -1.5491,
                          "found": true,
                          "fixtureSource": "fixture",
                          "futureField": "ignored"
                        }
                        """, MediaType.APPLICATION_JSON));

        PostcodeLocation result = client.fetchPostcodeDetails("LS1%201UR").block();

        assertNotNull(result);
        assertEquals("LS1 1UR", result.getPostcode());
        assertEquals("England", result.getCountry());
        assertEquals("Yorkshire and The Humber", result.getRegion());
        assertEquals("Leeds", result.getAdminDistrict());
        assertEquals(53.8008, result.getLatitude());
        assertEquals(-1.5491, result.getLongitude());
    }

    @Test
    void propagatesFixtureServiceFailures() {
        server.expect(once(), requestTo("http://fixture.test/internal/fixtures/postcodes/B1%201AA"))
                .andRespond(withServerError());

        assertThrows(HttpServerErrorException.class,
                () -> client.fetchPostcodeDetails("B1%201AA").block());
    }

    @Test
    void rejectsFixturesThatExplicitlyReportNoMatch() {
        server.expect(once(), requestTo("http://fixture.test/internal/fixtures/postcodes/LS1"))
                .andRespond(withSuccess("""
                        {"postcode":"LS1","found":false}
                        """, MediaType.APPLICATION_JSON));

        assertThrows(ProviderNotFoundException.class,
                () -> client.fetchPostcodeDetails("LS1").block());
    }

    @Test
    void keepsQueryTextInsideTheEncodedPostcodePathSegment() {
        server.expect(once(), request -> {
                    assertEquals("/internal/fixtures/postcodes/LS1%201UR%3Fadmin=true",
                            request.getURI().getRawPath());
                    assertNull(request.getURI().getRawQuery());
                })
                .andRespond(withSuccess("""
                        {"postcode":"LS1 1UR","found":true}
                        """, MediaType.APPLICATION_JSON));

        PostcodeLocation result = client.fetchPostcodeDetails("LS1%201UR%3Fadmin%3Dtrue").block();

        assertNotNull(result);
        assertEquals("LS1 1UR", result.getPostcode());
    }

    @Test
    void failsClosedWhenPlaceSearchIsUnavailableInFixtureMode() {
        assertThrows(ProviderModeUnavailableException.class,
                () -> client.searchPlaces("Leeds", 10).block());
    }
}
