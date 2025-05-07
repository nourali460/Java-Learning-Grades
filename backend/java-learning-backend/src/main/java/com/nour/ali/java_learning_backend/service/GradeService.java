package com.nour.ali.java_learning_backend.service;

import com.nour.ali.java_learning_backend.dto.GradeRequestDTO;
import com.nour.ali.java_learning_backend.dto.GradeResponseDTO;
import com.nour.ali.java_learning_backend.model.Grade;
import com.nour.ali.java_learning_backend.model.Student;
import com.nour.ali.java_learning_backend.repository.GradeRepository;
import com.nour.ali.java_learning_backend.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class GradeService {

    private final GradeRepository gradeRepository;
    private final StudentRepository studentRepository;

    @Autowired
    public GradeService(GradeRepository gradeRepository, StudentRepository studentRepository) {
        this.gradeRepository = gradeRepository;
        this.studentRepository = studentRepository;
    }

    public GradeResponseDTO submitOrUpdateGrade(GradeRequestDTO dto) {
        System.out.println("🚀 submitOrUpdateGrade triggered with data:");
        System.out.println("   ➤ Student ID: " + dto.getStudentId());
        System.out.println("   ➤ Course: " + dto.getCourse());
        System.out.println("   ➤ Assignment: " + dto.getAssignment());
        System.out.println("   ➤ Grade: " + dto.getGrade());
        System.out.println("   ➤ Admin: " + dto.getAdmin());
        System.out.println("   ➤ Semester: " + dto.getSemesterId());
        System.out.println("   ➤ Timestamp: " + dto.getTimestamp());

        Student student = studentRepository.findById(dto.getStudentId())
                .orElseThrow(() -> {
                    System.out.println("❌ Student not found: " + dto.getStudentId());
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found");
                });

        if (!student.isActive() || !student.isPaid()) {
            System.out.println("❌ Student not active/paid. Active: " + student.isActive() + ", Paid: " + student.isPaid());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student has not completed payment");
        }

        System.out.println("✅ Student is active and paid.");

        Optional<Grade> optionalGrade = gradeRepository
                .findByStudentIdAndCourseAndAssignmentAndSemesterId(dto.getStudentId(), dto.getCourse(), dto.getAssignment(), dto.getSemesterId());

        Grade grade;

        if (optionalGrade.isPresent()) {
            grade = optionalGrade.get();
            System.out.println("📝 Found existing grade. Will update: ID (composite) = " +
                    grade.getStudentId() + " | " + grade.getCourse() + " | " + grade.getAssignment() + " | " + grade.getSemesterId());
        } else {
            grade = new Grade();
            System.out.println("➕ No existing grade found. Will create new.");
        }

        // Apply updates
        grade.setStudentId(dto.getStudentId());
        grade.setCourse(dto.getCourse());
        grade.setAssignment(dto.getAssignment());
        grade.setGrade(dto.getGrade());
        grade.setConsoleOutput(dto.getConsoleOutput());
        grade.setTimestamp(dto.getTimestamp() != null ? dto.getTimestamp() : Instant.now());
        grade.setAdmin(dto.getAdmin());
        grade.setSemesterId(dto.getSemesterId());

        // ✅ NEW: Set submitted files
        if (dto.getSubmittedFiles() != null) {
            grade.setSubmittedFiles(dto.getSubmittedFiles());
        }

        Grade saved = gradeRepository.save(grade);
        System.out.println("✅ Grade saved successfully to DB!");

        // ✅ NEW: Return submitted files in response
        return new GradeResponseDTO(
                saved.getStudentId(),
                saved.getCourse(),
                saved.getAssignment(),
                saved.getGrade(),
                saved.getConsoleOutput(),
                saved.getTimestamp(),
                saved.getAdmin(),
                saved.getSemesterId(),
                saved.getSubmittedFiles()
        );
    }

    public List<Grade> findGrades(String studentId, String admin, String course, String assignment, String semesterId) {
        return gradeRepository.findByFilters(studentId, course, assignment, admin, semesterId);
    }
}
