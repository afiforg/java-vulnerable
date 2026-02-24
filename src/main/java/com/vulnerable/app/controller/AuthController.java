package com.vulnerable.app.controller;

import com.vulnerable.app.dto.LoginRequest;
import com.vulnerable.app.dto.LoginResponse;
import com.vulnerable.app.model.User;
import com.vulnerable.app.service.JwtService;
import com.vulnerable.app.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {
    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        User user = userService.findByUsername(request.getUsername());
        if (user == null || !userService.validatePassword(request.getUsername(), request.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }

        String token = jwtService.generateToken(user.getId(), user.getUsername(), user.getRole());
        
        // Create user response without sensitive data
        User userResponse = new User();
        userResponse.setId(user.getId());
        userResponse.setUsername(user.getUsername());
        userResponse.setEmail(user.getEmail());
        userResponse.setRole(user.getRole());

        return ResponseEntity.ok(new LoginResponse(token, userResponse));
    }
}
