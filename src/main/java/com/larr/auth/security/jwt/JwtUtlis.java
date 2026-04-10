package com.larr.auth.security.jwt;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.larr.auth.exception.InvalidTokenException;
import com.larr.auth.exception.TokenExpiredException;
import com.larr.auth.model.RefreshToken;
import com.larr.auth.model.User;
import com.larr.auth.model.UserRole;
import com.larr.auth.repository.RefreshTokenRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtUtlis {
    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);

        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = Instant.now().plusMillis(jwtProperties.getAccessTokenExpiration());

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.getId().toString());
        claims.put("email", user.getEmail());
        claims.put("roles", extractUserRole(user));

        return Jwts.builder().claims(claims).issuedAt(Date.from(now)).expiration(Date.from(expiry))
                .signWith(getSigningKey()).compact();

    }

    public Claims validateToken(String token) {
        try {
            return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException("Token expired at: " + e.getClaims().getExpiration());
        } catch (JwtException e) {
            throw new InvalidTokenException("Invalid token: " + e.getMessage());
        }
    }

    public Authentication getAuthentication(String token) {
        Claims claims = validateToken(token);

        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        Collection<? extends GrantedAuthority> authorities = roles.stream().map(SimpleGrantedAuthority::new)
                .toList();

        UserPrincipal principal = new UserPrincipal(UUID.fromString(claims.getId()), token);

        return new UsernamePasswordAuthenticationToken(principal, null, authorities);

    }

    public record UserPrincipal(UUID id, String email) {
    }

    public String generateRefreshTokenValue() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public RefreshToken createRefreshToken(User user, String rawToken) {
        String tokenHash = hashToken(rawToken);

        RefreshToken tokenEntity = RefreshToken.builder().user(user)
                .tokenHash(tokenHash).expiresAt(Instant.now().plusMillis(jwtProperties.getRefreshTokenExpiration()))
                .build();

        refreshTokenRepository.save(tokenEntity);

        return tokenEntity;

    }

    @Transactional
    public TokenPair refreshAccessToken(String rawRefreshToken) {
        String tokenHash = hashToken(rawRefreshToken);

        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Token not found"));

        if (!storedToken.isActive()) {
            throw new InvalidTokenException("Token revoked or expired");
        }

        storedToken.revoke();

        User user = storedToken.getUser();
        String newAccessToken = generateAccessToken(user);

        String newRawRefreshToken = generateRefreshTokenValue();
        RefreshToken newRefreshTokenEntity = createRefreshToken(user, newRawRefreshToken);
        newRefreshTokenEntity.setReplacedByToken(storedToken);

        return new TokenPair(newAccessToken, newRawRefreshToken);

    }

    public record TokenPair(String accessToken, String refreshToken) {
    }

    private String hashToken(String token) {
        return DigestUtils.sha256Hex(token);
    }

    private List<String> extractUserRole(User user) {
        return user.getRoles().stream().map(UserRole::getRole).toList();
    }
}
