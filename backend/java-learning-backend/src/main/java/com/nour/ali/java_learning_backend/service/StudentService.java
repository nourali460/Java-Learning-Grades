package com.nour.ali.java_learning_backend.service;

import com.nour.ali.java_learning_backend.dto.StudentRequestDTO;
import com.nour.ali.java_learning_backend.model.Student;
import com.nour.ali.java_learning_backend.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final StripeService stripeService;

    @Autowired
    public StudentService(StudentRepository studentRepository,
                          PasswordEncoder passwordEncoder,
                          StripeService stripeService) {
        this.studentRepository = studentRepository;
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
        student.setPassword(passwordEncoder.encode(dto.getPassword()));
        student.setAdmin(dto.getAdmin());
        student.setSemesterId(dto.getSemesterId()); // ✅ Set semester

        if (existingById.isEmpty()) {
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

        return studentRepository.save(student);
    }

    public boolean removeStudent(String id) {
        if (!studentRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student with ID '" + id + "' not found");
        }
        studentRepository.deleteById(id);
        return true;
    }

    public Optional<Student> findById(String id) {
        Optional<Student> student = studentRepository.findById(id);
        if (student.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student with ID '" + id + "' not found");
        }
        return student;
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
