package com.jobseekercopilot.postcodeiogateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {
        "deployment.environment-class=TEST",
        "external-provider.mode=FIXTURE"
})
@AutoConfigureMockMvc
class OpenApiContractTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void publishesTheBreakingCoverageGateAndStableErrorSchema() throws Exception {
        var response = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Files.createDirectories(Path.of("target"));
        Files.writeString(Path.of("target/openapi.json"), response);
        var root = objectMapper.readTree(response);

        assertEquals("2.0.0", root.at("/info/version").asText());
        var unsupported = root.at(
                "/paths/~1api~1postcodes~1{postcode}/get/responses/422");
        assertTrue(unsupported.isObject());
        assertEquals(
                "#/components/schemas/ApiError",
                unsupported.at("/content/application~1json/schema/$ref").asText());
    }
}
