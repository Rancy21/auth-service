package com.larr.auth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.jayway.jsonpath.JsonPath;
import com.larr.auth.model.User;
import com.larr.auth.model.UserRole;
import com.larr.auth.repository.UserRepository;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class UserControllerTest extends BaseControllerTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private String userAccessToken;
    private String adminAccessToken;

    @BeforeEach
    void setUp() throws Exception {
        // Regular user via API
        registerUser("user@test.com");
        userAccessToken = loginAndGetToken("user@test.com");

        // Admin user via API, then attach ADMIN role
        registerUser("admin@test.com");
        attachAdminRole("admin@test.com");
        adminAccessToken = loginAndGetToken("admin@test.com");
    }

    @AfterEach
    @Transactional
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void getProfile_whenAuthenticated_returnsUserProfile() throws Exception {
        mockMvc.perform(get("/api/v1/me")
                        .header("Authorization", "Bearer " + userAccessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.email").value("user@test.com"))
                .andExpect(jsonPath("$.emailVerified").value(false))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.roles[0]").value("USER"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void getProfile_whenUnauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication required!"));
    }

    @Test
    void listUsers_whenAdmin_returnsUserList() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").isNotEmpty())
                .andExpect(jsonPath("$[0].email").isNotEmpty())
                .andExpect(jsonPath("$[0].roles").isArray());
    }

    @Test
    void listUsers_whenRegularUser_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + userAccessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.messsage").value("Insufficient Permission"));
    }

    @Test
    void listUsers_whenUnauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication required!"));
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private void registerUser(String email) throws Exception {
        String body = String.format("""
                {
                    "email": "%s",
                    "password": "password123"
                }
                """, email);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    private String loginAndGetToken(String email) throws Exception {
        String body = String.format("""
                {
                    "email": "%s",
                    "password": "password123"
                }
                """, email);
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }

    private void attachAdminRole(String email) {
        transactionTemplate.executeWithoutResult(status -> {
            User user = userRepository.findByEmail(email).orElseThrow();
            UserRole adminRole = new UserRole();
            adminRole.setId(new UserRole.UserRoleId(user.getId(), "ADMIN"));
            user.addRole(adminRole);
            userRepository.save(user);
        });
    }
}
