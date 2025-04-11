package com.nour.ali.java_learning_backend.controller;

import com.nour.ali.java_learning_backend.dto.StudentRequestDTO;
import com.nour.ali.java_learning_backend.dto.StudentResponseDTO;
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
import java.util.List;
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

            if (dto.getSemesterId() == null || dto.getSemesterId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("{\"message\": \"Semester ID is required\"}");
            }

            studentService.addOrUpdateStudent(dto);
            System.out.println("✅ Student added/updated: " + dto.getId() + " (" + dto.getSemesterId() + ")");
            return ResponseEntity.ok().body("{\"message\": \"Student added/updated\"}");

        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body("{\"message\": \"" + e.getReason() + "\"}");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("{\"message\": \"Unexpected error occurred\"}");
        }
    }



    @DeleteMapping("/remove")
    public ResponseEntity<?> removeEnrollment(@RequestBody StudentRequestDTO dto, HttpServletRequest request) {
        try {
            AdminRole role = AdminRole.valueOf(jwtService.extractRole(jwtService.extractToken(request)));
            if (role != AdminRole.ADMIN && role != AdminRole.SUPERADMIN) {
                System.out.println("❌ Unauthorized attempt to remove student enrollment by role: " + role);
                return ResponseEntity.status(403).body("{\"message\": \"Forbidden: Admin access required\"}");
            }
        } catch (Exception e) {
            return ResponseEntity.status(403).body("{\"message\": \"Invalid or missing role in token\"}");
        }

        boolean removed = studentService.removeEnrollment(dto.getId(), dto.getCourse(), dto.getSemesterId());
        if (removed) {
            System.out.println("✅ Enrollment removed: " + dto.getId() + " - " + dto.getCourse() + " (" + dto.getSemesterId() + ")");
            return ResponseEntity.ok().body("{\"message\": \"Enrollment removed\"}");
        } else {
            return ResponseEntity.status(404).body("{\"message\": \"Enrollment not found\"}");
        }
    }


    @PostMapping("/validate")
    public ResponseEntity<?> validateStudent(@RequestBody StudentRequestDTO dto) {
        Optional<Student> optionalStudent = studentService.findById(dto.getId());
        if (optionalStudent.isEmpty()) {
            String errorMessage = "Student not found";
            System.out.println("🔁 Returning error: " + errorMessage);
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "error", errorMessage
            ));
        }

        Student student = optionalStudent.get();

        if (!studentService.validatePassword(dto.getPassword(), student.getPassword())) {
            String errorMessage = "Invalid credentials";
            System.out.println("🔁 Returning error: " + errorMessage);
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "error", errorMessage
            ));
        }

        // Check if student is unpaid (but not expired)
        if (!student.isPaid()) {
            if (student.getPaymentDate() != null) {
                Instant now = Instant.now();
                Instant expiration = student.getPaymentDate().plus(365, ChronoUnit.DAYS);
                if (now.isAfter(expiration)) {
                    try {
                        student.setPaid(false);
                        student.setActive(false);
                        String newLink = stripeService.generateCheckoutUrl(student.getId());
                        student.setPaymentLink(newLink);
                        studentService.save(student);

                        String errorMessage = "Access expired";
                        System.out.println("⚠️ Student's access expired, new payment link created: " + student.getId());
                        return ResponseEntity.status(403).body(Map.of(
                                "success", false,
                                "error", errorMessage,
                                "paymentLink", newLink
                        ));
                    } catch (StripeException e) {
                        String errorMessage = "Failed to generate new payment link. Please try again later.";
                        System.out.println("❌ Stripe error while generating new payment link: " + e.getMessage());
                        return ResponseEntity.status(500).body(Map.of(
                                "success", false,
                                "error", errorMessage
                        ));
                    }
                }
            }

            String errorMessage = "Payment required";
            System.out.println("🔁 Returning error: " + errorMessage + " with existing link: " + student.getPaymentLink());
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "error", errorMessage,
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
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            String userId = jwtService.extractUsername(token);
            String role = jwtService.extractRole(token);

            return ResponseEntity.ok(Map.of(
                    "user", userId,
                    "role", role
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }
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

    @GetMapping
    public ResponseEntity<?> getStudentsByAdmin(@RequestParam String admin, HttpServletRequest request) {
        try {
            AdminRole role = AdminRole.valueOf(jwtService.extractRole(jwtService.extractToken(request)));
            if (role != AdminRole.ADMIN && role != AdminRole.SUPERADMIN) {
                return ResponseEntity.status(403).body("{\"message\": \"Forbidden: Admin access required\"}");
            }

            List<Student> all = studentService.getStudentsByAdmin(admin);
            List<StudentResponseDTO> dtos = all.stream()
                    .map(studentService::toResponseDTO)
                    .toList();

            return ResponseEntity.ok(dtos);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("{\"message\": \"Failed to fetch students\"}");
        }
    }

}
