package com.vulnerable.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class VulnerableApplication {
    public static void main(String[] args) {
        SpringApplication.run(VulnerableApplication.class, args);
        System.out.println("=".repeat(60));
        System.out.println("Vulnerable Java API Server starting on port 8080");
        System.out.println("=".repeat(60));
        System.out.println("Sample users:");
        System.out.println("  - alice / alice123 (role: USER)");
        System.out.println("  - bob / bob123 (role: USER)");
        System.out.println("  - admin / admin123 (role: ADMIN)");
        System.out.println("\nVULNERABILITIES:");
        System.out.println("  - Regular users can access admin endpoints");
        System.out.println("  - Users can modify other users' settings");
        System.out.println("  - Sensitive information exposed");
        System.out.println("  - Missing authorization checks");
        System.out.println("=".repeat(60));
    }
}
