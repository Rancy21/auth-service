package com.larr.auth.dto;

import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Validated
public record PasswordResetConfirmRequest(@NotBlank String token, @NotBlank @Size(min = 8) String password) {
}