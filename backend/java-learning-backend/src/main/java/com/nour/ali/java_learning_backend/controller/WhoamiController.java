package com.nour.ali.java_learning_backend.controller;

import com.nour.ali.java_learning_backend.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/whoami")
public class WhoamiController {

    @Autowired
    private JwtService jwtService;

    @GetMapping
    public ResponseEntity<?> identifyUser(HttpServletRequest request) {
        String token = jwtService.extractToken(request);
        if (token == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String user = jwtService.extractUsername(token);
        String role = jwtService.extractRole(token);

        // Optional: validate against known roles
        List<String> allowedRoles = List.of("admin", "superadmin", "student");
        if (!allowedRoles.contains(role)) {
            System.out.println("❌ Unknown role in token for user " + user + ": " + role);
        }

        System.out.println("👤 Whoami request by: " + user + " (role: " + role + ")");

        return ResponseEntity.ok(Map.of(
                "user", user,
                "role", role
        ));
    }
}
