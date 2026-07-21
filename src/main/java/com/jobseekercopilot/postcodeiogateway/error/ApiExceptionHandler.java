package com.jobseekercopilot.postcodeiogateway.error;

import com.jobseekercopilot.postcodeiogateway.client.ProviderCircuitOpenException;
import com.jobseekercopilot.postcodeiogateway.client.ProviderModeUnavailableException;
import com.jobseekercopilot.postcodeiogateway.client.ProviderNotFoundException;
import com.jobseekercopilot.postcodeiogateway.client.ProviderResponseException;
import com.jobseekercopilot.postcodeiogateway.logging.CorrelationIdFilter;
import com.jobseekercopilot.postcodeiogateway.validation.InvalidPostcodeException;
import com.jobseekercopilot.postcodeiogateway.validation.InvalidPlaceSearchException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.codec.CodecException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(InvalidPostcodeException.class)
    ResponseEntity<ApiError> invalidPostcode(InvalidPostcodeException error, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_POSTCODE",
                "Postcode must be a valid UK postcode or outcode.", error, request);
    }

    @ExceptionHandler(InvalidPlaceSearchException.class)
    ResponseEntity<ApiError> invalidPlaceSearch(InvalidPlaceSearchException error, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_PLACE_SEARCH",
                "Place query or limit is invalid.", error, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> invalidPlaceSearchParameter(
            MethodArgumentTypeMismatchException error,
            HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_PLACE_SEARCH",
                "Place query or limit is invalid.", error, request);
    }

    @ExceptionHandler(ProviderModeUnavailableException.class)
    ResponseEntity<ApiError> providerModeUnavailable(
            ProviderModeUnavailableException error,
            HttpServletRequest request) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, "CAPABILITY_UNAVAILABLE",
                "Place search is unavailable in the configured provider mode.", error, request);
    }

    @ExceptionHandler(ProviderNotFoundException.class)
    ResponseEntity<ApiError> postcodeNotFound(ProviderNotFoundException error, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "POSTCODE_NOT_FOUND",
                "No location was found for the supplied postcode.", error, request);
    }

    @ExceptionHandler(ProviderCircuitOpenException.class)
    ResponseEntity<ApiError> providerUnavailable(ProviderCircuitOpenException error, HttpServletRequest request) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, "PROVIDER_UNAVAILABLE",
                "The postcode service is temporarily unavailable.", error, request);
    }

    @ExceptionHandler(TimeoutException.class)
    ResponseEntity<ApiError> providerTimeout(TimeoutException error, HttpServletRequest request) {
        return response(HttpStatus.GATEWAY_TIMEOUT, "PROVIDER_TIMEOUT",
                "The postcode provider did not respond in time.", error, request);
    }

    @ExceptionHandler({ProviderResponseException.class, CodecException.class,
            WebClientRequestException.class})
    ResponseEntity<ApiError> badProviderResponse(Exception error, HttpServletRequest request) {
        return response(HttpStatus.BAD_GATEWAY, "PROVIDER_BAD_RESPONSE",
                "The postcode provider could not complete the request.", error, request);
    }

    @ExceptionHandler(WebClientResponseException.class)
    ResponseEntity<ApiError> webProviderResponse(WebClientResponseException error, HttpServletRequest request) {
        return providerStatus(error.getStatusCode().value(), error, request);
    }

    @ExceptionHandler(RestClientResponseException.class)
    ResponseEntity<ApiError> fixtureProviderResponse(RestClientResponseException error, HttpServletRequest request) {
        return providerStatus(error.getStatusCode().value(), error, request);
    }

    @ExceptionHandler(RestClientException.class)
    ResponseEntity<ApiError> fixtureProviderFailure(RestClientException error, HttpServletRequest request) {
        return response(HttpStatus.BAD_GATEWAY, "PROVIDER_BAD_RESPONSE",
                "The postcode provider could not complete the request.", error, request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpectedFailure(Exception error, HttpServletRequest request) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "The postcode request could not be completed.", error, request);
    }

    private ResponseEntity<ApiError> providerStatus(int status, Exception error, HttpServletRequest request) {
        if (status == 404) {
            return response(HttpStatus.NOT_FOUND, "POSTCODE_NOT_FOUND",
                    "No location was found for the supplied postcode.", error, request);
        }
        if (status == 408) {
            return response(HttpStatus.GATEWAY_TIMEOUT, "PROVIDER_TIMEOUT",
                    "The postcode provider did not respond in time.", error, request);
        }
        if (status == 429) {
            return response(HttpStatus.TOO_MANY_REQUESTS, "PROVIDER_RATE_LIMITED",
                    "The postcode provider is temporarily rate limited.", error, request);
        }
        return response(HttpStatus.BAD_GATEWAY, "PROVIDER_BAD_RESPONSE",
                "The postcode provider could not complete the request.", error, request);
    }

    private ResponseEntity<ApiError> response(
            HttpStatus status,
            String code,
            String message,
            Exception error,
            HttpServletRequest request) {
        String correlationId = correlationId(request);
        log.warn("provider request failed correlationId={} status={} code={} error={}",
                correlationId, status.value(), code, error.getClass().getSimpleName());
        return ResponseEntity.status(status)
                .body(new ApiError(status.value(), code, message, correlationId));
    }

    private String correlationId(HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
        return value instanceof String text ? text : "unavailable";
    }
}
