package com.jobseekercopilot.postcodeiogateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "postcodes-io")
public class PostcodeCoverageProperties {

    private boolean northernIrelandEnabled;

    public boolean isNorthernIrelandEnabled() {
        return northernIrelandEnabled;
    }

    public void setNorthernIrelandEnabled(boolean northernIrelandEnabled) {
        this.northernIrelandEnabled = northernIrelandEnabled;
    }
}
