package com.jobseekercopilot.postcodeiogateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "external-provider")
public class ExternalProviderProperties {
    private ExternalProviderMode mode;
    private String baseUrl = "https://api.postcodes.io";

    public ExternalProviderMode getMode() {
        return mode;
    }

    public void setMode(ExternalProviderMode mode) {
        this.mode = mode;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
