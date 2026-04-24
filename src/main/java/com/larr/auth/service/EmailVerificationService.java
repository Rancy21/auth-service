package com.larr.auth.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import com.larr.auth.exception.InvalidTokenException;
import com.larr.auth.model.EmailVerificationToken;
import com.larr.auth.model.User;
import com.larr.auth.repository.EmailVerificationTokenRepository;
import com.larr.auth.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailService emailService;

    private static final long TOKEN_VALIDITY_HOURS = 24;

    @Transactional
    public void sendVerificationToken(User user) {
        // Check if already verified
        if (user.isEmailVerified()) {
            throw new InvalidTokenException("Email already verified");
        }

        // Delete any existing tokens
        tokenRepository.deleteByUser(user);

        // Generate new token
        String rawToken = UUID.randomUUID().toString().replace("-", "");
        String tokenHash = DigestUtils.sha256Hex(rawToken);

        // Create and save token entiy
        EmailVerificationToken token = EmailVerificationToken.builder().user(user).tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(TOKEN_VALIDITY_HOURS, ChronoUnit.HOURS)).build();

        tokenRepository.save(token);

        // Send email
        emailService.sendVerificationEmail(user.getEmail(), rawToken);

        log.info("Verfication token sent to: {}", user.getEmail());

    }

    @Transactional
    public void verifyEmail(String rawToken) {
        // Hash the token
        String tokenHash = DigestUtils.sha256Hex(rawToken);

        // Find token in DB
        EmailVerificationToken token = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Invalid verification token"));

        // Check expiration
        if (token.isExpired()) {
            throw new InvalidTokenException("Verification token has expired");
        }

        // Get the user
        User user = token.getUser();

        // Mark email as verified
        user.setEmailVerified(true);
        userRepository.save(user);

        // Delete the token (one-time use)
        tokenRepository.delete(token);

        log.info("Email verified for: {}", user.getEmail());
    }
}