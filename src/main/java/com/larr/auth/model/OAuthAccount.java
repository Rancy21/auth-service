package com.larr.auth.model;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OAuthAccount {
    private String id;
    private User user;
    private String provider;
    private String providerId;
    private String emailAtAuth;
    private Instant created_at;

}