package com.jobseekercopilot.postcodeiogateway.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";
    public static final String SERVICE_MDC_KEY = "serviceName";
    public static final String REQUEST_ATTRIBUTE = CorrelationIdFilter.class.getName() + ".correlationId";

    private static final Pattern SAFE_CORRELATION_ID = Pattern.compile("^[A-Za-z0-9._:-]{1,128}$");

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    private final String serviceName;

    public CorrelationIdFilter(@Value("${spring.application.name:postcode-io-gateway}") String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = request.getHeader(HEADER_NAME);
        if (!StringUtils.hasText(correlationId) || !SAFE_CORRELATION_ID.matcher(correlationId).matches()) {
            correlationId = UUID.randomUUID().toString();
        }

        long startedAt = System.nanoTime();
        String safePath = safePath(request.getRequestURI());
        MDC.put(MDC_KEY, correlationId);
        MDC.put(SERVICE_MDC_KEY, serviceName);
        request.setAttribute(REQUEST_ATTRIBUTE, correlationId);
        response.setHeader(HEADER_NAME, correlationId);

        try {
            log.info("service={} request started method={} path={}", serviceName, request.getMethod(), safePath);
            filterChain.doFilter(request, response);
        } finally {
            if (request.isAsyncStarted()) {
                registerAsyncCompletionLog(request, response, correlationId, safePath, startedAt);
            } else {
                logCompletion(request.getMethod(), safePath, response.getStatus(), startedAt);
            }
            MDC.remove(MDC_KEY);
            MDC.remove(SERVICE_MDC_KEY);
        }
    }

    private void registerAsyncCompletionLog(
            HttpServletRequest request,
            HttpServletResponse response,
            String correlationId,
            String safePath,
            long startedAt) {
        String method = request.getMethod();
        request.getAsyncContext().addListener(new AsyncListener() {
            @Override
            public void onComplete(AsyncEvent event) {
                MDC.put(MDC_KEY, correlationId);
                MDC.put(SERVICE_MDC_KEY, serviceName);
                try {
                    logCompletion(method, safePath, response.getStatus(), startedAt);
                } finally {
                    MDC.remove(MDC_KEY);
                    MDC.remove(SERVICE_MDC_KEY);
                }
            }

            @Override
            public void onTimeout(AsyncEvent event) {
                // Completion records the final servlet status.
            }

            @Override
            public void onError(AsyncEvent event) {
                // The redacted exception handler records the failure class and public code.
            }

            @Override
            public void onStartAsync(AsyncEvent event) {
                event.getAsyncContext().addListener(this);
            }
        });
    }

    private void logCompletion(String method, String safePath, int status, long startedAt) {
        long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
        log.info("service={} request completed method={} path={} status={} durationMs={}",
                serviceName, method, safePath, status, durationMs);
    }

    static String safePath(String requestUri) {
        if (requestUri != null && requestUri.startsWith("/api/postcodes/")) {
            return "/api/postcodes/{postcode}";
        }
        return requestUri;
    }
}
