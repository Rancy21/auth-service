package com.larr.auth.model;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRole {
    private String id;
    private String role;
    private Instant createdAt;
    private User user;

}