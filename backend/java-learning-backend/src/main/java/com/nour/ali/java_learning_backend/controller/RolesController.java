package com.nour.ali.java_learning_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/roles")
public class RolesController {

    @GetMapping
    public ResponseEntity<List<Map<String, String>>> getRolesTable() {
        List<Map<String, String>> rolesTable = List.of(
                Map.of(
                        "endpoint", "/admins/validate",
                        "method", "POST",
                        "access", "Super Admin, Admin",
                        "description", "Log in, returns JWT"
                ),
                Map.of(
                        "endpoint", "/admins/add",
                        "method", "POST",
                        "access", "Super Admin",
                        "description", "Add new admin"
                ),
                Map.of(
                        "endpoint", "/admins/remove",
                        "method", "DELETE",
                        "access", "Super Admin",
                        "description", "Remove admin"
                ),
                Map.of(
                        "endpoint", "/admins",
                        "method", "GET",
                        "access", "Public",
                        "description", "List admins + roles"
                ),
                Map.of(
                        "endpoint", "/admins/contains",
                        "method", "POST",
                        "access", "Public",
                        "description", "Check if admin exists"
                ),
                Map.of(
                        "endpoint", "/students/add",
                        "method", "POST",
                        "access", "Admin, Super Admin",
                        "description", "Add/update a student"
                ),
                Map.of(
                        "endpoint", "/students/remove",
                        "method", "DELETE",
                        "access", "Admin, Super Admin",
                        "description", "Remove student"
                ),
                Map.of(
                        "endpoint", "/validateStudent",
                        "method", "POST",
                        "access", "Public",
                        "description", "Student login, returns JWT"
                ),
                Map.of(
                        "endpoint", "/grades",
                        "method", "POST",
                        "access", "Student",
                        "description", "Submit or update a grade"
                ),
                Map.of(
                        "endpoint", "/grades",
                        "method", "GET",
                        "access", "Public",
                        "description", "View grades (with filters)"
                ),
                Map.of(
                        "endpoint", "/whoami",
                        "method", "GET",
                        "access", "Authenticated Users",
                        "description", "Identify current user"
                ),
                Map.of(
                        "endpoint", "/roles",
                        "method", "GET",
                        "access", "Public",
                        "description", "View access control table"
                )
        );

        return ResponseEntity.ok(rolesTable);
    }
}
