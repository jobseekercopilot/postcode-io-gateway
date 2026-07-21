package com.jobseekercopilot.postcodeiogateway;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jobseekercopilot.postcodeiogateway.client.ProviderModeUnavailableException;
import com.jobseekercopilot.postcodeiogateway.controller.PlaceController;
import com.jobseekercopilot.postcodeiogateway.logging.CorrelationIdFilter;
import com.jobseekercopilot.postcodeiogateway.model.PlaceLocation;
import com.jobseekercopilot.postcodeiogateway.service.PlaceSearchService;
import com.jobseekercopilot.postcodeiogateway.validation.InvalidPlaceSearchException;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Mono;

@WebMvcTest(PlaceController.class)
@ExtendWith(OutputCaptureExtension.class)
class PlaceControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlaceSearchService placeSearchService;

    @Test
    void returnsBoundedMappedPlaces() throws Exception {
        when(placeSearchService.search("Leeds", 2)).thenReturn(Mono.just(List.of(
                new PlaceLocation("place-1", "Leeds", "LS1", "Yorkshire and The Humber",
                        "Leeds", 53.8008, -1.5491),
                new PlaceLocation("place-2", "Leeds Beckett", "LS6", "Yorkshire and The Humber",
                        "Leeds", 53.82, -1.58))));

        MvcResult result = mockMvc.perform(get("/api/places").queryParam("q", "Leeds")
                        .queryParam("limit", "2"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value("place-1"))
                .andExpect(jsonPath("$[0].postcode").value("LS1"));
        verify(placeSearchService).search("Leeds", 2);
    }

    @Test
    void returnsStableRedactedValidationAndModeErrors(CapturedOutput output) throws Exception {
        String unsafe = "SECRET?limit=100";
        when(placeSearchService.search(unsafe, 10)).thenReturn(Mono.error(new InvalidPlaceSearchException()));
        MvcResult invalid = mockMvc.perform(get("/api/places").queryParam("q", unsafe)
                        .header(CorrelationIdFilter.HEADER_NAME, "place-test-123"))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PLACE_SEARCH"))
                .andExpect(content().string(Matchers.not(Matchers.containsString(unsafe))));

        when(placeSearchService.search("Leeds", 10))
                .thenReturn(Mono.error(new ProviderModeUnavailableException()));
        MvcResult unavailable = mockMvc.perform(get("/api/places").queryParam("q", "Leeds"))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(unavailable))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("CAPABILITY_UNAVAILABLE"));

        org.assertj.core.api.Assertions.assertThat(output)
                .doesNotContain(unsafe)
                .contains("path=/api/places");
    }

    @Test
    void rejectsMissingQueryAndMalformedLimit() throws Exception {
        when(placeSearchService.search("", 10)).thenReturn(Mono.error(new InvalidPlaceSearchException()));
        MvcResult missing = mockMvc.perform(get("/api/places"))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(missing))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PLACE_SEARCH"));

        mockMvc.perform(get("/api/places").queryParam("q", "Leeds").queryParam("limit", "many"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PLACE_SEARCH"));
    }
}
