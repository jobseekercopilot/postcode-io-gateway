package com.jobseekercopilot.postcodeiogateway.controller;

import com.jobseekercopilot.postcodeiogateway.error.ApiError;
import com.jobseekercopilot.postcodeiogateway.model.PostcodeLocation;
import com.jobseekercopilot.postcodeiogateway.service.PostcodeService;
import com.jobseekercopilot.postcodeiogateway.validation.InvalidPostcodeException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
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

    @GetMapping(value = "/{postcode}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get location by postcode", description = "Retrieves geographic location data for a UK postcode via postcodes.io.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Postcode location found"),
            @ApiResponse(responseCode = "400", description = "Invalid postcode or outcode",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Postcode not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Postcode area is outside approved coverage",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "429", description = "Provider rate limited",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "502", description = "Provider returned an invalid response",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "503", description = "Postcode service unavailable",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "504", description = "Postcode service timed out",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
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
