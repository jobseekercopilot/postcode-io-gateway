package com.jobseekercopilot.postcodeiogateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class PostcodeCoveragePropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(CoverageConfiguration.class);

    @Test
    void northernIrelandCoverageDefaultsOffAndRequiresAnExplicitTrueValue() {
        contextRunner.run(context -> assertThat(context
                .getBean(PostcodeCoverageProperties.class)
                .isNorthernIrelandEnabled()).isFalse());

        contextRunner.withPropertyValues("postcodes-io.northern-ireland-enabled=true")
                .run(context -> assertThat(context
                        .getBean(PostcodeCoverageProperties.class)
                        .isNorthernIrelandEnabled()).isTrue());
    }

    @Test
    void runtimeEnvironmentNameAndCheckedInDefaultArePinned() throws Exception {
        String properties = Files.readString(
                Path.of("src/main/resources/application.properties"));
        assertThat(properties).contains(
                "postcodes-io.northern-ireland-enabled="
                        + "${POSTCODES_IO_NORTHERN_IRELAND_ENABLED:false}");
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(PostcodeCoverageProperties.class)
    static class CoverageConfiguration {
    }
}
