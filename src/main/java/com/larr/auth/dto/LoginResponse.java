package com.larr.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Login response with JWT tokens")
public record LoginResponse(
        @Schema(description = "JWT access token", example = "eyJhbGciOiJI...") String accessToken,
        @Schema(description = "Refresh token for obtaining new access tokens", example = "dGhpcyBpcyBh...") String refreshToken,
        @Schema(description = "Token type", example = "Bearer") String tokenType,
        @Schema(description = "Access token expiration in seconds", example = "900") long expiresIn

) {
}