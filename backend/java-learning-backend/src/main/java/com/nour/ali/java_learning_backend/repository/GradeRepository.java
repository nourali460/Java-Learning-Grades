package com.nour.ali.java_learning_backend.repository;

import com.nour.ali.java_learning_backend.model.Grade;
import com.nour.ali.java_learning_backend.model.GradeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GradeRepository extends JpaRepository<Grade, GradeId> {

    Optional<Grade> findByStudentIdAndCourseAndAssignment(String studentId, String course, String assignment);

    @Query("SELECT g FROM Grade g WHERE " +
            "(:studentId IS NULL OR g.studentId = :studentId) AND " +
            "(:course IS NULL OR g.course = :course) AND " +
            "(:assignment IS NULL OR g.assignment = :assignment) AND " +
            "(:admin IS NULL OR g.admin = :admin) AND " +
            "(:semesterId IS NULL OR g.semesterId = :semesterId)")
    List<Grade> findByFilters(
            @Param("studentId") String studentId,
            @Param("course") String course,
            @Param("assignment") String assignment,
            @Param("admin") String admin,
            @Param("semesterId") String semesterId
    );
}

