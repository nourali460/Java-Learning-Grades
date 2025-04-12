package com.nour.ali.java_learning_backend.repository;

import com.nour.ali.java_learning_backend.model.Enrollment;
import com.nour.ali.java_learning_backend.model.EnrollmentId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, EnrollmentId> {

    // Check if a student is already enrolled in a course+semester
    boolean existsById(EnrollmentId id);

    // Get all enrollments by a given admin
    List<Enrollment> findByAdmin(String admin);

    List<Enrollment> findByStudentIdAndAdmin(String studentId, String admin);

    // Optional: Delete a specific enrollment
    void deleteById(EnrollmentId id);
}
