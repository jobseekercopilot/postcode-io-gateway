package com.jobseekercopilot.postcodeiogateway.client;

import com.jobseekercopilot.postcodeiogateway.model.PlaceLocation;
import com.jobseekercopilot.postcodeiogateway.model.PostcodeLocation;
import java.util.List;
import reactor.core.publisher.Mono;

public interface PostcodeProviderClient {
    Mono<PostcodeLocation> fetchPostcodeDetails(String postcode);

    Mono<List<PlaceLocation>> searchPlaces(String query, int limit);

    boolean isReady();
}
