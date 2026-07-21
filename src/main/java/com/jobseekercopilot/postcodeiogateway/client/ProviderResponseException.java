package com.jobseekercopilot.postcodeiogateway.client;

public class ProviderResponseException extends RuntimeException {
    public ProviderResponseException() {
        super("The postcode provider returned an unusable response.");
    }
}
