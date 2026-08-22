package com.jobseekercopilot.postcodeiogateway.controller;

import com.jobseekercopilot.postcodeiogateway.config.DeploymentProperties;
import com.jobseekercopilot.postcodeiogateway.config.ExternalProviderMode;
import com.jobseekercopilot.postcodeiogateway.config.ExternalProviderProperties;
import com.jobseekercopilot.postcodeiogateway.config.FixtureProperties;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
@ConditionalOnProperty(prefix = "provider-diagnostics", name = "enabled", havingValue = "true")
public class ProviderModeController {
    private final ExternalProviderProperties providerProperties;
    private final DeploymentProperties deploymentProperties;
    private final FixtureProperties fixtureProperties;

    public ProviderModeController(
            ExternalProviderProperties providerProperties,
            DeploymentProperties deploymentProperties,
            FixtureProperties fixtureProperties) {
        this.providerProperties = providerProperties;
        this.deploymentProperties = deploymentProperties;
        this.fixtureProperties = fixtureProperties;
    }

    @GetMapping("/provider-mode")
    public Map<String, Object> providerMode() {
        return Map.of(
                "gateway", "postcode-io-gateway",
                "environmentClass", deploymentProperties.getEnvironmentClass().name(),
                "mode", providerProperties.getMode().name(),
                "datasetId", fixtureProperties.getDatasetId(),
                "datasetVersion", fixtureProperties.getDatasetVersion(),
                "scenario", fixtureProperties.getScenario(),
                "externalCallsEnabled", providerProperties.getMode() == ExternalProviderMode.LIVE);
    }
}
