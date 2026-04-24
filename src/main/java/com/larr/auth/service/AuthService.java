package com.larr.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.larr.auth.dto.LoginRequest;
import com.larr.auth.dto.LoginResponse;
import com.larr.auth.dto.RegisterRequest;
import com.larr.auth.dto.RegisterResponse;
import com.larr.auth.exception.EmailAlreadyExistsException;
import com.larr.auth.exception.InvalidCredentialsException;
import com.larr.auth.model.User;
import com.larr.auth.model.UserRole;
import com.larr.auth.model.UserStatus;
import com.larr.auth.repository.UserRepository;
import com.larr.auth.security.jwt.JwtProperties;
import com.larr.auth.security.jwt.JwtUtils;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final JwtProperties jwtProperties;
    private final EmailVerificationService emailVerificationService;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (repository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Email: " + request.email() + " already registered");
        }

        User user = User.builder().email(request.email().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.password())).emailVerified(false).status(UserStatus.ACTIVE)
                .build();

        // First save user to get ID
        User savedUser = repository.save(user);

        // Then add role with the user's ID
        UserRole role = new UserRole();
        role.setId(new UserRole.UserRoleId(savedUser.getId(), "USER"));
        savedUser.addRole(role);

        // Send verification email
        emailVerificationService.sendVerificationToken(savedUser);

        return new RegisterResponse("Registration successful. Please check your email to verify your account",
                savedUser.getEmail());

    }

    @Transactional
    public LoginResponse authenticate(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidCredentialsException("Account not active");
        }

        String rawRefreshToken = jwtUtils.generateRefreshTokenValue();
        jwtUtils.createRefreshToken(user, rawRefreshToken);

        String accessToken = jwtUtils.generateAccessToken(user);

        return new LoginResponse(accessToken, rawRefreshToken, "Bearer",
                jwtProperties.accessToken().expiration() / 1000);

    }
}
