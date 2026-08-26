FROM eclipse-temurin:17-jre-alpine@sha256:90b7615cb81e3a75f69124fb480e48981c7d56dbc9f32c614d789d3a1c3e32fe

# Refresh runtime OpenSSL to the fixed CVE-2026-14456 build.
RUN apk add --no-cache --upgrade \
    libcrypto3=3.5.8-r0 \
    libssl3=3.5.8-r0 \
    openssl=3.5.8-r0

WORKDIR /app

RUN addgroup -S -g 10001 app \
    && adduser -S -D -H -u 10001 -G app app

COPY --chown=10001:10001 target/postcode-io-gateway-1.0.0.jar app.jar
COPY --chown=10001:10001 scripts/container-healthcheck.sh /usr/local/bin/container-healthcheck

EXPOSE 8082

USER 10001:10001

HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=3 \
    CMD ["/usr/local/bin/container-healthcheck"]

ENTRYPOINT ["java", "-jar", "app.jar"]
