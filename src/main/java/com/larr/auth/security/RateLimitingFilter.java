package com.larr.auth.security;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.larr.auth.config.RateLimitConfig;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    private final Map<String, Bandwidth> endpointLimits;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitingFilter(RateLimitConfig config) {
        this.endpointLimits = config.getEndpoints();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();
        Bandwidth limit = findLimit(path);
        if (limit == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIP = getClientIP(request);
        String bucketKey = clientIP + ":" + path;

        Bucket bucket = buckets.computeIfAbsent(bucketKey, key -> Bucket.builder().addLimit(limit).build());
        log.info("RateLimitingFilter: path={}, matchedLimit={}, ip={}", path, limit, clientIP);
        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("""
                        {
                        "status":429,
                        "message": "Too many requests. Please Try again later. "
                        }
                    """);
        }
    }

    private Bandwidth findLimit(String path) {
        for (Map.Entry<String, Bandwidth> entry : endpointLimits.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String getClientIP(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

}