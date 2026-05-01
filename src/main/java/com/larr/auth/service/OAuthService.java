package com.larr.auth.service;

import org.springframework.stereotype.Service;

import com.larr.auth.model.OAuthAccount;
import com.larr.auth.model.User;
import com.larr.auth.model.UserRole;
import com.larr.auth.model.UserStatus;
import com.larr.auth.repository.OAuthAccountRepository;
import com.larr.auth.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OAuthService {
    private final OAuthAccountRepository oAuthAccountRepository;
    private final UserRepository userRepository;

    @Transactional
    public User processOAuthUser(String provider, String providerUserId, String email) {
        // Look user by email in the DB
        User user = userRepository.findByEmail(email).orElseGet(() -> {

            User newUser = User.builder().email(email).emailVerified(true).status(UserStatus.ACTIVE)
                    .passwordHash(null).build();

            // save user in db
            User savedUser = userRepository.save(newUser);
            UserRole role = new UserRole();

            // Add role
            role.setId(new UserRole.UserRoleId(savedUser.getId(), "USER"));
            savedUser.addRole(role);

            // Create new OAuth account and link it to newly created user
            OAuthAccount oauthAccount = OAuthAccount.builder().emailAtAuth(email).provider(provider)
                    .providerId(providerUserId).user(savedUser).build();
            savedUser.addOAuthAcccount(oauthAccount);

            return savedUser;
        });

        // Check if oauth account is already linked to the user. If not create new oauth
        // account and link it to user
        if (!oAuthAccountRepository.findByProviderAndProviderId(provider, providerUserId).isPresent()) {
            OAuthAccount oauthAccount = OAuthAccount.builder().emailAtAuth(email).provider(provider)
                    .providerId(providerUserId).user(user).build();
            oAuthAccountRepository.save(oauthAccount);
        }

        return user;

    }
}
