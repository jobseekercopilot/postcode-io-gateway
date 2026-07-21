package com.jobseekercopilot.postcodeiogateway.client;

public class ProviderCircuitOpenException extends RuntimeException {
    public ProviderCircuitOpenException() {
        super("The postcode provider circuit is open.");
    }
}
