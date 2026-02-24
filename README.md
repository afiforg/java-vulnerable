# Vulnerable Java Spring Boot API

This is an intentionally vulnerable Java Spring Boot application designed for testing security scanners. **DO NOT USE IN PRODUCTION.**

## Vulnerability: Flawed Authorization in Java REST API

This application demonstrates **authorization vulnerabilities** in a Java Spring Boot REST API where:
- **Authentication works correctly** - users can log in and receive JWT tokens
- **Authorization is flawed** - authorization checks are missing or insufficient, allowing users to access resources or perform actions beyond their permissions
- **Information disclosure** - sensitive data like password hashes and API keys are exposed

## Vulnerabilities

### 1. **GET /api/admin/users** - Missing Admin Role Check
- **Expected**: Only users with "ADMIN" role should access this endpoint
- **Actual**: Any authenticated user can access it
- **Vulnerability**: The endpoint checks authentication but doesn't verify the role is "ADMIN"
- **Impact**: Regular users can enumerate all users in the system
- **Example exploit:**
  ```bash
  # Login as regular user (alice)
  TOKEN=$(curl -s -X POST http://localhost:8080/api/login \
    -H "Content-Type: application/json" \
    -d '{"username":"alice","password":"alice123"}' | jq -r '.token')
  
  # Access admin endpoint with regular user token - VULNERABLE
  curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/admin/users
  ```

### 2. **DELETE /api/admin/users/{id}** - Missing Admin Authorization
- **Expected**: Only admins should be able to delete users
- **Actual**: Any authenticated user can delete users
- **Vulnerability**: No role verification before allowing user deletion
- **Impact**: Regular users can delete other users, including admins
- **Example exploit:**
  ```bash
  # Login as regular user (bob)
  TOKEN=$(curl -s -X POST http://localhost:8080/api/login \
    -H "Content-Type: application/json" \
    -d '{"username":"bob","password":"bob123"}' | jq -r '.token')
  
  # Delete user as regular user - VULNERABLE
  curl -X DELETE -H "Authorization: Bearer $TOKEN" \
    http://localhost:8080/api/admin/users/1
  ```

### 3. **PUT /api/admin/settings** - Missing Admin Authorization
- **Expected**: Only admins should be able to update system settings
- **Actual**: Any authenticated user can modify system settings
- **Vulnerability**: Authorization check is missing
- **Impact**: Regular users can change critical system configuration
- **Example exploit:**
  ```bash
  # Login as regular user (alice)
  TOKEN=$(curl -s -X POST http://localhost:8080/api/login \
    -H "Content-Type: application/json" \
    -d '{"username":"alice","password":"alice123"}' | jq -r '.token')
  
  # Update system settings as regular user - VULNERABLE
  curl -X PUT -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"setting":"max_users","value":"1000"}' \
    http://localhost:8080/api/admin/settings
  ```

### 4. **PUT /api/users/{id}/settings** - Missing Ownership Check
- **Expected**: Users should only be able to update their own settings, or admins can update any user's settings
- **Actual**: Any authenticated user can update any user's settings
- **Vulnerability**: No check to verify `currentUserID == targetUserID` or `currentUser.role == "ADMIN"`
- **Impact**: Users can modify other users' settings without authorization
- **Example exploit:**
  ```bash
  # Login as alice (user_id=1)
  TOKEN=$(curl -s -X POST http://localhost:8080/api/login \
    -H "Content-Type: application/json" \
    -d '{"username":"alice","password":"alice123"}' | jq -r '.token')
  
  # Update bob's settings (user_id=2) as alice - VULNERABLE
  curl -X PUT -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"setting":"email_notifications","value":"false"}' \
    http://localhost:8080/api/users/2/settings
  ```

### 5. **POST /api/moderate** - Missing Moderator Role Check
- **Expected**: Only users with "MODERATOR" or "ADMIN" role should moderate content
- **Actual**: Any authenticated user can moderate content
- **Vulnerability**: Endpoint checks if user is authenticated but doesn't verify role
- **Impact**: Regular users can perform moderation actions
- **Example exploit:**
  ```bash
  # Login as regular user (bob)
  TOKEN=$(curl -s -X POST http://localhost:8080/api/login \
    -H "Content-Type: application/json" \
    -d '{"username":"bob","password":"bob123"}' | jq -r '.token')
  
  # Moderate content as regular user - VULNERABLE
  curl -X POST -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"contentId":"content123","action":"approve"}' \
    http://localhost:8080/api/moderate
  ```

### 6. **GET /api/users/{id}/details** - Information Disclosure
- **Expected**: Should only return non-sensitive user information
- **Actual**: Returns password hashes and API keys
- **Vulnerability**: Exposes sensitive information that should never be returned
- **Impact**: Attackers can obtain password hashes and API keys
- **Example exploit:**
  ```bash
  # Login as any user
  TOKEN=$(curl -s -X POST http://localhost:8080/api/login \
    -H "Content-Type: application/json" \
    -d '{"username":"alice","password":"alice123"}' | jq -r '.token')
  
  # Get user details - VULNERABLE: Exposes sensitive information
  curl -H "Authorization: Bearer $TOKEN" \
    http://localhost:8080/api/users/2/details
  ```

## Sample Users

The application includes sample users:

- **alice** / alice123 (role: USER, user_id: 1)
- **bob** / bob123 (role: USER, user_id: 2)
- **admin** / admin123 (role: ADMIN, user_id: 3)

## Setup

### Prerequisites

1. Java 17 or later
2. Maven 3.6 or later

### Build and Run

1. Build the project:
   ```bash
   cd java-vulnerable
   mvn clean package
   ```

2. Run the application:
   ```bash
   mvn spring-boot:run
   # Or
   java -jar target/java-vulnerable-1.0.0.jar
   ```

The server will start on `http://localhost:8080`

## Testing

### Test 1: Regular User Accessing Admin Endpoint

```bash
# Login as regular user
TOKEN=$(curl -s -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"alice123"}' | jq -r '.token')

# Try to access admin endpoint - should fail but doesn't
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/admin/users
```

### Test 2: Regular User Deleting Users

```bash
# Login as regular user
TOKEN=$(curl -s -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{"username":"bob","password":"bob123"}' | jq -r '.token')

# Try to delete user - should fail but doesn't
curl -X DELETE -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/admin/users/1
```

### Test 3: Regular User Updating System Settings

```bash
# Login as regular user
TOKEN=$(curl -s -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"alice123"}' | jq -r '.token')

# Try to update system settings - should fail but doesn't
curl -X PUT -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"setting":"maintenance_mode","value":"true"}' \
  http://localhost:8080/api/admin/settings
```

### Test 4: User Modifying Other User's Settings

```bash
# Login as alice
TOKEN=$(curl -s -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"alice123"}' | jq -r '.token')

# Try to update bob's settings - should fail but doesn't
curl -X PUT -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"setting":"theme","value":"dark"}' \
  http://localhost:8080/api/users/2/settings
```

### Test 5: Regular User Moderating Content

```bash
# Login as regular user
TOKEN=$(curl -s -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{"username":"bob","password":"bob123"}' | jq -r '.token')

# Try to moderate content - should fail but doesn't
curl -X POST -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"contentId":"content123","action":"approve"}' \
  http://localhost:8080/api/moderate
```

### Test 6: Information Disclosure

```bash
# Login as any user
TOKEN=$(curl -s -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"alice123"}' | jq -r '.token')

# Get user details - exposes sensitive information
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/users/2/details
```

## Purpose

This code is intentionally vulnerable for security testing purposes only. A security scanner should detect:

- Missing authorization checks in REST endpoints
- Insufficient role-based access control (RBAC)
- Missing ownership verification
- Privilege escalation vulnerabilities
- Information disclosure vulnerabilities
- Horizontal privilege escalation (user accessing other user's resources)
- Vertical privilege escalation (regular user accessing admin functions)

## Security Best Practices (NOT implemented here)

- **Implement proper authorization checks**: Always verify user roles and permissions before allowing access
- **Use role-based access control (RBAC)**: Enforce role checks in controllers or use Spring Security method security
- **Verify ownership**: Check if user owns the resource or has admin privileges before allowing modifications
- **Principle of least privilege**: Users should only have access to resources they need
- **Defense in depth**: Check authorization at multiple layers (controller, service, database)
- **Fail securely**: Default to denying access if authorization cannot be verified
- **Log authorization failures**: Monitor and log unauthorized access attempts
- **Use Spring Security method security**: Use `@PreAuthorize` annotations for declarative authorization
- **Never expose sensitive data**: Password hashes, API keys, and other secrets should never be returned in API responses
- **Use proper error messages**: Don't leak information about resource existence in error messages

## How to Fix

### Fix 1: Add Role Check in Admin Endpoints

```java
@GetMapping("/admin/users")
public ResponseEntity<?> listAllUsers(@RequestHeader("Authorization") String authHeader) {
    String token = extractToken(authHeader);
    if (!jwtService.validateToken(token)) {
        return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
    }

    // FIX: Actually check the role
    String role = jwtService.extractRole(token);
    if (!"ADMIN".equals(role)) {
        return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
    }

    // ... rest of method
}
```

### Fix 2: Add Ownership Check

```java
@PutMapping("/users/{id}/settings")
public ResponseEntity<?> updateUserSettings(
        @RequestHeader("Authorization") String authHeader,
        @PathVariable int id,
        @RequestBody Map<String, String> request) {
    String token = extractToken(authHeader);
    if (!jwtService.validateToken(token)) {
        return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
    }

    // FIX: Check ownership or admin role
    Integer currentUserId = jwtService.extractUserId(token);
    String role = jwtService.extractRole(token);
    if (currentUserId != id && !"ADMIN".equals(role)) {
        return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
    }

    // ... rest of method
}
```

### Fix 3: Remove Sensitive Data from Responses

```java
@GetMapping("/users/{id}/details")
public ResponseEntity<?> getUserDetails(@RequestHeader("Authorization") String authHeader, @PathVariable int id) {
    // ... authentication and authorization checks ...

    // FIX: Don't expose sensitive information
    Map<String, Object> response = new HashMap<>();
    response.put("id", user.getId());
    response.put("username", user.getUsername());
    response.put("email", user.getEmail());
    response.put("role", user.getRole());
    response.put("createdAt", user.getCreatedAt());
    // PasswordHash and ApiKey should NOT be included

    return ResponseEntity.ok(response);
}
```

### Fix 4: Use Spring Security Method Security

```java
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/admin/users")
public ResponseEntity<?> listAllUsers(@RequestHeader("Authorization") String authHeader) {
    // ... implementation
}
```

### Fix 5: Create Custom Authorization Filter

```java
@Component
public class AuthorizationFilter implements Filter {
    @Autowired
    private JwtService jwtService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        // Extract token and verify role
        // Reject requests that don't meet authorization requirements
    }
}
```
