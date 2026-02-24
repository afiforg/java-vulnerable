package com.vulnerable.app.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class HomeController {
    @GetMapping("/")
    public Map<String, Object> home() {
        return Map.of(
            "message", "Vulnerable Java API",
            "endpoints", List.of(
                "POST /api/login - Login (public)",
                "GET /api/profile - Get user profile (authenticated)",
                "GET /api/admin/users - List all users (admin only - VULNERABLE)",
                "DELETE /api/admin/users/{id} - Delete user (admin only - VULNERABLE)",
                "PUT /api/admin/settings - Update system settings (admin only - VULNERABLE)",
                "POST /api/moderate - Moderate content (moderator only - VULNERABLE)",
                "PUT /api/users/{id}/settings - Update user settings (ownership check - VULNERABLE)",
                "GET /api/users/{id}/details - Get user details (information disclosure - VULNERABLE)"
            ),
            "sample_users", Map.of(
                "alice", "alice123 (role: USER)",
                "bob", "bob123 (role: USER)",
                "admin", "admin123 (role: ADMIN)"
            ),
            "vulnerabilities", List.of(
                "Regular users can access admin endpoints",
                "Users can modify other users' settings",
                "Sensitive information (password hash, API key) exposed",
                "Missing authorization checks"
            )
        );
    }
}
