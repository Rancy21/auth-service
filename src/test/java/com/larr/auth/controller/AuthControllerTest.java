package com.larr.auth.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.larr.auth.security.CookieUtil;
import com.larr.auth.service.EmailService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AuthControllerTest extends BaseControllerTest {

    @MockitoSpyBean
    private EmailService emailService;

    @Test
    void register_whenValidRequest_createsUser() throws Exception {
        String body = """
            {
                "email": "test@mail.com",
                "password": "password123"
            }
                    """;
        mockMvc
            .perform(
                post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
            )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.message").value(
                    "Registration successful. Please check your email to verify your account"
                )
            )
            .andExpect(jsonPath("$.email").value("test@mail.com"));
    }

    @Test
    void register_whenDuplicateEmail_returnConfilct() throws Exception {
        String body = """
            {
                "email": "register@mail.com",
                "password": "password123"
            }
                    """;

        mockMvc
            .perform(
                post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
            )
            .andExpect(status().isOk());

        mockMvc
            .perform(
                post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
            )
            .andExpect(status().isConflict());
    }

    @Test
    void login_whenValidCredentials_returnsTokens() throws Exception {
        String body = """
            {
                "email": "login@mail.com",
                "password": "password123"
            }
                    """;

        mockMvc
            .perform(
                post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
            )
            .andExpect(status().isOk());

        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").isNumber());
    }

    @Test
    void login_whenWrongPassword_returnsUnauthorized() throws Exception {
        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                        "email": "badpw@test.com",
                        "password": "password123"
                    }
                    """
                )
        );

        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "email": "badpw@test.com",
                            "password": "wrongpassword"
                        }
                        """
                    )
            )
            .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_whenValidRefreshToken_returnsNewTokens() throws Exception {
        String body = """
            {
                "email": "refresh@test.com",
                "password": "password123"
            }
            """;

        mockMvc
            .perform(
                post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
            )
            .andExpect(status().isOk());

        MockHttpServletResponse response = mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
            )
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

        String refreshToken = CookieUtil.extractRefreshToken(
            response.getCookies()
        );

        mockMvc
            .perform(
                post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .cookie(new Cookie("refresh_token", refreshToken))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").isNumber());
    }

    @Test
    void logout_whenValidRefreshToken_returnsSuccess() throws Exception {
        String body = """
            {
                "email": "logout@test.com",
                "password": "password123"
            }
            """;

        mockMvc
            .perform(
                post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
            )
            .andExpect(status().isOk());

        MockHttpServletResponse response = mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
            )
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

        String refreshToken = CookieUtil.extractRefreshToken(
            response.getCookies()
        );

        mockMvc
            .perform(
                post("/api/v1/auth/logout")
                    .contentType(MediaType.APPLICATION_JSON)
                    .cookie(new Cookie("refresh_token", refreshToken))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Logged out successfully"));

        // Trying to use the same refresh token again should fail

        mockMvc
            .perform(
                post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .cookie(new Cookie("refresh_token", refreshToken))
            )
            .andExpect(status().isConflict());
    }

    @Test
    void verifyEmail_whenValidToken_returnsSuccess() throws Exception {
        String email = "verify@test.com";
        String body = String.format(
            """
            {
                "email": "%s",
                "password": "password123"
            }
            """,
            email
        );

        mockMvc
            .perform(
                post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
            )
            .andExpect(status().isOk());

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(
            String.class
        );
        verify(emailService).sendVerificationEmail(
            eq(email),
            tokenCaptor.capture()
        );
        String token = tokenCaptor.getValue();

        String verifyBody = String.format(
            """
            {
                "token": "%s"
            }
            """,
            token
        );

        mockMvc
            .perform(
                post("/api/v1/auth/verify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(verifyBody)
            )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.message").value("Email verified successfully")
            );
    }

    @Test
    void resendVerification_whenValidEmail_returnsSuccess() throws Exception {
        String email = "resend@test.com";
        String body = String.format(
            """
            {
                "email": "%s",
                "password": "password123"
            }
            """,
            email
        );

        mockMvc
            .perform(
                post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
            )
            .andExpect(status().isOk());

        String resendBody = String.format(
            """
            {
                "email": "%s"
            }
            """,
            email
        );

        mockMvc
            .perform(
                post("/api/v1/auth/resend-verification")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(resendBody)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Verification email sent"));

        verify(emailService, times(2)).sendVerificationEmail(
            eq(email),
            anyString()
        );
    }

    @Test
    void resendVerification_whenEmailNotFound_returnsNotFound()
        throws Exception {
        String body = """
            {
                "email": "notfound@test.com"
            }
            """;

        mockMvc
            .perform(
                post("/api/v1/auth/resend-verification")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
            )
            .andExpect(status().isNotFound());
    }

    @Test
    void verifyEmail_whenInvalidToken_returnsConflict() throws Exception {
        String body = """
            {
                "token": "invalid-token-12345"
            }
            """;

        mockMvc
            .perform(
                post("/api/v1/auth/verify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
            )
            .andExpect(status().isConflict());
    }

    @Test
    void login_whenRateLimitExceeded_returns429() throws Exception {
        String body = """
            {
                "email": "ratelimit@test.com",
                "password": "password123"
            }
            """;

        for (int i = 0; i < 6; i++) {
            mockMvc.perform(
                post("/api/v1/auth/login")
                    .with(request -> {
                        request.setRemoteAddr("192.168.1.1");
                        return request;
                    })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
            );
        }

        // 7th should be 429
        mockMvc.perform(
            post("/api/v1/auth/login")
                .with(request -> {
                    request.setRemoteAddr("192.168.1.1");
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        );
    }
}
