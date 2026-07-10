package com.smartfinance.backend.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        String issuer,
        long accessExpirationMs,
        long refreshExpirationMs,
        String refreshCookieName,
        boolean refreshCookieSecure,
        String refreshCookieSameSite
) {
}
