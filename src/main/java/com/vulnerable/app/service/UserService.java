package com.vulnerable.app.service;

import com.vulnerable.app.model.User;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserService {
    private static final Map<String, User> users = new HashMap<>();
    private static final Map<String, String> passwords = new HashMap<>();

    static {
        // Initialize sample users
        users.put("alice", new User(1, "alice", "alice@example.com", "USER", 
            sha256("alice123"), "alice_api_key_12345", "2024-01-01T00:00:00Z"));
        users.put("bob", new User(2, "bob", "bob@example.com", "USER", 
            sha256("bob123"), "bob_api_key_67890", "2024-01-02T00:00:00Z"));
        users.put("admin", new User(3, "admin", "admin@example.com", "ADMIN", 
            sha256("admin123"), "admin_api_key_secret", "2024-01-01T00:00:00Z"));

        passwords.put("alice", "alice123");
        passwords.put("bob", "bob123");
        passwords.put("admin", "admin123");
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public User findByUsername(String username) {
        return users.get(username);
    }

    public User findById(int id) {
        return users.values().stream()
                .filter(user -> user.getId() == id)
                .findFirst()
                .orElse(null);
    }

    public boolean validatePassword(String username, String password) {
        String storedPassword = passwords.get(username);
        return storedPassword != null && storedPassword.equals(password);
    }

    public java.util.List<User> getAllUsers() {
        return new java.util.ArrayList<>(users.values());
    }
}
