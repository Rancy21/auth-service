package com.larr.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "user registration request")
public record RegisterRequest(
                @Schema(description = "User email", example = "user@examplemail.com") @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email,

                @Schema(description = "Password", example = "password321", minLength = 8) @NotBlank(message = "Password is required") @Size(min = 8, message = "Password must be at least 8 characters long") String password) {
}