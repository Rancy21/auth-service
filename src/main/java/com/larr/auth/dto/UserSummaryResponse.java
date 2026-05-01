package com.larr.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "User summary for admin listing")
public record UserSummaryResponse(
        @Schema(description = "User ID", example = "550e8400-e29b-41d4-a716-446655440000") UUID id,
        @Schema(description = "User email", example = "user@examplemail.com") String email,
        @Schema(description = "Whether email is verified", example = "true") boolean emailVerified,
        @Schema(description = "Account status", example = "ACTIVE") String status,
        @Schema(description = "Assigned roles", example = "[\"USER\", \"ADMIN\"]") List<String> roles,
        @Schema(description = "Account creation timestamp") Instant createdAt
) {}
