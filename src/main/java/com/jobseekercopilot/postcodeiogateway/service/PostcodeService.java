package com.jobseekercopilot.postcodeiogateway.service;

import com.jobseekercopilot.postcodeiogateway.client.PostcodeIoApiClient;
import com.jobseekercopilot.postcodeiogateway.model.PostcodeLocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class PostcodeService {
    
    @Autowired
    private PostcodeIoApiClient postcodeIoApiClient;
    
    public Mono<PostcodeLocation> getPostcodeInfo(String postcode) {
        return postcodeIoApiClient.fetchPostcodeDetails(postcode);
    }
}
