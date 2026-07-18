package com.jobseekercopilot.postcodeiogateway.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jobseekercopilot.postcodeiogateway.logging.CorrelationIdFilter;
import com.jobseekercopilot.postcodeiogateway.model.PostcodeLocation;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "external-provider", name = "mode", havingValue = "LIVE", matchIfMissing = true)
public class PostcodeIoApiClient implements PostcodeProviderClient {

    private static final Logger log = LoggerFactory.getLogger(PostcodeIoApiClient.class);

    private final WebClient webClient;

    public PostcodeIoApiClient() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.postcodes.io")
                .filter((request, next) -> {
                    String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
                    if (StringUtils.hasText(correlationId)) {
                        return next.exchange(ClientRequest.from(request)
                                .header(CorrelationIdFilter.HEADER_NAME, correlationId)
                                .build());
                    }
                    return next.exchange(request);
                })
                .build();
    }

    @Override
    public Mono<PostcodeLocation> fetchPostcodeDetails(String postcode) {
        long startedAt = System.nanoTime();
        String clean = postcode.replaceAll("\\s+", "").toUpperCase();
        boolean isOutcode = clean.length() <= 4;
        log.info("postcodes.io lookup started lookupType={} postcodePresent={}",
                isOutcode ? "OUTCODE" : "POSTCODE",
                postcode != null && !postcode.isBlank());

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
                    })
                    .doOnSuccess(location -> log.info("postcodes.io lookup completed lookupType=OUTCODE found={} durationMs={}",
                            location != null,
                            (System.nanoTime() - startedAt) / 1_000_000))
                    .doOnError(error -> log.warn("postcodes.io lookup failed lookupType=OUTCODE durationMs={} error={}",
                            (System.nanoTime() - startedAt) / 1_000_000,
                            error.getClass().getSimpleName()));
        } else {
            return webClient.get()
                    .uri("/postcodes/" + clean)
                    .retrieve()
                    .bodyToMono(PostcodeResponse.class)
                    .map(PostcodeResponse::getResult)
                    .doOnSuccess(location -> log.info("postcodes.io lookup completed lookupType=POSTCODE found={} durationMs={}",
                            location != null,
                            (System.nanoTime() - startedAt) / 1_000_000))
                    .doOnError(error -> log.warn("postcodes.io lookup failed lookupType=POSTCODE durationMs={} error={}",
                            (System.nanoTime() - startedAt) / 1_000_000,
                            error.getClass().getSimpleName()));
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
