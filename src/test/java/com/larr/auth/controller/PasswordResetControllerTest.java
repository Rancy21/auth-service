package com.larr.auth.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.larr.auth.service.EmailService;

public class PasswordResetControllerTest extends BaseControllerTest {

    @MockitoSpyBean
    private EmailService emailService;

    @Test
    void requestPasswordReset_whenValidEmail_returnsSuccess() throws Exception {
        String email = "reset@test.com";
        String registerBody = String.format("""
                {
                    "email": "%s",
                    "password": "password123"
                }
                """, email);

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerBody))
                .andExpect(status().isOk());

        String requestBody = String.format("""
                {
                    "email": "%s"
                }
                """, email);

        mockMvc.perform(post("/api/v1/auth/password-reset/request").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("A mail to reset your password has been set to your email "));

        verify(emailService).sendPasswordResetEmail(eq(email), anyString());
    }

    @Test
    void requestPasswordReset_whenEmailNotFound_returnsNotFound() throws Exception {
        String body = """
                {
                    "email": "notfound@test.com"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/password-reset/request").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void confirmPasswordReset_whenValidToken_returnsSuccess() throws Exception {
        String email = "confirm-reset@test.com";
        String registerBody = String.format("""
                {
                    "email": "%s",
                    "password": "password123"
                }
                """, email);

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerBody))
                .andExpect(status().isOk());

        String requestBody = String.format("""
                {
                    "email": "%s"
                }
                """, email);

        mockMvc.perform(post("/api/v1/auth/password-reset/request").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk());

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(eq(email), tokenCaptor.capture());
        String token = tokenCaptor.getValue();

        String confirmBody = String.format("""
                {
                    "token": "%s",
                    "password": "newpassword123"
                }
                """, token);

        mockMvc.perform(post("/api/v1/auth/password-reset/confirm").contentType(MediaType.APPLICATION_JSON).content(confirmBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Your password has been reset successfully"));

        // Verify login with new password works
        String loginBody = String.format("""
                {
                    "email": "%s",
                    "password": "newpassword123"
                }
                """, email);

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void confirmPasswordReset_whenInvalidToken_returnsConflict() throws Exception {
        String body = """
                {
                    "token": "invalid-token",
                    "password": "newpassword123"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/password-reset/confirm").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void confirmPasswordReset_whenWeakPassword_returnsBadRequest() throws Exception {
        String body = """
                {
                    "token": "some-token",
                    "password": "short"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/password-reset/confirm").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }
}
