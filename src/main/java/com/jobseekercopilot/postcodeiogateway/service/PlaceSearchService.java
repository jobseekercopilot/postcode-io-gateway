package com.jobseekercopilot.postcodeiogateway.service;

import com.jobseekercopilot.postcodeiogateway.client.PostcodeProviderClient;
import com.jobseekercopilot.postcodeiogateway.model.PlaceLocation;
import com.jobseekercopilot.postcodeiogateway.validation.PlaceSearchValidator;
import java.util.List;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class PlaceSearchService {
    private final PostcodeProviderClient postcodeProviderClient;
    private final PlaceSearchValidator placeSearchValidator;

    public PlaceSearchService(
            PostcodeProviderClient postcodeProviderClient,
            PlaceSearchValidator placeSearchValidator) {
        this.postcodeProviderClient = postcodeProviderClient;
        this.placeSearchValidator = placeSearchValidator;
    }

    public Mono<List<PlaceLocation>> search(String query, int limit) {
        String cleanQuery = placeSearchValidator.validateAndNormalise(query, limit);
        return postcodeProviderClient.searchPlaces(cleanQuery, limit);
    }
}
