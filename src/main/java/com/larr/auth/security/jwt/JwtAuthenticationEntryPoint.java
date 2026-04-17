package com.larr.auth.security.jwt;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper mapper;

    public JwtAuthenticationEntryPoint(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        boolean tokenExpired = request.getAttribute("tokenExpired") != null;
        String unexpectedError = (String) request.getAttribute("unexpectedError");
        String unexpectedErrorMessage = (String) request.getAttribute("unexpectedErrorMessage");

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("error", "Unauthorized");
        if (tokenExpired) {
            body.put("message", "Token expired");
        } else if (unexpectedError != null) {
            body.put("message", "Authentication failed: " + unexpectedErrorMessage);
            body.put("errorType", unexpectedError);
        } else {
            body.put("message", "Authentication required!");
        }
        body.put("path", request.getRequestURI());

        mapper.writeValue(response.getOutputStream(), body);
    }

}
