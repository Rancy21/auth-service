package com.larr.auth.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.larr.auth.model.PasswordResetToken;
import com.larr.auth.model.User;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("Delete from PasswordResetToken t where t.user = ?1")
    void deleteByUser(User user);

    @Modifying
    @Query("Delete from PasswordResetToken t where t.expiresAt < ?1 and t.usedAt is null")
    void deleteExipredToken(Instant before);
}
