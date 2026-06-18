package com.jobseekercopilot.postcodeiogateway.service;

import com.jobseekercopilot.postcodeiogateway.client.PostcodeIoApiClient;
import com.jobseekercopilot.postcodeiogateway.model.PostcodeLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PostcodeServiceTest {

    @Mock
    private PostcodeIoApiClient postcodeIoApiClient;

    @InjectMocks
    private PostcodeService postcodeService;

    @Test
    public void testGetPostcodeInfo_success() {
        String postcode = "LS1";
        PostcodeLocation mockLocation = new PostcodeLocation();
        mockLocation.setPostcode(postcode);
        mockLocation.setRegion("Yorkshire and the Humber");
        mockLocation.setAdminDistrict("Leeds");

        when(postcodeIoApiClient.fetchPostcodeDetails(postcode)).thenReturn(Mono.just(mockLocation));

        Mono<PostcodeLocation> resultMono = postcodeService.getPostcodeInfo(postcode);
        PostcodeLocation result = resultMono.block();

        assertNotNull(result);
        assertEquals(postcode, result.getPostcode());
        assertEquals("Yorkshire and the Humber", result.getRegion());
        assertEquals("Leeds", result.getAdminDistrict());

        verify(postcodeIoApiClient, times(1)).fetchPostcodeDetails(postcode);
    }
}
