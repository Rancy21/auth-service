package com.larr.auth.service;

import java.time.Instant;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.larr.auth.exception.InvalidTokenException;
import com.larr.auth.exception.TokenExpiredException;
import com.larr.auth.exception.UserNotFoundException;
import com.larr.auth.model.PasswordResetToken;
import com.larr.auth.model.User;
import com.larr.auth.repository.PasswordResetTokenRepository;
import com.larr.auth.repository.UserRepository;
import com.larr.auth.security.jwt.JwtUtils;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final EmailService emailService;

    @Transactional
    public void requestPasswordReset(String email) {
        // validate user exitst
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("user with email: " + email + " does not exits"));

        // delete any existing unused tokens for that user
        tokenRepository.deleteByUser(user);

        // generate raw token + hash
        String token = jwtUtils.generateRefreshTokenValue();
        String tokenHash = DigestUtils.sha256Hex(token);

        // save passwordToken entity
        PasswordResetToken resetToken = PasswordResetToken.builder().user(user).tokenHash(tokenHash)
                .expiresAt(Instant.now().plusSeconds(3600)).build();
        tokenRepository.save(resetToken);

        // Send reset token via emailService
        emailService.sendPasswordResetEmail(email, token);
    }

    @Transactional
    public void confirmPasswordResetToken(String token, String rawPassword) {
        // hash raw token
        String tokenHash = DigestUtils.sha256Hex(token);

        // find reset token
        PasswordResetToken resetToken = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Invalid Reset token"));

        // validate not expired, not used
        if (resetToken.isExpired()) {
            throw new TokenExpiredException("Reset token expired");
        }

        if (resetToken.isUsed()) {
            throw new InvalidTokenException("Reset toke already used");
        }

        // encode new password
        String passwordHash = passwordEncoder.encode(rawPassword);

        // update user password
        User user = resetToken.getUser();
        user.setPasswordHash(passwordHash);
        userRepository.save(user);

        // mark token as used
        resetToken.setUsedAt(Instant.now());
        tokenRepository.save(resetToken);
    }
}