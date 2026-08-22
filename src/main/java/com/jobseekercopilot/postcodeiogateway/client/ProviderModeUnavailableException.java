package com.jobseekercopilot.postcodeiogateway.client;

public class ProviderModeUnavailableException extends RuntimeException {
    public ProviderModeUnavailableException() {
        super("The requested provider capability is unavailable in the configured mode.");
    }
}
