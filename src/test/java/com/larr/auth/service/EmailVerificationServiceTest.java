package com.larr.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.larr.auth.exception.InvalidTokenException;
import com.larr.auth.model.EmailVerificationToken;
import com.larr.auth.model.User;
import com.larr.auth.model.UserStatus;
import com.larr.auth.repository.EmailVerificationTokenRepository;
import com.larr.auth.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class EmailVerificationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    @Test
    void sendVerificationToken_whenEmailAlreadyVerified_throwsException() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("test@mail.com")
                .emailVerified(true)
                .status(UserStatus.ACTIVE)
                .build();

        assertThrows(InvalidTokenException.class, () -> emailVerificationService.sendVerificationToken(user));
        verify(tokenRepository, never()).deleteByUser(any());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void sendVerificationToken_whenValidUser_sendsEmailAndSavesToken() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("test@mail.com")
                .emailVerified(false)
                .status(UserStatus.ACTIVE)
                .build();

        doNothing().when(tokenRepository).deleteByUser(user);
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(emailService).sendVerificationEmail(anyString(), anyString());

        emailVerificationService.sendVerificationToken(user);

        verify(tokenRepository).deleteByUser(user);
        verify(tokenRepository).save(any(EmailVerificationToken.class));
        verify(emailService).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void verifyEmail_whenInvalidToken_throwsException() {
        String rawToken = "invalid-token";
        String tokenHash = DigestUtils.sha256Hex(rawToken);

        when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class, () -> emailVerificationService.verifyEmail(rawToken));
        verify(tokenRepository).findByTokenHash(tokenHash);
    }

    @Test
    void verifyEmail_whenExpiredToken_throwsException() {
        String rawToken = "expired-token";
        String tokenHash = DigestUtils.sha256Hex(rawToken);

        EmailVerificationToken expiredToken = EmailVerificationToken.builder()
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().minusSeconds(1))
                .build();

        when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(expiredToken));

        InvalidTokenException exception = assertThrows(InvalidTokenException.class,
                () -> emailVerificationService.verifyEmail(rawToken));
        assertTrue(exception.getMessage().contains("expired"));
    }

    @Test
    void verifyEmail_whenValidToken_verifiesEmailAndDeletesToken() {
        String rawToken = "valid-token";
        String tokenHash = DigestUtils.sha256Hex(rawToken);

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("test@mail.com")
                .emailVerified(false)
                .status(UserStatus.ACTIVE)
                .build();

        EmailVerificationToken validToken = EmailVerificationToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();

        when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(validToken));
        when(userRepository.save(any(User.class))).thenReturn(user);
        doNothing().when(tokenRepository).delete(any(EmailVerificationToken.class));

        emailVerificationService.verifyEmail(rawToken);

        assertTrue(user.isEmailVerified());
        verify(userRepository).save(user);
        verify(tokenRepository).delete(validToken);
    }
}
