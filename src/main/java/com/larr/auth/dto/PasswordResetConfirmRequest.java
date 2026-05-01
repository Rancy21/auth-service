package com.larr.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Validated
@Schema(description = "Password reset confirmation with token and new password")
public record PasswordResetConfirmRequest(
        @Schema(description = "Password reset token from email", example = "dGhpcyBpcyBh...") @NotBlank String token,
        @Schema(description = "New password", example = "newSecurePass123", minLength = 8) @NotBlank @Size(min = 8) String password) {
}