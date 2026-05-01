package com.larr.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.larr.auth.exception.InvalidTokenException;
import com.larr.auth.exception.TokenExpiredException;
import com.larr.auth.exception.UserNotFoundException;
import com.larr.auth.model.PasswordResetToken;
import com.larr.auth.model.User;
import com.larr.auth.model.UserStatus;
import com.larr.auth.repository.PasswordResetTokenRepository;
import com.larr.auth.repository.UserRepository;
import com.larr.auth.security.jwt.JwtUtils;

@ExtendWith(MockitoExtension.class)
public class PasswordResetServiceTest {
    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    @Test
    void requestPasswordReset_whenUserNotFound_throwsException() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> passwordResetService.requestPasswordReset("test@mail.com"));
        verify(userRepository).findByEmail("test@mail.com");
    }

    @Test
    void requestPasswordReset_whenValidEmail_returnSuccess() {
        String email = "test@mail.com";
        User user = User.builder().id(UUID.randomUUID()).email(email).passwordHash("hashedPassword")
                .emailVerified(false)
                .status(UserStatus.ACTIVE).build();

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(jwtUtils.generateRefreshTokenValue()).thenReturn("rawRefreshToken");

        doNothing().when(emailService).sendPasswordResetEmail(email, "rawRefreshToken");

        passwordResetService.requestPasswordReset(email);

        verify(userRepository).findByEmail(email);
        verify(jwtUtils).generateRefreshTokenValue();
        verify(tokenRepository).deleteByUser(user);
        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(email, "rawRefreshToken");

    }

    @Test
    void confirmPasswordReset_whenInvalidToken_throwsException() {
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class,
                () -> passwordResetService.confirmPasswordResetToken("invalidToken", "newPassword"));
    }

    @Test
    void confirmPasswordReset_whenTokenExpired_throwsException() {
        PasswordResetToken expiredToken = PasswordResetToken.builder()
                .expiresAt(Instant.now().minusSeconds(1))
                .usedAt(null)
                .build();

        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expiredToken));

        assertThrows(TokenExpiredException.class,
                () -> passwordResetService.confirmPasswordResetToken("expiredToken", "newPassword"));
    }

    @Test
    void confirmPasswordReset_whenTokenAlreadyUsed_throwsException() {
        PasswordResetToken usedToken = PasswordResetToken.builder()
                .expiresAt(Instant.now().plusSeconds(3600))
                .usedAt(Instant.now())
                .build();

        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(usedToken));

        assertThrows(InvalidTokenException.class,
                () -> passwordResetService.confirmPasswordResetToken("usedToken", "newPassword"));
    }

    @Test
    void confirmPasswordReset_whenValidToken_updatesPasswordAndMarksUsed() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("test@mail.com")
                .passwordHash("oldHash")
                .status(UserStatus.ACTIVE)
                .build();

        PasswordResetToken validToken = PasswordResetToken.builder()
                .user(user)
                .expiresAt(Instant.now().plusSeconds(3600))
                .usedAt(null)
                .build();

        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(validToken));
        when(passwordEncoder.encode("newPassword")).thenReturn("newHash");

        passwordResetService.confirmPasswordResetToken("validToken", "newPassword");

        verify(passwordEncoder).encode("newPassword");
        verify(userRepository).save(user);
        assertEquals("newHash", user.getPasswordHash());
        verify(tokenRepository).save(validToken);
        assertNotNull(validToken.getUsedAt());
    }
}
