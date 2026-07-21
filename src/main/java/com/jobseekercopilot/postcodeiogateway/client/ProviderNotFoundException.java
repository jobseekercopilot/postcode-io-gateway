package com.jobseekercopilot.postcodeiogateway.client;

public class ProviderNotFoundException extends RuntimeException {
    public ProviderNotFoundException() {
        super("The postcode provider returned no matching location.");
    }
}
