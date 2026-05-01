package com.larr.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@Schema(description = "Logout request with refresh token to revoke")
public record LogoutRequest(
        @Schema(description = "Refresh token to revoke", example = "dGhpcyBpcyBh...") @NotBlank String refreshToken) {

}
