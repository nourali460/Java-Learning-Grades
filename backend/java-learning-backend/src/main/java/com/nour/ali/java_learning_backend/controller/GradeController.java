package com.nour.ali.java_learning_backend.controller;

import com.nour.ali.java_learning_backend.dto.GradeRequestDTO;
import com.nour.ali.java_learning_backend.dto.GradeResponseDTO;
import com.nour.ali.java_learning_backend.model.Grade;
import com.nour.ali.java_learning_backend.model.Student;
import com.nour.ali.java_learning_backend.service.GradeService;
import com.nour.ali.java_learning_backend.service.JwtService;
import com.nour.ali.java_learning_backend.service.StudentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/grades")
public class GradeController {

    private final GradeService gradeService;
    private final JwtService jwtService;
    private final StudentService studentService;

    @Autowired
    public GradeController(GradeService gradeService, JwtService jwtService, StudentService studentService) {
        this.gradeService = gradeService;
        this.jwtService = jwtService;
        this.studentService = studentService;
    }

    // ✅ Public access — no login required
    @GetMapping
    public ResponseEntity<List<Grade>> getGrades(
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String course,
            @RequestParam(required = false) String assignment,
            @RequestParam(required = false) String admin
    ) {
        List<Grade> grades = gradeService.findGrades(studentId, admin, course, assignment);
        return ResponseEntity.ok(grades);
    }

    @PostMapping
    public ResponseEntity<?> submitGrade(@RequestBody GradeRequestDTO dto, HttpServletRequest request) {
        String token = jwtService.extractToken(request);
        String role = jwtService.extractRole(token);

        if (role == null || !role.equalsIgnoreCase("STUDENT")) {
            return ResponseEntity.status(403).body("{\"message\": \"Forbidden: Student access required\"}");
        }

        String studentIdFromToken = jwtService.extractUsername(token);

        // 🔐 Replace the ID in the DTO with the one from the token
        dto.setStudentId(studentIdFromToken);

        Optional<Student> optionalStudent = studentService.findById(studentIdFromToken);
        if (optionalStudent.isEmpty()) {
            return ResponseEntity.status(404).body("{\"message\": \"Student not found\"}");
        }

        Student student = optionalStudent.get();
        if (!student.isActive()) {
            return ResponseEntity.status(403).body("{\"error\": \"Student has not completed payment\"}");
        }

        System.out.println("✅ Grade submitted by student: " + studentIdFromToken +
                " for course: " + dto.getCourse() +
                ", assignment: " + dto.getAssignment());

        GradeResponseDTO response = gradeService.submitOrUpdateGrade(dto);
        return ResponseEntity.ok(response);
    }

}
