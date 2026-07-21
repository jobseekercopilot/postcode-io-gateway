package com.jobseekercopilot.postcodeiogateway.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.jobseekercopilot.postcodeiogateway.config.FixtureProperties;
import com.jobseekercopilot.postcodeiogateway.model.PostcodeLocation;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(prefix = "external-provider", name = "mode", havingValue = "FIXTURE")
public class FixturePostcodeProviderClient implements PostcodeProviderClient {
    private static final Logger log = LoggerFactory.getLogger(FixturePostcodeProviderClient.class);
    private final FixtureProperties fixtureProperties;
    private final RestClient restClient;

    public FixturePostcodeProviderClient(FixtureProperties fixtureProperties, RestClient.Builder restClientBuilder) {
        this.fixtureProperties = fixtureProperties;
        this.restClient = restClientBuilder
                .baseUrl(fixtureProperties.getSystemDataServiceUrl())
                .build();
    }

    @Override
    public Mono<PostcodeLocation> fetchPostcodeDetails(String postcode) {
        String decodedPostcode = URLDecoder.decode(postcode, StandardCharsets.UTF_8);
        return Mono.fromSupplier(() -> {
            FixturePostcodeResponse body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .pathSegment("internal", "fixtures", "postcodes", decodedPostcode)
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(FixturePostcodeResponse.class)
                    .getBody();

            PostcodeLocation location = new PostcodeLocation();
            if (body != null) {
                location.setPostcode(text(body.postcode(), decodedPostcode));
                location.setCountry(body.country());
                location.setRegion(body.region());
                location.setAdminDistrict(body.adminDistrict());
                location.setLongitude(decimal(body.longitude()));
                location.setLatitude(decimal(body.latitude()));
            }
            log.info("postcode fixture lookup completed found={}",
                    body != null && Boolean.TRUE.equals(body.found()));
            return location;
        });
    }

    private String text(String value, String fallback) {
        return value == null ? fallback : value;
    }

    private double decimal(Double value) {
        return value == null ? 0.0 : value;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record FixturePostcodeResponse(
            String postcode,
            String country,
            String region,
            String adminDistrict,
            Double latitude,
            Double longitude,
            Boolean found) {
    }
}
