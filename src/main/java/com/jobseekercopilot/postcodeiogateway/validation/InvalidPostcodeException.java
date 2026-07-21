package com.jobseekercopilot.postcodeiogateway.validation;

public class InvalidPostcodeException extends RuntimeException {
    public InvalidPostcodeException() {
        super("Invalid UK postcode or outcode.");
    }
}
