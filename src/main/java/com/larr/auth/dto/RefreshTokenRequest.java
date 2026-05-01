package com.larr.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@Schema(description = "Token refresh request")
public record RefreshTokenRequest(
        @Schema(description = "Current refresh token", example = "dGhpcyBpcyBh...") @NotBlank String refreshToken) {

}
