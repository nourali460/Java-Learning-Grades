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
        // 🔍 1. Check if student exists in DB
        Optional<Student> optionalStudent = studentService.findById(dto.getId());
        if (optionalStudent.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "error", "Student not found"));
        }

        Student student = optionalStudent.get();

        // 🔐 2. Validate password
        if (!studentService.validatePassword(dto.getPassword(), student.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "error", "Invalid credentials"));
        }

        // 💳 3. Always generate a new Stripe link if student is unpaid
        if (!student.isPaid()) {
            try {
                String newLink = stripeService.generateCheckoutUrl(student.getId());
                student.setPaymentLink(newLink);
                student.setPaid(false);
                student.setActive(false);
                studentService.save(student);

                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "error", "Payment required", "paymentLink", newLink));
            } catch (StripeException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("success", false, "error", "Failed to generate payment link"));
            }
        }

        // 🔓 4. All checks passed: active + paid
        String token = jwtService.generateToken(student.getId(), "STUDENT");

        // 📦 5. Fetch all enrollments for this student under the specified professor (admin)
        List<Enrollment> studentEnrollments = enrollmentRepository.findByStudentIdAndAdmin(dto.getId(), dto.getAdmin());

        // 📊 6. Map enrollments to a list of {course, semesterId} objects
        List<Map<String, String>> enrollments = studentEnrollments.stream()
                .map(e -> Map.of(
                        "course", e.getCourse(),
                        "semesterId", e.getSemesterId()
                ))
                .toList();

        // ✅ 7. Return success response with JWT and filtered enrollments
        return ResponseEntity.ok(Map.of(
                "success", true,
                "token", token,
                "enrollments", enrollments
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

            // Group enrollments by student
            List<Enrollment> enrollments = enrollmentRepository.findByAdmin(admin);

            Map<String, Map<String, Object>> response = new HashMap<>();

            for (Enrollment e : enrollments) {
                String studentId = e.getStudent().getId();
                String email = e.getStudent().getEmail();
                String course = e.getCourse();
                String semester = e.getSemesterId();

                response.computeIfAbsent(studentId, id -> {
                    Map<String, Object> studentData = new HashMap<>();
                    studentData.put("email", email);
                    studentData.put("enrollments", new HashMap<String, String>());
                    return studentData;
                });

                Map<String, String> studentEnrollments = (Map<String, String>) response.get(studentId).get("enrollments");
                studentEnrollments.put(course, semester);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Failed to fetch students"));
        }
    }

}
