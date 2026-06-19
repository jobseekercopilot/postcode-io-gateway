package com.jobseekercopilot.postcodeiogateway.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jobseekercopilot.postcodeiogateway.model.PostcodeLocation;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class PostcodeIoApiClient {

    private final WebClient webClient;

    public PostcodeIoApiClient() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.postcodes.io")
                .build();
    }

    public Mono<PostcodeLocation> fetchPostcodeDetails(String postcode) {
        String clean = postcode.replaceAll("\\s+", "").toUpperCase();
        boolean isOutcode = clean.length() <= 4;

        if (isOutcode) {
            return webClient.get()
                    .uri("/outcodes/" + clean)
                    .retrieve()
                    .bodyToMono(OutcodeResponse.class)
                    .map(outcodeResponse -> {
                        PostcodeLocation loc = new PostcodeLocation();
                        OutcodeResult result = outcodeResponse.getResult();
                        if (result != null) {
                            loc.setPostcode(result.getOutcode());
                            if (result.getRegion() != null && !result.getRegion().isEmpty()) {
                                loc.setRegion(result.getRegion().get(0));
                            }
                            if (result.getAdminDistrict() != null && !result.getAdminDistrict().isEmpty()) {
                                loc.setAdminDistrict(result.getAdminDistrict().get(0));
                            }
                            if (result.getCountry() != null && !result.getCountry().isEmpty()) {
                                loc.setCountry(result.getCountry().get(0));
                            }
                        }
                        return loc;
                    });
        } else {
            return webClient.get()
                    .uri("/postcodes/" + clean)
                    .retrieve()
                    .bodyToMono(PostcodeResponse.class)
                    .map(PostcodeResponse::getResult);
        }
    }

    @Getter
    @Setter
    private static class PostcodeResponse {
        private PostcodeLocation result;
    }

    @Getter
    @Setter
    private static class OutcodeResponse {
        private OutcodeResult result;
    }

    @Getter
    @Setter
    private static class OutcodeResult {
        private String outcode;
        private List<String> region;
        @JsonProperty("admin_district")
        private List<String> adminDistrict;
        private List<String> country;
    }
}