package com.nour.ali.java_learning_backend.repository;

import com.nour.ali.java_learning_backend.model.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {

    Optional<Grade> findByStudentIdAndCourseAndAssignment(String studentId, String course, String assignment);

}
