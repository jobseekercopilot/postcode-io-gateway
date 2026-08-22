package com.jobseekercopilot.postcodeiogateway.error;

public record ApiError(int status, String code, String message, String correlationId) {
}
