package com.larr.auth.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.larr.auth.dto.PasswordResetConfirmRequest;
import com.larr.auth.dto.PasswordResetRequest;
import com.larr.auth.service.PasswordResetService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/password-reset")
@Tag(name = "Password Reset", description = "Request and confirm password resets")
public class PasswordResetController {
    private final PasswordResetService passwordResetService;

    @Operation(summary = "Request password reset", description = "Sends a password reset link to the user's email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reset email sent"),
            @ApiResponse(responseCode = "404", description = "No account found with that email")
    })
    @PostMapping("/request")
    public ResponseEntity<Map<String, String>> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.requestPasswordReset(request.email());
        return ResponseEntity.ok(Map.of("message", "A mail to reset your password has been set to your email "));
    }

    @Operation(summary = "Confirm password reset", description = "Validates the reset token and sets a new password")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password reset successfully"),
            @ApiResponse(responseCode = "409", description = "Invalid or already-used reset token"),
            @ApiResponse(responseCode = "400", description = "Validation error (e.g. weak password)")
    })
    @PostMapping("/confirm")
    public ResponseEntity<Map<String, String>> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmRequest request) {
        passwordResetService.confirmPasswordResetToken(request.token(), request.password());
        return ResponseEntity.ok(Map.of("message", "Your password has been reset successfully"));
    }
}