package com.larr.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.larr.auth.model.RefreshToken;
import com.larr.auth.model.User;

import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Query("""
            select rt from RefreshToken rt where rt.user = ?1
            and rt.revokedAt is null and rt.expiresAt < ?2
            """)
    Optional<RefreshToken> findRefreshTokenForUser(User user, Instant now);

    @Modifying
    @Query("Update RefreshToken rt set rt.revokedAt = ?2 where rt.user = ?1 and rt.revokedAt is null")
    void revokeAllTokens(User user, Instant now);

    @Modifying
    @Query("delete from RefreshToken rt where rt.expiresAt < ?1")
    void deleteExpiredToken(Instant cutOff);

}
