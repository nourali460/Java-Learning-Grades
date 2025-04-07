// File: repository/GradeRepository.java
package com.nour.ali.java_learning_backend.repository;

import com.nour.ali.java_learning_backend.model.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GradeRepository extends JpaRepository<Grade, GradeId> {
}
