package com.jobseekercopilot.postcodeiogateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Jobseeker Copilot - Postcode.io Gateway API")
                        .description("Gateway API for UK postcode lookups via postcodes.io external API.")
                        .version("1.0.0"));
    }
}