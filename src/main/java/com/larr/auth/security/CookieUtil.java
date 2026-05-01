package com.larr.auth.security;

import org.springframework.http.ResponseCookie;

import jakarta.servlet.http.Cookie;

public class CookieUtil {
    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    private static final String REFRESH_PATH = "/api/v1/auth";
    private static final long MAX_AGE_SECONDS = 7 * 24 * 60 * 60;

    public static ResponseCookie createreRefreshCookie(String refreshToken) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
                .httpOnly(true) // JavaScript can't read it
                .secure(true) // HTTPS only
                .sameSite("Strict").path(REFRESH_PATH) // Only sent to /api/v1/auth/*
                .maxAge(MAX_AGE_SECONDS) // 7 days
                .build();
    }

    public static ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict").path(REFRESH_PATH)
                .maxAge(0) // Expired
                .build();
    }

    public static String extractRefreshToken(Cookie[] cookies) {
        if (cookies == null)
            return null;
        for (var cookie : cookies) {
            if (REFRESH_TOKEN_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
