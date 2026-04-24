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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/password-reset")
public class PasswordResetController {
    private final PasswordResetService passwordResetService;

    @PostMapping("/request")
    public ResponseEntity<Map<String, String>> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.requestPasswordReset(request.email());
        return ResponseEntity.ok(Map.of("message", "A mail to reset your password has been set to your email "));
    }

    @PostMapping("/confirm")
    public ResponseEntity<Map<String, String>> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmRequest request) {
        passwordResetService.confirmPasswordResetToken(request.token(), request.password());
        return ResponseEntity.ok(Map.of("message", "Your password has been reset successfully"));
    }
}