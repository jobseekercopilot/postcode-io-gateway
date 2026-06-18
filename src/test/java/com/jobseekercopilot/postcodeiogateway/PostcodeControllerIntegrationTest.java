package com.jobseekercopilot.postcodeiogateway;

import com.jobseekercopilot.postcodeiogateway.controller.PostcodeController;
import com.jobseekercopilot.postcodeiogateway.model.PostcodeLocation;
import com.jobseekercopilot.postcodeiogateway.service.PostcodeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PostcodeController.class)
public class PostcodeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
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
    public void testGetPostcodeInfo_error() throws Exception {
        String postcode = "INVALID";
        when(postcodeService.getPostcodeInfo(postcode)).thenReturn(Mono.error(new RuntimeException("Not found")));

        MvcResult mvcResult = mockMvc.perform(get("/api/postcodes/" + postcode)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isInternalServerError());

        verify(postcodeService, times(1)).getPostcodeInfo(postcode);
    }
}
