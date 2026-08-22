package com.jobseekercopilot.postcodeiogateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "deployment")
public class DeploymentProperties {
    private DeploymentEnvironmentClass environmentClass;

    public DeploymentEnvironmentClass getEnvironmentClass() {
        return environmentClass;
    }

    public void setEnvironmentClass(DeploymentEnvironmentClass environmentClass) {
        this.environmentClass = environmentClass;
    }
}
