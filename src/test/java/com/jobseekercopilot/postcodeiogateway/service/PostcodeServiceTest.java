package com.jobseekercopilot.postcodeiogateway.service;

import com.jobseekercopilot.postcodeiogateway.client.PostcodeProviderClient;
import com.jobseekercopilot.postcodeiogateway.model.PostcodeLocation;
import com.jobseekercopilot.postcodeiogateway.validation.PostcodeValidator;
import com.jobseekercopilot.postcodeiogateway.validation.InvalidPostcodeException;
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
    private PostcodeProviderClient postcodeProviderClient;

    @Mock
    private PostcodeValidator postcodeValidator;

    @InjectMocks
    private PostcodeService postcodeService;

    @Test
    public void testGetPostcodeInfo_success() {
        String postcode = "ls1  1ur";
        String canonicalPostcode = "LS1 1UR";
        PostcodeLocation mockLocation = new PostcodeLocation();
        mockLocation.setPostcode(canonicalPostcode);
        mockLocation.setRegion("Yorkshire and the Humber");
        mockLocation.setAdminDistrict("Leeds");

        when(postcodeValidator.validateAndCanonicalise(postcode)).thenReturn(canonicalPostcode);
        when(postcodeProviderClient.fetchPostcodeDetails(canonicalPostcode)).thenReturn(Mono.just(mockLocation));

        Mono<PostcodeLocation> resultMono = postcodeService.getPostcodeInfo(postcode);
        PostcodeLocation result = resultMono.block();

        assertNotNull(result);
        assertEquals(canonicalPostcode, result.getPostcode());
        assertEquals("Yorkshire and the Humber", result.getRegion());
        assertEquals("Leeds", result.getAdminDistrict());

        verify(postcodeProviderClient, times(1)).fetchPostcodeDetails(canonicalPostcode);
    }

    @Test
    void invalidInputNeverReachesTheProvider() {
        when(postcodeValidator.validateAndCanonicalise("partial"))
                .thenThrow(new InvalidPostcodeException());

        assertThrows(InvalidPostcodeException.class,
                () -> postcodeService.getPostcodeInfo("partial"));
        verifyNoInteractions(postcodeProviderClient);
    }
}
