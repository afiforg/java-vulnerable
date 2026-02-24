package com.vulnerable.app.controller;

import com.vulnerable.app.model.User;
import com.vulnerable.app.service.JwtService;
import com.vulnerable.app.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = extractToken(authHeader);
            if (!jwtService.validateToken(token)) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
            }

            String username = jwtService.extractUsername(token);
            User user = userService.findByUsername(username);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }

            User response = new User();
            response.setId(user.getId());
            response.setUsername(user.getUsername());
            response.setEmail(user.getEmail());
            response.setRole(user.getRole());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }
    }

    // VULNERABLE: Admin-only endpoint - should require admin role but doesn't check
    @GetMapping("/admin/users")
    public ResponseEntity<?> listAllUsers(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = extractToken(authHeader);
            if (!jwtService.validateToken(token)) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
            }

            // VULNERABLE: Missing role check!
            // Should check: if jwtService.extractRole(token) != "ADMIN", return 403
            String role = jwtService.extractRole(token);
            String username = jwtService.extractUsername(token);
            System.out.println("User " + username + " (role: " + role + ") listing all users");

            List<User> users = userService.getAllUsers();
            // Remove sensitive data from response
            List<Map<String, Object>> userList = users.stream()
                    .map(user -> {
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("id", user.getId());
                        userMap.put("username", user.getUsername());
                        userMap.put("email", user.getEmail());
                        userMap.put("role", user.getRole());
                        return userMap;
                    })
                    .toList();

            return ResponseEntity.ok(Map.of("users", userList));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }
    }

    // VULNERABLE: Admin-only endpoint - should require admin role but doesn't check
    @DeleteMapping("/admin/users/{id}")
    public ResponseEntity<?> deleteUser(@RequestHeader("Authorization") String authHeader, @PathVariable int id) {
        try {
            String token = extractToken(authHeader);
            if (!jwtService.validateToken(token)) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
            }

            // VULNERABLE: Missing role check!
            String role = jwtService.extractRole(token);
            String username = jwtService.extractUsername(token);
            System.out.println("User " + username + " (role: " + role + ") deleting user " + id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "User " + id + " deleted by " + username
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }
    }

    // VULNERABLE: Admin-only endpoint - should require admin role but doesn't check
    @PutMapping("/admin/settings")
    public ResponseEntity<?> updateSystemSettings(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> request) {
        try {
            String token = extractToken(authHeader);
            if (!jwtService.validateToken(token)) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
            }

            // VULNERABLE: Missing role check!
            String role = jwtService.extractRole(token);
            String username = jwtService.extractUsername(token);
            System.out.println("User " + username + " (role: " + role + ") updating system settings");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Setting '" + request.get("setting") + "' updated to '" + 
                              request.get("value") + "' by " + username
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }
    }

    // VULNERABLE: Missing ownership check
    @PutMapping("/users/{id}/settings")
    public ResponseEntity<?> updateUserSettings(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable int id,
            @RequestBody Map<String, String> request) {
        try {
            String token = extractToken(authHeader);
            if (!jwtService.validateToken(token)) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
            }

            // VULNERABLE: Should check if jwtService.extractUserId(token) == id or jwtService.extractRole(token) == "ADMIN"
            // But it doesn't check either condition
            Integer currentUserId = jwtService.extractUserId(token);
            System.out.println("User " + currentUserId + " updating settings for user " + id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Settings for user " + id + " updated by user " + currentUserId
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }
    }

    // VULNERABLE: Missing role check
    @PostMapping("/moderate")
    public ResponseEntity<?> moderateContent(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> request) {
        try {
            String token = extractToken(authHeader);
            if (!jwtService.validateToken(token)) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
            }

            // VULNERABLE: Should check if role is "MODERATOR" or "ADMIN"
            // But it only checks if user is authenticated, not their role
            String role = jwtService.extractRole(token);
            String username = jwtService.extractUsername(token);
            System.out.println("User " + username + " (role: " + role + ") moderating content");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Content " + request.get("contentId") + " moderated by " + username + " (role: " + role + ")"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }
    }

    // VULNERABLE: Information disclosure
    @GetMapping("/users/{id}/details")
    public ResponseEntity<?> getUserDetails(@RequestHeader("Authorization") String authHeader, @PathVariable int id) {
        try {
            String token = extractToken(authHeader);
            if (!jwtService.validateToken(token)) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
            }

            // VULNERABLE: Should check if jwtService.extractUserId(token) == id or jwtService.extractRole(token) == "ADMIN"
            // But it doesn't check ownership

            User user = userService.findById(id);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }

            // VULNERABLE: Exposes sensitive information
            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("role", user.getRole());
            response.put("passwordHash", user.getPasswordHash()); // SHOULD NOT BE EXPOSED
            response.put("apiKey", user.getApiKey()); // SHOULD NOT BE EXPOSED
            response.put("createdAt", user.getCreatedAt());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new IllegalArgumentException("Invalid authorization header");
    }
}
