package com.jobseekercopilot.postcodeiogateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "external-provider")
public class ExternalProviderProperties {
    private ExternalProviderMode mode;

    public ExternalProviderMode getMode() {
        return mode;
    }

    public void setMode(ExternalProviderMode mode) {
        this.mode = mode;
    }
}
