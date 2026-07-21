package com.jobseekercopilot.postcodeiogateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ProviderModeSafety {
    private static final Logger log = LoggerFactory.getLogger(ProviderModeSafety.class);

    public ProviderModeSafety(
            ExternalProviderProperties providerProperties,
            DeploymentProperties deploymentProperties,
            ProviderDiagnosticsProperties diagnosticsProperties,
            FixtureProperties fixtureProperties) {
        ExternalProviderMode mode = providerProperties.getMode();
        DeploymentEnvironmentClass environmentClass = deploymentProperties.getEnvironmentClass();
        validate(environmentClass, mode, diagnosticsProperties.isEnabled());

        log.info("provider mode active gateway=postcode-io-gateway environmentClass={} mode={} externalCallsEnabled={} diagnosticsEnabled={}",
                environmentClass, mode, mode == ExternalProviderMode.LIVE, diagnosticsProperties.isEnabled());
        if (mode == ExternalProviderMode.FIXTURE) {
            log.info("fixture configuration active gateway=postcode-io-gateway datasetId={} datasetVersion={} scenario={}",
                    fixtureProperties.getDatasetId(), fixtureProperties.getDatasetVersion(), fixtureProperties.getScenario());
        }
    }

    static void validate(
            DeploymentEnvironmentClass environmentClass,
            ExternalProviderMode mode,
            boolean diagnosticsEnabled) {
        if (environmentClass == null) {
            throw new IllegalStateException("DEPLOYMENT_ENVIRONMENT_CLASS must be set explicitly.");
        }
        if (mode == null) {
            throw new IllegalStateException("EXTERNAL_PROVIDER_MODE must be set explicitly.");
        }

        boolean fixtureOnly = environmentClass == DeploymentEnvironmentClass.TEST
                || environmentClass == DeploymentEnvironmentClass.DEMO;
        boolean liveOnly = environmentClass == DeploymentEnvironmentClass.STAGING
                || environmentClass == DeploymentEnvironmentClass.PRODUCTION;

        if (fixtureOnly && mode != ExternalProviderMode.FIXTURE) {
            throw new IllegalStateException(environmentClass + " must use FIXTURE provider mode.");
        }
        if (liveOnly && mode != ExternalProviderMode.LIVE) {
            throw new IllegalStateException(environmentClass + " must use LIVE provider mode.");
        }
        if (liveOnly && diagnosticsEnabled) {
            throw new IllegalStateException("Provider diagnostics cannot be enabled in " + environmentClass + ".");
        }
    }
}
