package com.vulnerable.app.model;

public class User {
    private int id;
    private String username;
    private String email;
    private String role;
    private String passwordHash;
    private String apiKey;
    private String createdAt;

    public User() {}

    public User(int id, String username, String email, String role, String passwordHash, String apiKey, String createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.passwordHash = passwordHash;
        this.apiKey = apiKey;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
