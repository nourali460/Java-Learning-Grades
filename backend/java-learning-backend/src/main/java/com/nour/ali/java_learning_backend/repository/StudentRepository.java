// File: repository/StudentRepository.java
package com.nour.ali.java_learning_backend.repository;

import com.nour.ali.java_learning_backend.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, String> {

    Optional<Student> findByEmail(String email);
}
