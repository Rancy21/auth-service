package com.larr.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Validated
@Schema(description = "Password reset request")
public record PasswordResetRequest(
        @Schema(description = "Email to send reset link to", example = "user@examplemail.com") @Email @NotBlank String email) {
}