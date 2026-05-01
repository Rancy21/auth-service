package com.larr.auth.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.larr.auth.dto.UserProfileResponse;
import com.larr.auth.dto.UserSummaryResponse;
import com.larr.auth.exception.UserNotFoundException;
import com.larr.auth.model.User;
import com.larr.auth.model.UserRole;
import com.larr.auth.repository.UserRepository;
import com.larr.auth.security.jwt.JwtUtils.UserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "User", description = "Authenticated user profile and admin user management")
public class UserController {

    private final UserRepository userRepository;

    @Operation(summary = "Get current user profile", description = "Returns the authenticated user's profile information",
            security = @SecurityRequirement(name = "Bearer"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User profile returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @GetMapping("/api/v1/me")
    public ResponseEntity<UserProfileResponse> getProfile(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        return ResponseEntity.ok(mapToProfile(user));
    }

    @Operation(summary = "List all users", description = "Returns all registered users. Requires ADMIN authority.",
            security = @SecurityRequirement(name = "Bearer"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of user summaries"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions (ADMIN only)")
    })
    @GetMapping("/api/v1/admin/users")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<UserSummaryResponse>> listUsers() {
        List<User> users = userRepository.findAll();

        List<UserSummaryResponse> response = users.stream()
                .map(this::mapToSummary)
                .toList();

        return ResponseEntity.ok(response);
    }

    private UserProfileResponse mapToProfile(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.isEmailVerified(),
                user.getStatus().name(),
                user.getRoles().stream().map(UserRole::getRole).toList(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private UserSummaryResponse mapToSummary(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getEmail(),
                user.isEmailVerified(),
                user.getStatus().name(),
                user.getRoles().stream().map(UserRole::getRole).toList(),
                user.getCreatedAt()
        );
    }
}
