package com.jobseekercopilot.postcodeiogateway.client;

import com.jobseekercopilot.postcodeiogateway.config.FixtureProperties;
import com.jobseekercopilot.postcodeiogateway.model.PostcodeLocation;
import com.jobseekercopilot.generated.systemdataservice.api.FixtureControllerApi;
import com.jobseekercopilot.generated.systemdataservice.model.FixturePostcodeResponse;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(prefix = "external-provider", name = "mode", havingValue = "FIXTURE")
public class FixturePostcodeProviderClient implements PostcodeProviderClient {
    private static final Logger log = LoggerFactory.getLogger(FixturePostcodeProviderClient.class);
    private final FixtureProperties fixtureProperties;
    private final FixtureControllerApi fixtureControllerApi;

    public FixturePostcodeProviderClient(FixtureProperties fixtureProperties, FixtureControllerApi fixtureControllerApi) {
        this.fixtureProperties = fixtureProperties;
        this.fixtureControllerApi = fixtureControllerApi;
    }

    @Override
    public Mono<PostcodeLocation> fetchPostcodeDetails(String postcode) {
        String decodedPostcode = URLDecoder.decode(postcode, StandardCharsets.UTF_8);
        FixturePostcodeResponse body = fixtureControllerApi.postcode(decodedPostcode);
        PostcodeLocation location = new PostcodeLocation();
        if (body != null) {
            location.setPostcode(text(body.getPostcode(), decodedPostcode));
            location.setCountry(body.getCountry());
            location.setRegion(body.getRegion());
            location.setAdminDistrict(body.getAdminDistrict());
            location.setLongitude(decimal(body.getLongitude()));
            location.setLatitude(decimal(body.getLatitude()));
        }
        log.info("postcode fixture lookup returned postcode={} region={} datasetId={} scenario={}",
                location.getPostcode(), location.getRegion(), fixtureProperties.getDatasetId(), fixtureProperties.getScenario());
        return Mono.just(location);
    }

    private String text(String value, String fallback) {
        return value == null ? fallback : value;
    }

    private double decimal(Double value) {
        return value == null ? 0.0 : value;
    }
}
