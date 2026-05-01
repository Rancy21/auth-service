package com.larr.auth.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.larr.auth.exception.UserNotFoundException;
import com.larr.auth.model.User;
import com.larr.auth.repository.UserRepository;
import com.larr.auth.security.jwt.JwtUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuthAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {
        // Extract email from authenticated OAuth2User
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        String email = oauthUser.getAttribute("email");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("user with email: " + email + " does not exits"));

        // Generate JWT tokens
        String accessToken = jwtUtils.generateAccessToken(user);
        String rawRefreshToken = jwtUtils.generateRefreshTokenValue();
        jwtUtils.createRefreshToken(user, rawRefreshToken);

        // Redierect to frontend
        String targetUrl = "http://localhost:3000/oauth/callback"
                + "?accessToken=" + accessToken
                + "&refreshToken=" + rawRefreshToken;

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
