package com.nour.ali.java_learning_backend.controller;

import com.nour.ali.java_learning_backend.dto.AdminRequestDTO;
import com.nour.ali.java_learning_backend.dto.PasswordUpdateRequestDTO;
import com.nour.ali.java_learning_backend.model.Admin;
import com.nour.ali.java_learning_backend.model.AdminRole;
import com.nour.ali.java_learning_backend.model.Student;
import com.nour.ali.java_learning_backend.service.AdminService;
import com.nour.ali.java_learning_backend.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/admins")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/validate")
    public ResponseEntity<?> validateAdmin(@RequestBody AdminRequestDTO request) {
        var adminOpt = adminService.validateAdmin(request.getName(), request.getPassword());
        if (adminOpt.isPresent()) {
            Admin admin = adminOpt.get();
            String token = jwtService.generateToken(admin.getName(), admin.getRole().name());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("token", token);
            return ResponseEntity.ok(response);
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    @PostMapping("/add")
    public ResponseEntity<?> addAdmin(@RequestBody AdminRequestDTO request, HttpServletRequest httpRequest) {
        try {
            AdminRole role = AdminRole.valueOf(jwtService.extractRole(jwtService.extractToken(httpRequest)));
            if (role != AdminRole.SUPERADMIN) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Super admin access required");
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid or missing role in token");
        }

        String name = request.getName();
        String password = request.getPassword();

        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Admin name is required"));
        }

        boolean exists = adminService.existsByName(name);

        if (exists) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "message", "Admin already exists. Use update endpoint if you wish to modify."
            ));
        }

        Admin admin = new Admin();
        admin.setName(name);
        admin.setPassword(password); // Password will be encoded in service
        admin.setRole(request.getRole() != null ? request.getRole() : AdminRole.ADMIN);

        adminService.addOrUpdateAdmin(admin);

        return ResponseEntity.ok(Map.of(
                "message", "New admin created",
                "admin", name,
                "role", admin.getRole().name()
        ));
    }



    @DeleteMapping("/remove")
    public ResponseEntity<?> removeAdmin(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        try {
            AdminRole role = AdminRole.valueOf(jwtService.extractRole(jwtService.extractToken(httpRequest)));
            if (role != AdminRole.SUPERADMIN) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Super admin access required");
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid or missing role in token");
        }

        String nameToRemove = request.get("name");
        String requester = jwtService.extractUsername(jwtService.extractToken(httpRequest));

        if (nameToRemove.equals(requester)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot remove yourself");
        }

        boolean success = adminService.removeAdmin(nameToRemove);
        if (!success) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin with name '" + nameToRemove + "' not found");
        }

        return ResponseEntity.ok(Map.of("message", "Admin removed"));
    }

    @GetMapping
    public ResponseEntity<?> getAllAdmins() {
        List<Admin> admins = adminService.getAllAdmins();
        return ResponseEntity.ok(admins.stream().map(a -> Map.of(
                "name", a.getName(),
                "role", a.getRole().name()
        )));
    }

    @PostMapping("/contains")
    public ResponseEntity<?> adminExists(@RequestBody Map<String, String> request) {
        boolean exists = adminService.existsByName(request.get("name"));
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    
    @PostMapping("/update-student-password")
    public ResponseEntity<?> updateStudentPassword(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String studentId = request.get("studentId");
        String newPassword = request.get("newPassword");

        if (studentId == null || newPassword == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing studentId or newPassword");
        }

        String adminName;
        try {
            adminName = jwtService.extractUsername(jwtService.extractToken(httpRequest));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }

        Student student = adminService.getStudentById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        // ✅ Check if the current admin added at least one enrollment
        boolean hasEnrollmentByAdmin = student.getEnrollments().stream()
                .anyMatch(e -> e.getAdmin().equals(adminName));

        if (!hasEnrollmentByAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only update passwords for your own students");
        }

        adminService.updateStudentPassword(student, newPassword);

        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }


    @PostMapping("/updatePassword")
    public ResponseEntity<?> updatePasswordForAnyUser(@RequestBody PasswordUpdateRequestDTO requestDto, HttpServletRequest httpRequest) {
        try {
            String role = jwtService.extractRole(jwtService.extractToken(httpRequest));
            if (!"SUPERADMIN".equals(role)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Superadmin access required"));
            }

            if ("student".equalsIgnoreCase(requestDto.getType())) {
                Optional<Student> optionalStudent = adminService.getStudentById(requestDto.getTargetId());
                if (optionalStudent.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Student not found"));
                }
                adminService.updateStudentPassword(optionalStudent.get(), requestDto.getNewPassword());
                return ResponseEntity.ok(Map.of("message", "Student password updated"));
            } else if ("admin".equalsIgnoreCase(requestDto.getType())) {
                Optional<Admin> optionalAdmin = adminService.findByName(requestDto.getTargetId());
                if (optionalAdmin.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Admin not found"));
                }
                adminService.updateAdminPassword(optionalAdmin.get(), requestDto.getNewPassword());
                return ResponseEntity.ok(Map.of("message", "Admin password updated"));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Invalid user type"));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized or invalid token"));
        }
    }


}
