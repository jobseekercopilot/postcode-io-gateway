package com.jobseekercopilot.postcodeiogateway.controller;

import com.jobseekercopilot.postcodeiogateway.model.PlaceLocation;
import com.jobseekercopilot.postcodeiogateway.service.PlaceSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/places")
@Tag(name = "Place", description = "Bounded UK place-name search")
public class PlaceController {
    private final PlaceSearchService placeSearchService;

    public PlaceController(PlaceSearchService placeSearchService) {
        this.placeSearchService = placeSearchService;
    }

    @GetMapping
    @Operation(summary = "Search UK places", description = "Returns at most ten matching UK places.")
    public Mono<ResponseEntity<List<PlaceLocation>>> searchPlaces(
            @Parameter(description = "Place-name query", required = true, example = "Leeds")
            @RequestParam(value = "q", defaultValue = "") String query,
            @Parameter(description = "Maximum results from 1 to 10", example = "10")
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return placeSearchService.search(query, limit).map(ResponseEntity::ok);
    }
}
