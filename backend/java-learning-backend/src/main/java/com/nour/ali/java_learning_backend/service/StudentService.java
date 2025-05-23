package com.nour.ali.java_learning_backend.service;

import com.nour.ali.java_learning_backend.dto.EnrolledStudentDTO;
import com.nour.ali.java_learning_backend.dto.EnrollmentDTO;
import com.nour.ali.java_learning_backend.dto.StudentRequestDTO;
import com.nour.ali.java_learning_backend.dto.StudentResponseDTO;
import com.nour.ali.java_learning_backend.model.Enrollment;
import com.nour.ali.java_learning_backend.model.EnrollmentId;
import com.nour.ali.java_learning_backend.model.Student;
import com.nour.ali.java_learning_backend.repository.EnrollmentRepository;
import com.nour.ali.java_learning_backend.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final StripeService stripeService;

    @Autowired
    public StudentService(StudentRepository studentRepository,
                          EnrollmentRepository enrollmentRepository,
                          PasswordEncoder passwordEncoder,
                          StripeService stripeService) {
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.stripeService = stripeService;
    }

    public List<Student> getStudentsByAdminCourseSemester(String admin, String course, String semesterId) {
        return studentRepository.findAllByAdminAndCourseAndSemester(admin, course, semesterId);
    }

    public boolean removeEnrollment(String username, String course, String semesterId) {
        EnrollmentId id = new EnrollmentId(username, course, semesterId);
        if (!enrollmentRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Enrollment not found");
        }
        enrollmentRepository.deleteById(id);
        return true;
    }

    public boolean validatePassword(String rawPassword, String storedPassword) {
        // If you're skipping hashing for now:
        return Objects.equals(rawPassword, storedPassword);

        // If hashing is reintroduced later:
        // return passwordEncoder.matches(rawPassword, storedPassword);
    }

    public Student save(Student student) {
        return studentRepository.save(student);
    }

    public Optional<Student> findById(String id) {
        return studentRepository.findById(id);
    }

    @Transactional
    public List<EnrolledStudentDTO> getStudentsByAdmin(String admin) {
        List<Enrollment> enrollments = enrollmentRepository.findByAdmin(admin);

        return enrollments.stream()
                .map(e -> new EnrolledStudentDTO(
                        e.getStudent().getId(),
                        e.getStudent().getEmail(), // ✅ safe here
                        e.getSemesterId(),
                        e.getCourse()
                ))
                .toList();
    }


    public StudentResponseDTO toResponseDTO(Student student) {
        List<EnrollmentDTO> enrollmentDTOs = student.getEnrollments().stream()
                .map(e -> new EnrollmentDTO(e.getCourse(), e.getSemesterId(), e.getAdmin()))
                .collect(Collectors.toList());

        return new StudentResponseDTO(
                student.getId(),
                student.getEmail(),
                student.isPaid(),
                student.getPaymentLink(),
                student.isActive(),
                student.getCreatedAt(),
                student.getPaymentDate(),
                enrollmentDTOs
        );
    }

    public Map<String, Object> addOrUpdateStudent(StudentRequestDTO dto) {
        System.out.println("📥 Incoming student add/update request:");
        System.out.println("  🔹 ID: " + dto.getId());
        System.out.println("  🔹 Email: " + dto.getEmail());
        System.out.println("  🔹 Course: " + dto.getCourse());
        System.out.println("  🔹 Semester: " + dto.getSemesterId());
        System.out.println("  🔹 Admin (from DTO): " + dto.getAdmin());

        String authenticatedUser = SecurityContextHolder.getContext().getAuthentication().getName();
        String adminUsername = authenticatedUser != null && !authenticatedUser.equals("anonymousUser")
                ? authenticatedUser
                : dto.getAdmin();

        if (adminUsername == null || adminUsername.isBlank()) {
            System.out.println("❌ No valid admin in token or DTO.");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session expired or invalid admin.");
        }

        System.out.println("  🔐 Resolved admin: " + adminUsername);

        if (dto.getId() == null || dto.getId().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student ID cannot be empty");
        }

        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email cannot be empty");
        }

        Optional<Student> existingById = studentRepository.findById(dto.getId());
        Optional<Student> existingByEmail = studentRepository.findByEmail(dto.getEmail());

        if (existingByEmail.isPresent() &&
                (!existingById.isPresent() || !existingByEmail.get().getId().equals(dto.getId()))) {
            System.out.println("❌ Email already used by another student.");
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already in use by another student");
        }

        Student student = existingById.orElse(new Student());
        student.setId(dto.getId());
        student.setEmail(dto.getEmail());

        String rawPassword = dto.getPassword();

        if (existingById.isEmpty()) {
            System.out.println("🆕 Creating new student record...");

            if (rawPassword == null || rawPassword.trim().isEmpty()) {
                rawPassword = generateRandomPassword(6);
                System.out.println("  🔐 Auto-generated password: " + rawPassword);
            }

            student.setPassword(rawPassword);
            student.setCreatedAt(Instant.now());
            student.setPaid(false);
            student.setActive(false);
            student.setPaymentDate(null);

            try {
                String checkoutUrl = stripeService.generateCheckoutUrl(dto.getId());
                student.setPaymentLink(checkoutUrl);
                System.out.println("  💳 Stripe checkout URL created");
            } catch (Exception e) {
                System.out.println("❌ Stripe error: " + e.getMessage());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate payment link");
            }
        } else {
            System.out.println("✅ Existing student found: " + dto.getId());
        }

        student = studentRepository.save(student);
        System.out.println("✅ Student saved to DB: " + student.getId());

        // 🔁 Smart enrollment update
        Optional<Enrollment> existingEnrollment = enrollmentRepository
                .findByIdStudentIdAndIdCourseAndAdmin(dto.getId(), dto.getCourse(), adminUsername);

        if (existingEnrollment.isPresent()) {
            System.out.println("🗑 Deleting previous enrollment in course: " + dto.getCourse() +
                    " (was in semester: " + existingEnrollment.get().getSemesterId() + ")");
            enrollmentRepository.delete(existingEnrollment.get());
        }

        EnrollmentId newEnrollmentId = new EnrollmentId(dto.getId(), dto.getCourse(), dto.getSemesterId());
        Enrollment newEnrollment = new Enrollment();
        newEnrollment.setId(newEnrollmentId);
        newEnrollment.setStudent(student);
        newEnrollment.setAdmin(adminUsername);
        enrollmentRepository.save(newEnrollment);
        System.out.println("✅ Created enrollment with updated semester: " + dto.getSemesterId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("studentId", student.getId());
        response.put("email", student.getEmail());
        response.put("password", rawPassword != null ? rawPassword : "unchanged");
        response.put("enrollment", Map.of(
                "course", dto.getCourse(),
                "semesterId", dto.getSemesterId()
        ));

        return response;
    }





    private String generateRandomPassword(int length) {
        if (length < 3) throw new IllegalArgumentException("Password length must be at least 3");

        String letters = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);

        // First 2: lowercase letters
        for (int i = 0; i < 2; i++) {
            sb.append(letters.charAt(random.nextInt(letters.length())));
        }

        // Remaining: digits
        for (int i = 2; i < length; i++) {
            sb.append(digits.charAt(random.nextInt(digits.length())));
        }

        return sb.toString();
    }

}
