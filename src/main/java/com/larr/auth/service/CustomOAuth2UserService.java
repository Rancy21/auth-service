package com.larr.auth.service;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import com.larr.auth.model.User;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final OAuthService oAuthService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {

        OAuth2User oauthUser = super.loadUser(userRequest);

        // Extract provider name, provider user ID, and email.
        String provider = userRequest.getClientRegistration().getRegistrationId();
        String providerUserId = oauthUser.getName();
        String email = oauthUser.getAttribute("email");

        // process OAuth user
        User user = oAuthService.processOAuthUser(provider, providerUserId, email);

        // Get user roles as authorities
        List<SimpleGrantedAuthority> authorities = user.getRoles().stream().map((role) -> {
            return new SimpleGrantedAuthority(role.getRole());
        }).toList();

        return new DefaultOAuth2User(authorities, oauthUser.getAttributes(), "email");
    }

}
