package com.jobseekercopilot.postcodeiogateway.controller;

import com.jobseekercopilot.postcodeiogateway.model.PostcodeLocation;
import com.jobseekercopilot.postcodeiogateway.service.PostcodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class PostcodeController {
    
    @Autowired
    private PostcodeService postcodeService;
    
    @GetMapping("/postcodes/{postcode}")
    public Mono<ResponseEntity<PostcodeLocation>> getPostcodeInfo(@PathVariable String postcode) {
        return postcodeService.getPostcodeInfo(postcode)
                .map(location -> ResponseEntity.ok(location))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()));
    }
}
