package com.jobseekercopilot.postcodeiogateway.controller;

import com.jobseekercopilot.postcodeiogateway.model.PostcodeLocation;
import com.jobseekercopilot.postcodeiogateway.service.PostcodeService;
import com.jobseekercopilot.postcodeiogateway.validation.InvalidPostcodeException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/postcodes")
@Tag(name = "Postcode", description = "UK postcode lookup endpoints")
public class PostcodeController {

    private final PostcodeService postcodeService;

    public PostcodeController(PostcodeService postcodeService) {
        this.postcodeService = postcodeService;
    }

    @GetMapping("/{postcode}")
    @Operation(summary = "Get location by postcode", description = "Retrieves geographic location data for a UK postcode via postcodes.io.")
    @Tag(name = "Postcode")
    public Mono<ResponseEntity<PostcodeLocation>> getLocationByPostcode(
            @Parameter(description = "UK postcode", required = true, example = "SW1A 1AA")
            @PathVariable String postcode) {
        return postcodeService.getPostcodeInfo(postcode)
                .map(ResponseEntity::ok);
    }

    @GetMapping({"", "/"})
    public Mono<ResponseEntity<PostcodeLocation>> rejectMissingPostcode() {
        return Mono.error(new InvalidPostcodeException());
    }
}
