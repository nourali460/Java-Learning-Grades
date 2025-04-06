package com.nour.ali.java_learning_backend.controller;

import com.nour.ali.java_learning_backend.dto.StudentRequestDTO;
import com.nour.ali.java_learning_backend.model.AdminRole;
import com.nour.ali.java_learning_backend.model.Student;
import com.nour.ali.java_learning_backend.service.JwtService;
import com.nour.ali.java_learning_backend.service.StudentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/students")
public class StudentController {

    private final StudentService studentService;
    private final JwtService jwtService;

    @Autowired
    public StudentController(StudentService studentService, JwtService jwtService) {
        this.studentService = studentService;
        this.jwtService = jwtService;
    }

    @PostMapping("/add")
    public ResponseEntity<?> addStudent(@RequestBody StudentRequestDTO dto, HttpServletRequest request) {
        try {
            AdminRole role = AdminRole.valueOf(jwtService.extractRole(jwtService.extractToken(request)));
            if (role != AdminRole.ADMIN && role != AdminRole.SUPERADMIN) {
                System.out.println("❌ Unauthorized attempt to add student by role: " + role);
                return ResponseEntity.status(403).body("{\"message\": \"Forbidden: Admin access required\"}");
            }

            studentService.addOrUpdateStudent(dto);
            System.out.println("✅ Student added/updated: " + dto.getId());
            return ResponseEntity.ok().body("{\"message\": \"Student added/updated\"}");

        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body("{\"message\": \"" + e.getReason() + "\"}");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("{\"message\": \"Unexpected error occurred\"}");
        }
    }




    @DeleteMapping("/remove")
    public ResponseEntity<?> removeStudent(@RequestBody StudentRequestDTO dto, HttpServletRequest request) {
        try {
            AdminRole role = AdminRole.valueOf(jwtService.extractRole(jwtService.extractToken(request)));
            if (role != AdminRole.ADMIN && role != AdminRole.SUPERADMIN) {
                System.out.println("❌ Unauthorized attempt to remove student by role: " + role);
                return ResponseEntity.status(403).body("{\"message\": \"Forbidden: Admin access required\"}");
            }
        } catch (Exception e) {
            return ResponseEntity.status(403).body("{\"message\": \"Invalid or missing role in token\"}");
        }

        boolean removed = studentService.removeStudent(dto.getId());
        if (removed) {
            System.out.println("✅ Student removed: " + dto.getId());
            return ResponseEntity.ok().body("{\"message\": \"Student removed\"}");
        } else {
            return ResponseEntity.status(404).body("{\"message\": \"Student not found\"}");
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateStudent(@RequestBody StudentRequestDTO dto) {
        Optional<Student> optionalStudent = studentService.findById(dto.getId());
        if (optionalStudent.isEmpty()) {
            return ResponseEntity.status(401).body("{\"success\": false}");
        }

        Student student = optionalStudent.get();
        boolean valid = studentService.validatePassword(dto.getPassword(), student.getPassword());

        if (!valid || !student.getAdmin().equals(dto.getAdmin())) {
            return ResponseEntity.status(401).body("{\"success\": false}");
        }

        String token = jwtService.generateToken(student.getId(), "STUDENT");
        System.out.println("✅ Student validated: " + student.getId());
        return ResponseEntity.ok().body("{\"success\": true, \"token\": \"" + token + "\"}");
    }

    @GetMapping("/whoami")
    public ResponseEntity<?> whoAmI(HttpServletRequest request) {
        String token = jwtService.extractToken(request);
        if (token == null) {
            return ResponseEntity.status(401).body("{\"error\": \"Unauthorized\"}");
        }

        String userId = jwtService.extractUsername(token);
        String role = jwtService.extractRole(token);

        return ResponseEntity.ok().body("{\"user\": \"" + userId + "\", \"role\": \"" + role + "\"}");
    }

    @PostMapping("/activate")
    public ResponseEntity<?> activateStudent(@RequestBody Map<String, String> payload, HttpServletRequest request) {
        try {
            AdminRole role = AdminRole.valueOf(jwtService.extractRole(jwtService.extractToken(request)));
            if (role != AdminRole.SUPERADMIN) {
                System.out.println("❌ Unauthorized attempt to activate student by role: " + role);
                return ResponseEntity.status(403).body("{\"message\": \"Forbidden: Superadmin access required\"}");
            }
        } catch (Exception e) {
            return ResponseEntity.status(403).body("{\"message\": \"Invalid or missing role in token\"}");
        }

        String studentId = payload.get("id");
        Optional<Student> optionalStudent = studentService.findById(studentId);

        if (optionalStudent.isEmpty()) {
            return ResponseEntity.status(404).body("{\"message\": \"Student not found\"}");
        }

        Student student = optionalStudent.get();
        student.setActive(true);
        studentService.save(student);

        System.out.println("✅ Student activated: " + studentId);
        return ResponseEntity.ok().body("{\"message\": \"Student activated\"}");
    }

    @PostMapping("/approve")
    public ResponseEntity<?> approveStudent(@RequestBody Map<String, String> payload, HttpServletRequest request) {
        try {
            AdminRole role = AdminRole.valueOf(jwtService.extractRole(jwtService.extractToken(request)));
            if (role != AdminRole.SUPERADMIN) {
                System.out.println("❌ Unauthorized attempt to approve student by role: " + role);
                return ResponseEntity.status(403).body("{\"message\": \"Forbidden: Superadmin access required\"}");
            }
        } catch (Exception e) {
            return ResponseEntity.status(403).body("{\"message\": \"Invalid or missing role in token\"}");
        }

        String studentId = payload.get("id");
        Optional<Student> optionalStudent = studentService.findById(studentId);

        if (optionalStudent.isEmpty()) {
            return ResponseEntity.status(404).body("{\"message\": \"Student not found\"}");
        }

        Student student = optionalStudent.get();
        student.setActive(true);
        student.setPaid(true);
        studentService.save(student);

        System.out.println("✅ Student approved (active + paid): " + studentId);
        return ResponseEntity.ok().body("{\"message\": \"Student marked as active and paid\"}");
    }
}
