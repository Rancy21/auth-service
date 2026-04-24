package com.larr.auth.dto;

import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Validated
public record PasswordResetRequest(@Email @NotBlank String email) {
}