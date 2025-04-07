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
import java.util.stream.Collectors;

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
        Student student = studentRepository.findById(dto.getStudentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student with ID '" + dto.getStudentId() + "' not found"));

        if (!student.isActive() || !student.isPaid()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student has not completed payment");
        }

        // 🔍 Try to find an existing grade
        Grade grade = gradeRepository
                .findByStudentIdAndCourseAndAssignment(dto.getStudentId(), dto.getCourse(), dto.getAssignment())
                .orElse(new Grade());

        // ✍️ Set fields (either update existing or create new)
        grade.setStudentId(dto.getStudentId());
        grade.setCourse(dto.getCourse());
        grade.setAssignment(dto.getAssignment());
        grade.setGrade(dto.getGrade());
        grade.setConsoleOutput(dto.getConsoleOutput());
        grade.setTimestamp(dto.getTimestamp() != null ? dto.getTimestamp() : Instant.now());
        grade.setAdmin(dto.getAdmin());

        Grade saved = gradeRepository.save(grade); // ✅ will UPDATE if grade has an id

        return new GradeResponseDTO(
                saved.getStudentId(),
                saved.getCourse(),
                saved.getAssignment(),
                saved.getGrade(),
                saved.getConsoleOutput(),
                saved.getTimestamp(),
                saved.getAdmin()
        );
    }

    public List<Grade> findGrades(String studentId, String admin, String course, String assignment) {
        return gradeRepository.findAll().stream()
                .filter(g ->
                        (studentId == null || g.getStudentId().equals(studentId)) &&
                                (admin == null || g.getAdmin().equals(admin)) &&
                                (course == null || g.getCourse().equals(course)) &&
                                (assignment == null || g.getAssignment().equals(assignment))
                )
                .collect(Collectors.toList());
    }
}
