package com.larr.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

import com.larr.auth.model.OAuthAccount;
import com.larr.auth.model.User;
import com.larr.auth.model.UserStatus;
import com.larr.auth.repository.OAuthAccountRepository;
import com.larr.auth.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class OAuthServiceTest {

    @Mock
    private OAuthAccountRepository oAuthAccountRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OAuthService oAuthService;

    @Test
    void processOAuthUser_whenNewUser_createsUserAndLinksOAuth() {
        String provider = "google";
        String providerUserId = "google-123";
        String email = "test@mail.com";
        UUID userId = UUID.randomUUID();

        User savedUser = User.builder()
                .id(userId)
                .email(email)
                .emailVerified(true)
                .status(UserStatus.ACTIVE)
                .passwordHash(null)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(oAuthAccountRepository.findByProviderAndProviderId(provider, providerUserId)).thenReturn(Optional.empty());
        when(oAuthAccountRepository.save(any(OAuthAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = oAuthService.processOAuthUser(provider, providerUserId, email);

        assertNotNull(result);
        assertEquals(email, result.getEmail());
        assertTrue(result.isEmailVerified());
        assertEquals(UserStatus.ACTIVE, result.getStatus());

        verify(userRepository).findByEmail(email);
        verify(userRepository).save(any(User.class));
        verify(oAuthAccountRepository).findByProviderAndProviderId(provider, providerUserId);
        verify(oAuthAccountRepository).save(any(OAuthAccount.class));
    }

    @Test
    void processOAuthUser_whenExistingUserAndExistingOAuthAccount_returnsUserWithoutCreatingNewAccount() {
        String provider = "google";
        String providerUserId = "google-123";
        String email = "test@mail.com";
        UUID userId = UUID.randomUUID();

        User existingUser = User.builder()
                .id(userId)
                .email(email)
                .emailVerified(true)
                .status(UserStatus.ACTIVE)
                .build();

        OAuthAccount existingAccount = OAuthAccount.builder()
                .provider(provider)
                .providerId(providerUserId)
                .user(existingUser)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(existingUser));
        when(oAuthAccountRepository.findByProviderAndProviderId(provider, providerUserId))
                .thenReturn(Optional.of(existingAccount));

        User result = oAuthService.processOAuthUser(provider, providerUserId, email);

        assertNotNull(result);
        assertEquals(existingUser, result);

        verify(userRepository).findByEmail(email);
        verify(oAuthAccountRepository).findByProviderAndProviderId(provider, providerUserId);
        verify(oAuthAccountRepository, never()).save(any(OAuthAccount.class));
    }

    @Test
    void processOAuthUser_whenExistingUserButNewProvider_createsOAuthAccount() {
        String provider = "github";
        String providerUserId = "github-456";
        String email = "test@mail.com";
        UUID userId = UUID.randomUUID();

        User existingUser = User.builder()
                .id(userId)
                .email(email)
                .emailVerified(true)
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(existingUser));
        when(oAuthAccountRepository.findByProviderAndProviderId(provider, providerUserId))
                .thenReturn(Optional.empty());
        when(oAuthAccountRepository.save(any(OAuthAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = oAuthService.processOAuthUser(provider, providerUserId, email);

        assertNotNull(result);
        assertEquals(existingUser, result);

        verify(userRepository).findByEmail(email);
        verify(oAuthAccountRepository).findByProviderAndProviderId(provider, providerUserId);
        verify(oAuthAccountRepository).save(any(OAuthAccount.class));
    }
}
