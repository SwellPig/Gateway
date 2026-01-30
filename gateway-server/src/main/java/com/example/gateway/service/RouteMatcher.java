package com.example.gateway.service;

import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

@Component
public class RouteMatcher {
    private final AntPathMatcher matcher = new AntPathMatcher();

    public boolean matches(String pattern, String path) {
        return matcher.match(pattern, path);
    }

    public String extractPath(String pattern, String path) {
        return matcher.extractPathWithinPattern(pattern, path);
    }
}
