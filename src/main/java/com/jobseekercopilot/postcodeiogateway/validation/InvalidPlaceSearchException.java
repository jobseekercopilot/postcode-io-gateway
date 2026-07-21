package com.jobseekercopilot.postcodeiogateway.validation;

public class InvalidPlaceSearchException extends RuntimeException {
    public InvalidPlaceSearchException() {
        super("Invalid place search query or limit.");
    }
}
