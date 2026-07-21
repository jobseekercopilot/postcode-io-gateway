package com.jobseekercopilot.postcodeiogateway;

import com.jobseekercopilot.postcodeiogateway.controller.PostcodeController;
import com.jobseekercopilot.postcodeiogateway.client.ProviderCircuitOpenException;
import com.jobseekercopilot.postcodeiogateway.client.ProviderNotFoundException;
import com.jobseekercopilot.postcodeiogateway.client.ProviderResponseException;
import com.jobseekercopilot.postcodeiogateway.logging.CorrelationIdFilter;
import com.jobseekercopilot.postcodeiogateway.model.PostcodeLocation;
import com.jobseekercopilot.postcodeiogateway.service.PostcodeService;
import com.jobseekercopilot.postcodeiogateway.validation.InvalidPostcodeException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PostcodeController.class)
@ExtendWith(OutputCaptureExtension.class)
public class PostcodeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostcodeService postcodeService;

    @Test
    public void testGetPostcodeInfo_success() throws Exception {
        String postcode = "LS1";
        PostcodeLocation mockLocation = new PostcodeLocation();
        mockLocation.setPostcode(postcode);
        mockLocation.setRegion("Yorkshire and the Humber");
        mockLocation.setAdminDistrict("Leeds");

        when(postcodeService.getPostcodeInfo(postcode)).thenReturn(Mono.just(mockLocation));

        MvcResult mvcResult = mockMvc.perform(get("/api/postcodes/" + postcode)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postcode").value(postcode))
                .andExpect(jsonPath("$.region").value("Yorkshire and the Humber"))
                .andExpect(jsonPath("$.admin_district").value("Leeds"));

        verify(postcodeService, times(1)).getPostcodeInfo(postcode);
    }

    @Test
    void mapsInvalidPostcodesToAStableRedacted400Response(CapturedOutput output) throws Exception {
        errorRequest("INVALID-SECRET", new InvalidPostcodeException())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_POSTCODE"))
                .andExpect(jsonPath("$.message").value("Postcode must be a valid UK postcode or outcode."))
                .andExpect(jsonPath("$.correlationId").value("contract-test-123"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("INVALID-SECRET"))));
        org.assertj.core.api.Assertions.assertThat(output)
                .contains("path=/api/postcodes/{postcode}", "status=400")
                .doesNotContain("INVALID-SECRET");
    }

    @Test
    void rejectsAnEmptyPostcodePath() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/api/postcodes")
                        .header(CorrelationIdFilter.HEADER_NAME, "contract-test-123"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_POSTCODE"))
                .andExpect(jsonPath("$.correlationId").value("contract-test-123"));
    }

    @Test
    void mapsProviderFailuresToDocumentedStatuses() throws Exception {
        errorRequest("LS1", new ProviderNotFoundException())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POSTCODE_NOT_FOUND"));
        errorRequest("LS2", WebClientResponseException.create(
                        429, "provider-secret", null, new byte[0], StandardCharsets.UTF_8))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("PROVIDER_RATE_LIMITED"));
        errorRequest("LS3", new ProviderResponseException())
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("PROVIDER_BAD_RESPONSE"));
        errorRequest("LS4", new ProviderCircuitOpenException())
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("PROVIDER_UNAVAILABLE"));
        errorRequest("LS5", new TimeoutException("provider-secret-timeout"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.code").value("PROVIDER_TIMEOUT"));
    }

    @Test
    void redactsUnexpectedFailureDetails(CapturedOutput output) throws Exception {
        errorRequest("SE1", new RuntimeException("provider-secret-diagnostic"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("provider-secret-diagnostic"))));
        org.assertj.core.api.Assertions.assertThat(output)
                .contains("status=500", "code=INTERNAL_ERROR")
                .doesNotContain("provider-secret-diagnostic");
    }

    private ResultActions errorRequest(String postcode, Throwable error) throws Exception {
        when(postcodeService.getPostcodeInfo(postcode)).thenReturn(Mono.error(error));

        MvcResult mvcResult = mockMvc.perform(get("/api/postcodes/{postcode}", postcode)
                        .header(CorrelationIdFilter.HEADER_NAME, "contract-test-123")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(request().asyncStarted())
                .andReturn();

        return mockMvc.perform(asyncDispatch(mvcResult));
    }
}
