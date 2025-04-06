package com.nour.ali.java_learning_backend.repository;

import com.nour.ali.java_learning_backend.model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, String> {
    Optional<Admin> findByName(String name);
    boolean existsByName(String name);
}
