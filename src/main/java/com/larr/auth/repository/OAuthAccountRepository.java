package com.larr.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.larr.auth.model.OAuthAccount;

@Repository
public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, UUID> {
    Optional<OAuthAccount> findByProviderAndProviderId(String provider, String providerUserId);

}
