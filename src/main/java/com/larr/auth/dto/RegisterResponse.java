package com.larr.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Registration response")
public record RegisterResponse(
        @Schema(description = "Result message", example = "Registration successful. Please check your email to verify your account") String message,
        @Schema(description = "Registered email", example = "user@examplemail.com") String email) {
}