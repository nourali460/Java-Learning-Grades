// File: service/StudentService.java
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

    @Autowired
    public StudentService(StudentRepository studentRepository, PasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Student addOrUpdateStudent(StudentRequestDTO dto) {
        if (dto.getId() == null || dto.getId().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student ID cannot be empty");
        }

        Student student = new Student();
        student.setId(dto.getId());
        student.setPassword(passwordEncoder.encode(dto.getPassword()));
        student.setEmail(dto.getEmail());
        student.setAdmin(dto.getAdmin());
        student.setPaid(dto.isPaid());
        student.setPaymentLink(dto.getPaymentLink());
        student.setActive(dto.isActive());
        student.setCreatedAt(dto.getCreatedAt() != null ? dto.getCreatedAt() : Instant.now());
        student.setPaymentDate(dto.getPaymentDate());

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
}
