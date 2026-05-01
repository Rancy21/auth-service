package com.larr.auth.security.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.larr.auth.exception.InvalidTokenException;
import com.larr.auth.exception.TokenExpiredException;
import com.larr.auth.model.RefreshToken;
import com.larr.auth.model.User;
import com.larr.auth.model.UserRole;
import com.larr.auth.model.UserStatus;
import com.larr.auth.repository.RefreshTokenRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@ExtendWith(MockitoExtension.class)
public class JwtUtilsTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private JwtUtils jwtUtils;
    private static final String TEST_SECRET = "test-secret-key-that-is-at-least-32-bytes-long-for-hmac-sha-256";

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties(
                TEST_SECRET,
                new JwtProperties.Token(3600000L),
                new JwtProperties.Token(604800000L)
        );
        jwtUtils = new JwtUtils(jwtProperties, refreshTokenRepository);
    }

    private SecretKey getTestSigningKey() {
        byte[] keyBytes = TEST_SECRET.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private User createTestUser() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("test@mail.com")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build();

        UserRole role = new UserRole();
        role.setId(new UserRole.UserRoleId(userId, "USER"));
        role.setRole("USER");
        user.addRole(role);

        return user;
    }

    @Test
    void generateAccessToken_returnsValidToken() {
        User user = createTestUser();

        String token = jwtUtils.generateAccessToken(user);

        assertNotNull(token);
        assertFalse(token.isBlank());

        Claims claims = jwtUtils.validateToken(token);
        assertEquals(user.getId().toString(), claims.getSubject());
        assertEquals(user.getEmail(), claims.get("email", String.class));

        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        assertNotNull(roles);
        assertTrue(roles.contains("USER"));
    }

    @Test
    void validateToken_whenExpired_throwsTokenExpiredException() {
        String expiredToken = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(getTestSigningKey())
                .compact();

        assertThrows(TokenExpiredException.class, () -> jwtUtils.validateToken(expiredToken));
    }

    @Test
    void validateToken_whenInvalidSignature_throwsInvalidTokenException() {
        String invalidToken = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor("a-different-secret-key-that-is-32-bytes!".getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThrows(InvalidTokenException.class, () -> jwtUtils.validateToken(invalidToken));
    }

    @Test
    void validateToken_whenMalformed_throwsInvalidTokenException() {
        assertThrows(InvalidTokenException.class, () -> jwtUtils.validateToken("not-a-valid-jwt"));
    }

    @Test
    void getAuthentication_returnsCorrectAuthentication() {
        User user = createTestUser();
        String token = jwtUtils.generateAccessToken(user);

        Authentication authentication = jwtUtils.getAuthentication(token);

        assertNotNull(authentication);
        assertTrue(authentication instanceof UsernamePasswordAuthenticationToken);
        assertTrue(authentication.isAuthenticated());

        JwtUtils.UserPrincipal principal = (JwtUtils.UserPrincipal) authentication.getPrincipal();
        assertEquals(user.getId(), principal.id());
        assertEquals(user.getEmail(), principal.email());

        assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority("USER")));
    }

    @Test
    void generateRefreshTokenValue_returnsNonEmptyString() {
        String token1 = jwtUtils.generateRefreshTokenValue();
        String token2 = jwtUtils.generateRefreshTokenValue();

        assertNotNull(token1);
        assertNotNull(token2);
        assertFalse(token1.isBlank());
        assertFalse(token2.isBlank());
        assertFalse(token1.equals(token2), "Each refresh token should be unique");
    }

    @Test
    void createRefreshToken_savesTokenEntity() {
        User user = createTestUser();
        String rawToken = jwtUtils.generateRefreshTokenValue();

        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken result = jwtUtils.createRefreshToken(user, rawToken);

        assertNotNull(result);
        assertNotNull(result.getTokenHash());
        assertNotNull(result.getExpiresAt());
        assertEquals(user, result.getUser());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void refreshAccessToken_whenValid_returnsNewTokenPair() {
        User user = createTestUser();
        String rawRefreshToken = jwtUtils.generateRefreshTokenValue();
        String tokenHash = org.apache.commons.codec.digest.DigestUtils.sha256Hex(rawRefreshToken);

        RefreshToken storedToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(storedToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        JwtUtils.TokenPair pair = jwtUtils.refreshAccessToken(rawRefreshToken);

        assertNotNull(pair);
        assertNotNull(pair.accessToken());
        assertNotNull(pair.refreshToken());
        assertFalse(pair.accessToken().isBlank());
        assertFalse(pair.refreshToken().isBlank());

        assertNotNull(storedToken.getRevokedAt(), "Old token should be revoked");

        verify(refreshTokenRepository).findByTokenHash(tokenHash);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void refreshAccessToken_whenTokenNotFound_throwsInvalidTokenException() {
        String rawRefreshToken = jwtUtils.generateRefreshTokenValue();
        String tokenHash = org.apache.commons.codec.digest.DigestUtils.sha256Hex(rawRefreshToken);

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class, () -> jwtUtils.refreshAccessToken(rawRefreshToken));
    }

    @Test
    void refreshAccessToken_whenTokenInactive_throwsInvalidTokenException() {
        User user = createTestUser();
        String rawRefreshToken = jwtUtils.generateRefreshTokenValue();
        String tokenHash = org.apache.commons.codec.digest.DigestUtils.sha256Hex(rawRefreshToken);

        RefreshToken revokedToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plusSeconds(3600))
                .revokedAt(Instant.now())
                .build();

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(revokedToken));

        assertThrows(InvalidTokenException.class, () -> jwtUtils.refreshAccessToken(rawRefreshToken));
    }

    @Test
    void revokeRefreshToken_whenValid_revokesAndSavesToken() {
        User user = createTestUser();
        String rawRefreshToken = jwtUtils.generateRefreshTokenValue();
        String tokenHash = org.apache.commons.codec.digest.DigestUtils.sha256Hex(rawRefreshToken);

        RefreshToken activeToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(activeToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        jwtUtils.revokeRefreshToken(rawRefreshToken);

        assertNotNull(activeToken.getRevokedAt());
        verify(refreshTokenRepository).findByTokenHash(tokenHash);
        verify(refreshTokenRepository).save(activeToken);
    }

    @Test
    void revokeRefreshToken_whenTokenNotFound_throwsInvalidTokenException() {
        String rawRefreshToken = jwtUtils.generateRefreshTokenValue();
        String tokenHash = org.apache.commons.codec.digest.DigestUtils.sha256Hex(rawRefreshToken);

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class, () -> jwtUtils.revokeRefreshToken(rawRefreshToken));
    }
}
