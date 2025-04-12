package com.nour.ali.java_learning_backend.controller;

import com.nour.ali.java_learning_backend.dto.StudentRequestDTO;
import com.nour.ali.java_learning_backend.dto.StudentResponseDTO;
import com.nour.ali.java_learning_backend.model.AdminRole;
import com.nour.ali.java_learning_backend.model.Enrollment;
import com.nour.ali.java_learning_backend.model.Student;
import com.nour.ali.java_learning_backend.repository.EnrollmentRepository;
import com.nour.ali.java_learning_backend.service.AdminService;
import com.nour.ali.java_learning_backend.service.JwtService;
import com.nour.ali.java_learning_backend.service.StripeService;
import com.nour.ali.java_learning_backend.service.StudentService;
import com.stripe.exception.StripeException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/students")
public class StudentController {

    private final StudentService studentService;
    private final JwtService jwtService;
    private final StripeService stripeService;
    private final AdminService adminService;
    private final EnrollmentRepository enrollmentRepository;



    @Autowired
    public StudentController(StudentService studentService,
                             JwtService jwtService,
                             StripeService stripeService,
                             AdminService adminService,
                             EnrollmentRepository enrollmentRepository) {
        this.studentService = studentService;
        this.jwtService = jwtService;
        this.stripeService = stripeService;
        this.adminService = adminService;
        this.enrollmentRepository = enrollmentRepository; // ✅ added
    }


    @PostMapping("/add")
    public ResponseEntity<?> addStudent(@RequestBody StudentRequestDTO dto) {
        try {
            // ✅ Allow only if admin exists
            if (dto.getAdmin() == null || dto.getAdmin().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Admin field is required"));
            }

            boolean adminExists = adminService.existsByName(dto.getAdmin());
            if (!adminExists) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Invalid admin: professor not found"));
            }

            // ✅ Validate semester field
            if (dto.getSemesterId() == null || dto.getSemesterId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Semester ID is required"));
            }

            // ✅ Add or update the student
            var response = studentService.addOrUpdateStudent(dto);
            return ResponseEntity.ok(response); // includes password + info

        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", e.getReason()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Unexpected error occurred"));
        }
    }


    @DeleteMapping("/remove")
    public ResponseEntity<?> removeEnrollment(@RequestBody StudentRequestDTO dto, HttpServletRequest request) {
        try {
            AdminRole role = AdminRole.valueOf(jwtService.extractRole(jwtService.extractToken(request)));
            if (role != AdminRole.ADMIN && role != AdminRole.SUPERADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Forbidden: Admin access required"));
            }

            boolean removed = studentService.removeEnrollment(dto.getId(), dto.getCourse(), dto.getSemesterId());
            return removed
                    ? ResponseEntity.ok(Map.of("message", "Enrollment removed"))
                    : ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Enrollment not found"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Invalid or missing role in token"));
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateStudent(@RequestBody StudentRequestDTO dto) {
        Optional<Student> optionalStudent = studentService.findById(dto.getId());
        if (optionalStudent.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "error", "Student not found"));
        }

        Student student = optionalStudent.get();

        if (!studentService.validatePassword(dto.getPassword(), student.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "error", "Invalid credentials"));
        }

        // Check payment status
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
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of("success", false, "error", "Access expired", "paymentLink", newLink));
                    } catch (StripeException e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of("success", false, "error", "Failed to generate new payment link"));
                    }
                }
            }

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "error", "Payment required", "paymentLink", student.getPaymentLink()));
        }

        // ✅ At this point: Student is valid, active, and paid
        String token = jwtService.generateToken(student.getId(), "STUDENT");

        // ✅ Fetch enrollments from database (filter by student + admin)
        List<Enrollment> studentEnrollments = enrollmentRepository.findByStudentIdAndAdmin(dto.getId(), dto.getAdmin());

        // Extract semesterId (assume all enrollments in same semester for this student+admin)
        String semesterId = studentEnrollments.isEmpty() ? null : studentEnrollments.get(0).getSemesterId();

        // Extract course list
        List<String> courses = studentEnrollments.stream()
                .map(Enrollment::getCourse)
                .toList();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "token", token,
                "semesterId", semesterId,
                "enrollments", courses
        ));
    }


    @GetMapping("/whoami")
    public ResponseEntity<?> whoAmI(HttpServletRequest request) {
        try {
            String token = jwtService.extractToken(request);
            String userId = jwtService.extractUsername(token);
            String role = jwtService.extractRole(token);
            return ResponseEntity.ok(Map.of("user", userId, "role", role));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
    }

    @PostMapping("/activate")
    public ResponseEntity<?> activateStudent(@RequestBody Map<String, String> payload, HttpServletRequest request) {
        try {
            AdminRole role = AdminRole.valueOf(jwtService.extractRole(jwtService.extractToken(request)));
            if (role != AdminRole.SUPERADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Forbidden: Superadmin access required"));
            }

            String studentId = payload.get("id");
            Optional<Student> optionalStudent = studentService.findById(studentId);
            if (optionalStudent.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Student not found"));
            }

            Student student = optionalStudent.get();
            student.setActive(true);
            studentService.save(student);
            return ResponseEntity.ok(Map.of("message", "Student activated"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Invalid or missing role in token"));
        }
    }

    @PostMapping("/approve")
    public ResponseEntity<?> approveStudent(@RequestBody Map<String, String> payload, HttpServletRequest request) {
        try {
            AdminRole role = AdminRole.valueOf(jwtService.extractRole(jwtService.extractToken(request)));
            if (role != AdminRole.SUPERADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Forbidden: Superadmin access required"));
            }

            String studentId = payload.get("id");
            Optional<Student> optionalStudent = studentService.findById(studentId);
            if (optionalStudent.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Student not found"));
            }

            Student student = optionalStudent.get();
            student.setActive(true);
            student.setPaid(true);
            studentService.save(student);
            return ResponseEntity.ok(Map.of("message", "Student marked as active and paid"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Invalid or missing role in token"));
        }
    }

    @GetMapping
    public ResponseEntity<?> getStudentsByAdmin(@RequestParam String admin, HttpServletRequest request) {
        try {
            AdminRole role = AdminRole.valueOf(jwtService.extractRole(jwtService.extractToken(request)));
            if (role != AdminRole.ADMIN && role != AdminRole.SUPERADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Forbidden: Admin access required"));
            }

            List<Student> all = studentService.getStudentsByAdmin(admin);
            List<StudentResponseDTO> dtos = all.stream()
                    .map(studentService::toResponseDTO)
                    .toList();

            return ResponseEntity.ok(dtos);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Failed to fetch students"));
        }
    }
}
