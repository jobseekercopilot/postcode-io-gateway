package com.jobseekercopilot.postcodeiogateway.service;

import com.jobseekercopilot.postcodeiogateway.client.PostcodeProviderClient;
import com.jobseekercopilot.postcodeiogateway.model.PostcodeLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class PostcodeService {
    private static final Logger log = LoggerFactory.getLogger(PostcodeService.class);

    private final PostcodeProviderClient postcodeProviderClient;

    public PostcodeService(PostcodeProviderClient postcodeProviderClient) {
        this.postcodeProviderClient = postcodeProviderClient;
    }
    
    public Mono<PostcodeLocation> getPostcodeInfo(String postcode) {
        log.info("Postcode lookup requested postcodePresent={}", postcode != null && !postcode.isBlank());
        return postcodeProviderClient.fetchPostcodeDetails(postcode);
    }
}
