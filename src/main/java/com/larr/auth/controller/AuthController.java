package com.larr.auth.controller;

import com.larr.auth.security.jwt.JwtProperties;
import com.larr.auth.security.jwt.JwtUtils;
import com.larr.auth.security.jwt.JwtUtils.TokenPair;
import com.larr.auth.service.EmailVerificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.larr.auth.dto.LoginRequest;
import com.larr.auth.dto.LoginResponse;
import com.larr.auth.dto.LogoutRequest;
import com.larr.auth.dto.RefreshTokenRequest;
import com.larr.auth.dto.RegisterRequest;
import com.larr.auth.dto.RegisterResponse;
import com.larr.auth.exception.UserNotFoundException;
import com.larr.auth.model.User;
import com.larr.auth.repository.UserRepository;
import com.larr.auth.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "Authentication", description = "Register, login, refresh, logout, email verification")
public class AuthController {
    private final JwtProperties jwtProperties;
    private final JwtUtils jwtUtils;
    private final EmailVerificationService emailVerificationService;
    private final AuthService authService;
    private final UserRepository userRepository;

    @Operation(summary = "Register a new user", description = "Creates a new account and sends a verification email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registration successful"),
            @ApiResponse(responseCode = "409", description = "Email already registered")
    })
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @Operation(summary = "Login with email and password")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "returns access + refresh token"),
            @ApiResponse(responseCode = "401", description = "Invalid Credentials or Email not verified")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }

    @Operation(summary = "Verify new user email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful verification"),
            @ApiResponse(responseCode = "409", description = "Invalid or expired verification token")
    })
    @PostMapping("/verify")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        emailVerificationService.verifyEmail(token);
        return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
    }

    @Operation(summary = "Resend verification email")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Verification email sent successfully"),
            @ApiResponse(responseCode = "404", description = "New user not found. Email does not exist"),
            @ApiResponse(responseCode = "409", description = "Email already verified")

    })
    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerificationEmail(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User with email: " + email + " not found"));

        emailVerificationService.sendVerificationToken(user);
        return ResponseEntity.ok(Map.of("message", "Verification email sent"));

    }

    @Operation(summary = "Refresh access token", description = "Rotates refresh token and returns a new access + refresh token pair")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "New token pair issued"),
            @ApiResponse(responseCode = "409", description = "Invalid or already-revoked refresh token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenPair tokenPair = jwtUtils.refreshAccessToken(request.refreshToken());
        return ResponseEntity.ok(new LoginResponse(tokenPair.accessToken(), tokenPair.refreshToken(), "Bearer",
                jwtProperties.accessToken().expiration() / 1000));
    }

    @Operation(summary = "Logout", description = "Revokes the refresh token so it can no longer be used")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logged out successfully"),
            @ApiResponse(responseCode = "409", description = "Invalid refresh token")
    })
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@Valid @RequestBody LogoutRequest request) {
        jwtUtils.revokeRefreshToken(request.refreshToken());
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}