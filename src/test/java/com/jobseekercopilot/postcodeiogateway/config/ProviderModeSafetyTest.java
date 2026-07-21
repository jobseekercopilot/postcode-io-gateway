package com.jobseekercopilot.postcodeiogateway.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jobseekercopilot.postcodeiogateway.controller.ProviderModeController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class ProviderModeSafetyTest {
    private final ApplicationContextRunner safetyContext = new ApplicationContextRunner()
            .withUserConfiguration(SafetyConfiguration.class);

    private final ApplicationContextRunner diagnosticsContext = new ApplicationContextRunner()
            .withUserConfiguration(DiagnosticsConfiguration.class)
            .withPropertyValues(
                    "external-provider.mode=FIXTURE",
                    "deployment.environment-class=LOCAL");

    @Test
    void acceptsOnlyTheDocumentedEnvironmentModeMatrix() {
        assertThatCode(() -> ProviderModeSafety.validate(
                DeploymentEnvironmentClass.LOCAL, ExternalProviderMode.LIVE, false)).doesNotThrowAnyException();
        assertThatCode(() -> ProviderModeSafety.validate(
                DeploymentEnvironmentClass.LOCAL, ExternalProviderMode.FIXTURE, true)).doesNotThrowAnyException();
        assertThatCode(() -> ProviderModeSafety.validate(
                DeploymentEnvironmentClass.TEST, ExternalProviderMode.FIXTURE, false)).doesNotThrowAnyException();
        assertThatCode(() -> ProviderModeSafety.validate(
                DeploymentEnvironmentClass.DEMO, ExternalProviderMode.FIXTURE, false)).doesNotThrowAnyException();
        assertThatCode(() -> ProviderModeSafety.validate(
                DeploymentEnvironmentClass.STAGING, ExternalProviderMode.LIVE, false)).doesNotThrowAnyException();
        assertThatCode(() -> ProviderModeSafety.validate(
                DeploymentEnvironmentClass.PRODUCTION, ExternalProviderMode.LIVE, false)).doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingConfiguration() {
        assertThatThrownBy(() -> ProviderModeSafety.validate(null, ExternalProviderMode.LIVE, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DEPLOYMENT_ENVIRONMENT_CLASS");
        assertThatThrownBy(() -> ProviderModeSafety.validate(DeploymentEnvironmentClass.LOCAL, null, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EXTERNAL_PROVIDER_MODE");
    }

    @Test
    void rejectsLiveCallsInTestLikeEnvironments() {
        assertThatThrownBy(() -> ProviderModeSafety.validate(
                DeploymentEnvironmentClass.TEST, ExternalProviderMode.LIVE, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TEST must use FIXTURE");
        assertThatThrownBy(() -> ProviderModeSafety.validate(
                DeploymentEnvironmentClass.DEMO, ExternalProviderMode.LIVE, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DEMO must use FIXTURE");
    }

    @Test
    void rejectsFixtureDataAndDiagnosticsInProductionLikeEnvironments() {
        assertThatThrownBy(() -> ProviderModeSafety.validate(
                DeploymentEnvironmentClass.STAGING, ExternalProviderMode.FIXTURE, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("STAGING must use LIVE");
        assertThatThrownBy(() -> ProviderModeSafety.validate(
                DeploymentEnvironmentClass.PRODUCTION, ExternalProviderMode.FIXTURE, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PRODUCTION must use LIVE");
        assertThatThrownBy(() -> ProviderModeSafety.validate(
                DeploymentEnvironmentClass.PRODUCTION, ExternalProviderMode.LIVE, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("diagnostics cannot be enabled");
    }

    @Test
    void missingConfigurationFailsContextStartup() {
        safetyContext.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseMessage(
                    "DEPLOYMENT_ENVIRONMENT_CLASS must be set explicitly.");
        });
    }

    @Test
    void blankUnknownAndUnsafeConfigurationFailContextStartup() {
        safetyContext.withPropertyValues(
                        "deployment.environment-class=",
                        "external-provider.mode=FIXTURE")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseMessage(
                            "DEPLOYMENT_ENVIRONMENT_CLASS must be set explicitly.");
                });

        safetyContext.withPropertyValues(
                        "deployment.environment-class=LOCAL",
                        "external-provider.mode=")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseMessage(
                            "EXTERNAL_PROVIDER_MODE must be set explicitly.");
                });

        safetyContext.withPropertyValues(
                        "deployment.environment-class=prod-like",
                        "external-provider.mode=LIVE")
                .run(context -> assertThat(context).hasFailed());

        safetyContext.withPropertyValues(
                        "deployment.environment-class=STAGING",
                        "external-provider.mode=FIXTURE")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseMessage(
                            "STAGING must use LIVE provider mode.");
                });
    }

    @Test
    void validExplicitConfigurationStartsSafetyContext() {
        safetyContext.withPropertyValues(
                        "deployment.environment-class=LOCAL",
                        "external-provider.mode=FIXTURE")
                .run(context -> assertThat(context).hasSingleBean(ProviderModeSafety.class));
    }

    @Test
    void diagnosticControllerIsOptIn() {
        diagnosticsContext.run(context -> assertThat(context).doesNotHaveBean(ProviderModeController.class));
        diagnosticsContext.withPropertyValues("provider-diagnostics.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(ProviderModeController.class));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({
            ExternalProviderProperties.class,
            DeploymentProperties.class,
            ProviderDiagnosticsProperties.class,
            FixtureProperties.class
    })
    @Import(ProviderModeSafety.class)
    static class SafetyConfiguration {
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({
            ExternalProviderProperties.class,
            DeploymentProperties.class,
            FixtureProperties.class
    })
    @Import(ProviderModeController.class)
    static class DiagnosticsConfiguration {
    }
}
