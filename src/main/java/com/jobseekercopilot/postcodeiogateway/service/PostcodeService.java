package com.jobseekercopilot.postcodeiogateway.service;

import com.jobseekercopilot.postcodeiogateway.client.PostcodeProviderClient;
import com.jobseekercopilot.postcodeiogateway.config.PostcodeCoverageProperties;
import com.jobseekercopilot.postcodeiogateway.model.PostcodeLocation;
import com.jobseekercopilot.postcodeiogateway.validation.PostcodeValidator;
import com.jobseekercopilot.postcodeiogateway.validation.UnsupportedPostcodeCoverageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class PostcodeService {
    private static final Logger log = LoggerFactory.getLogger(PostcodeService.class);

    private final PostcodeProviderClient postcodeProviderClient;
    private final PostcodeValidator postcodeValidator;
    private final PostcodeCoverageProperties coverageProperties;

    public PostcodeService(
            PostcodeProviderClient postcodeProviderClient,
            PostcodeValidator postcodeValidator,
            PostcodeCoverageProperties coverageProperties) {
        this.postcodeProviderClient = postcodeProviderClient;
        this.postcodeValidator = postcodeValidator;
        this.coverageProperties = coverageProperties;
    }
    
    public Mono<PostcodeLocation> getPostcodeInfo(String postcode) {
        String canonicalPostcode = postcodeValidator.validateAndCanonicalise(postcode);
        if (canonicalPostcode.startsWith("BT")
                && !coverageProperties.isNorthernIrelandEnabled()) {
            throw new UnsupportedPostcodeCoverageException();
        }
        log.info("Postcode lookup requested postcodePresent=true");
        return postcodeProviderClient.fetchPostcodeDetails(canonicalPostcode);
    }
}
