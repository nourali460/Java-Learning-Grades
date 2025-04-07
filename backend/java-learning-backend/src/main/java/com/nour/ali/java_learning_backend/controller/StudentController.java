package com.nour.ali.java_learning_backend.controller;

import com.nour.ali.java_learning_backend.dto.StudentRequestDTO;
import com.nour.ali.java_learning_backend.model.AdminRole;
import com.nour.ali.java_learning_backend.model.Student;
import com.nour.ali.java_learning_backend.service.JwtService;
import com.nour.ali.java_learning_backend.service.StripeService;
import com.nour.ali.java_learning_backend.service.StudentService;
import com.stripe.exception.StripeException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/students")
public class StudentController {

    private final StudentService studentService;
    private final JwtService jwtService;
    private final StripeService stripeService; // ✅ Add this field

    @Autowired
    public StudentController(StudentService studentService, JwtService jwtService, StripeService stripeService) {
        this.studentService = studentService;
        this.jwtService = jwtService;
        this.stripeService = stripeService; // ✅ Assign the service
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
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "error", "Student not found"
            ));
        }

        Student student = optionalStudent.get();

        if (!studentService.validatePassword(dto.getPassword(), student.getPassword())
                || !student.getAdmin().equals(dto.getAdmin())) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "error", "Invalid credentials"
            ));
        }

        // Check if student is unpaid (but not expired)
        if (!student.isPaid()) {
            // If they *were* paid before, check expiration
            if (student.getPaymentDate() != null) {
                Instant now = Instant.now();
                Instant expiration = student.getPaymentDate().plus(365, ChronoUnit.DAYS);
                if (now.isAfter(expiration)) {
                    // Access expired — generate new link and deactivate
                    try {
                        student.setPaid(false);
                        student.setActive(false);
                        String newLink = stripeService.generateCheckoutUrl(student.getId());
                        student.setPaymentLink(newLink);
                        studentService.save(student);
                        System.out.println("⚠️ Student's access expired, new payment link created: " + student.getId());
                        return ResponseEntity.status(403).body(Map.of(
                                "success", false,
                                "error", "Access expired",
                                "paymentLink", newLink
                        ));
                    } catch (StripeException e) {
                        System.out.println("❌ Stripe error while generating new payment link: " + e.getMessage());
                        return ResponseEntity.status(500).body(Map.of(
                                "success", false,
                                "error", "Failed to generate new payment link. Please try again later."
                        ));
                    }
                }
            }

            // Student was never paid — just return existing link
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "error", "Payment required",
                    "paymentLink", student.getPaymentLink()
            ));
        }

        // Student is valid and paid
        String token = jwtService.generateToken(student.getId(), "STUDENT");
        System.out.println("✅ Student validated: " + student.getId());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "token", token
        ));
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
