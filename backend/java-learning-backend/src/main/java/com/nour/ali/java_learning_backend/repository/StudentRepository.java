// File: repository/StudentRepository.java
package com.nour.ali.java_learning_backend.repository;

import com.nour.ali.java_learning_backend.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, String> {

    Optional<Student> findByEmail(String email);

    @Query("SELECT s FROM Student s JOIN Enrollment e ON s.id = e.id.studentId " +
            "WHERE e.admin = :admin AND e.id.course = :course AND e.id.semesterId = :semesterId")
    List<Student> findAllByAdminAndCourseAndSemester(String admin, String course, String semesterId);

    List<Student> findByAdminAndCourseAndSemesterId(String admin, String course, String semesterId);

}
