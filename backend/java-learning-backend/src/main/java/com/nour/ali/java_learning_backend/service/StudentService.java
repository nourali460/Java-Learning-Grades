package com.nour.ali.java_learning_backend.service;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

    public Student addOrUpdateStudent(StudentRequestDTO dto) {
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
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already in use by another student");
        }

        Student student = existingById.orElse(new Student());
        student.setId(dto.getId());
        student.setEmail(dto.getEmail());

        if (existingById.isEmpty()) {
            student.setPassword(passwordEncoder.encode(dto.getPassword()));
            student.setCreatedAt(Instant.now());
            student.setPaid(false);
            student.setActive(false);
            student.setPaymentDate(null);

            try {
                String checkoutUrl = stripeService.generateCheckoutUrl(dto.getId());
                student.setPaymentLink(checkoutUrl);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate payment link");
            }
        }

        student = studentRepository.save(student);

        // Add new enrollment
        EnrollmentId enrollmentId = new EnrollmentId(dto.getId(), dto.getCourse(), dto.getSemesterId());
        if (enrollmentRepository.existsById(enrollmentId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Student already enrolled in this course/semester");
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setId(enrollmentId);
        enrollment.setStudent(student);
        enrollment.setAdmin(dto.getAdmin());

        enrollmentRepository.save(enrollment);
        return student;
    }

    public boolean removeEnrollment(String username, String course, String semesterId) {
        EnrollmentId id = new EnrollmentId(username, course, semesterId);
        if (!enrollmentRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Enrollment not found");
        }
        enrollmentRepository.deleteById(id);
        return true;
    }

    public Optional<Student> findById(String id) {
        Optional<Student> student = studentRepository.findById(id);
        if (student.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student with ID '" + id + "' not found");
        }
        return student;
    }

    public List<Student> getStudentsByAdmin(String admin) {
        List<Enrollment> enrollments = enrollmentRepository.findByAdmin(admin);
        Set<String> studentIds = enrollments.stream()
                .map(e -> e.getStudent().getId())
                .collect(Collectors.toSet());

        return studentRepository.findAll().stream()
                .filter(s -> studentIds.contains(s.getId()))
                .collect(Collectors.toList());
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

    public boolean validatePassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public Student save(Student student) {
        return studentRepository.save(student);
    }

    public void markStudentAsPaid(String studentId) {
        Optional<Student> optionalStudent = studentRepository.findById(studentId);

        if (optionalStudent.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found");
        }

        Student student = optionalStudent.get();
        student.setPaid(true);
        student.setActive(true);
        student.setPaymentDate(Instant.now());

        studentRepository.save(student);
    }
}
