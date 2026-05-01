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

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.larr.auth.dto.LoginRequest;
import com.larr.auth.dto.LoginResponse;
import com.larr.auth.dto.RegisterRequest;
import com.larr.auth.dto.RegisterResponse;
import com.larr.auth.exception.EmailAlreadyExistsException;
import com.larr.auth.exception.InvalidCredentialsException;
import com.larr.auth.model.User;
import com.larr.auth.model.UserStatus;
import com.larr.auth.repository.UserRepository;
import com.larr.auth.security.jwt.JwtProperties;
import com.larr.auth.security.jwt.JwtUtils;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private EmailVerificationService emailVerificationService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_whenEmailAlreadyExists_throwsException() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        RegisterRequest request = new RegisterRequest("test@test.com", "password123");

        assertThrows(EmailAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_whenValidRequest_returnSuccess() {
        String email = "test@test.com";
        String password = "password123";
        RegisterRequest request = new RegisterRequest(email, password);
        UUID userId = UUID.randomUUID();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");

        User savedUser = User.builder().id(userId).email(email).passwordHash("hashedPassword").emailVerified(false)
                .status(UserStatus.ACTIVE).build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        doNothing().when(emailVerificationService).sendVerificationToken(any(User.class));

        RegisterResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(response.email(), email);
        assertTrue(response.message().contains("successful"));

        verify(userRepository).existsByEmail(email);
        verify(passwordEncoder).encode(password);
        verify(userRepository).save(any(User.class));
        verify(emailVerificationService).sendVerificationToken(any(User.class));
    }

    @Test
    void authenticate_whenValidCredentials_returnsToken() {
        String email = "test@test.com";
        String password = "password123";
        LoginRequest request = new LoginRequest(email, password);

        User user = User.builder().id(UUID.randomUUID()).email(email).passwordHash("hashedPassword")
                .status(UserStatus.ACTIVE).build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, user.getPasswordHash())).thenReturn(true);
        when(jwtUtils.generateRefreshTokenValue()).thenReturn("rawRefreshToken");
        when(jwtUtils.generateAccessToken(user)).thenReturn("accessToken");

        JwtProperties.Token accessTokenRecord = new JwtProperties.Token(900_000L);
        when(jwtProperties.accessToken()).thenReturn(accessTokenRecord);

        LoginResponse response = authService.authenticate(request);

        assertNotNull(response);
        assertEquals("accessToken", response.accessToken());
        assertEquals("rawRefreshToken", response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900L, response.expiresIn());

        verify(userRepository).findByEmail(email);
        verify(passwordEncoder).matches(password, user.getPasswordHash());
        verify(jwtUtils).generateRefreshTokenValue();
        verify(jwtUtils).createRefreshToken(user, "rawRefreshToken");
        verify(jwtUtils).generateAccessToken(user);
    }

    @Test
    void authenticate_whenEmailNotFound_throwsException() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest("test@mail.com", "password123");

        assertThrows(InvalidCredentialsException.class, () -> authService.authenticate(request));

        verify(userRepository).findByEmail("test@mail.com");

    }

    @Test
    void authenticate_whenWrongPassword_throwsException() {
        String email = "test@mail.com";

        LoginRequest request = new LoginRequest(email, "wrongPassword");

        User user = User.builder().id(UUID.randomUUID()).email(email).passwordHash("hashedPassword")
                .status(UserStatus.ACTIVE).build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.authenticate(request));

        verify(passwordEncoder).matches("wrongPassword", "hashedPassword");

    }

    @Test
    void authenticate_whenUserSuspended_throwsException() {
        String email = "test@mail.com";

        LoginRequest request = new LoginRequest(email, "password123");

        User user = User.builder().id(UUID.randomUUID()).email(email).passwordHash("hashedPassword")
                .status(UserStatus.SUSPENDED).build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThrows(InvalidCredentialsException.class, () -> authService.authenticate(request));
    }

}
