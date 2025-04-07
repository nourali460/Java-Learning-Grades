package com.nour.ali.java_learning_backend.repository;

import com.nour.ali.java_learning_backend.model.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {

    // Optional: Add this if you want to prevent duplicate submissions manually
    boolean existsByStudentIdAndCourseAndAssignment(String studentId, String course, String assignment);
}
