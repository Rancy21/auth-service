package com.larr.auth.service;

import org.springframework.stereotype.Service;

import com.larr.auth.model.User;
import com.larr.auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository repository;

    public User saveUser() {
        return null;
    }
}
