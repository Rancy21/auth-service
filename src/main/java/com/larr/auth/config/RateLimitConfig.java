package com.larr.auth.config;

import java.time.Duration;
import java.util.Map;

import org.springframework.stereotype.Component;

import io.github.bucket4j.Bandwidth;

@Component
public class RateLimitConfig {
    private Map<String, Bandwidth> endpoints = Map.of(
            "/api/v1/auth/login", Bandwidth.builder().capacity(5).refillGreedy(5, Duration.ofMinutes(1)).build(),
            "/api/v1/auth/register", Bandwidth.builder().capacity(3).refillGreedy(3, Duration.ofHours(1)).build(),
            "/api/v1/auth/password-reset/request",
            Bandwidth.builder().capacity(3).refillGreedy(3, Duration.ofHours(1)).build(),
            "/api/v1/auth/password-reset/confirm",
            Bandwidth.builder().capacity(5).refillGreedy(5, Duration.ofMinutes(10)).build(),
            "/api/v1/auth/verify", Bandwidth.builder().capacity(5).refillGreedy(5, Duration.ofMinutes(10)).build(),
            "/api/v1/auth/resend-verification",
            Bandwidth.builder().capacity(2).refillGreedy(2, Duration.ofMinutes(10)).build());

    public Map<String, Bandwidth> getEndpoints() {
        return endpoints;
    }

    public record Limit(int capacity, Duration refillDuration) {
    }
}
