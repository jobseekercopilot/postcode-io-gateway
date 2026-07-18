package com.jobseekercopilot.postcodeiogateway.client;

import com.jobseekercopilot.postcodeiogateway.model.PostcodeLocation;
import reactor.core.publisher.Mono;

public interface PostcodeProviderClient {
    Mono<PostcodeLocation> fetchPostcodeDetails(String postcode);
}
