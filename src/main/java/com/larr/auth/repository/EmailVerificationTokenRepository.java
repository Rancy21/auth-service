package com.larr.auth.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.larr.auth.model.EmailVerificationToken;
import com.larr.auth.model.User;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {
    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("Delete from EmailVerificationToken t where t.user = ?1")
    void deleteByUser(User user);

    @Modifying
    @Query("Delete from EmailVerificationToken t where t.expiresAt < ?1")
    void deleteExpiredToken(Instant before);

}
