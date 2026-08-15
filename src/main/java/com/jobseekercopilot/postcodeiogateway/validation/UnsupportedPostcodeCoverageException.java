package com.jobseekercopilot.postcodeiogateway.validation;

public class UnsupportedPostcodeCoverageException extends RuntimeException {
    public UnsupportedPostcodeCoverageException() {
        super("Postcode area is not enabled.");
    }
}
