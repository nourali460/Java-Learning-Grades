package com.nour.ali.java_learning_backend.config;

import com.nour.ali.java_learning_backend.model.Admin;
import com.nour.ali.java_learning_backend.model.AdminRole;
import com.nour.ali.java_learning_backend.repository.AdminRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SuperAdminInitializer {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${superadmin.name}")
    private String superAdminName;

    @Value("${superadmin.password}")
    private String superAdminPassword;

    public SuperAdminInitializer(AdminRepository adminRepository, PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void initSuperAdmin() {
        if (!adminRepository.existsByName(superAdminName)) {
            Admin admin = new Admin();
            admin.setName(superAdminName);
            admin.setPassword(passwordEncoder.encode(superAdminPassword));
            admin.setRole(AdminRole.SUPERADMIN); // ✅ enum instead of string
            adminRepository.save(admin);
            System.out.println("✅ Super Admin created: " + superAdminName);
        } else {
            System.out.println("ℹ️ Super Admin already exists.");
        }
    }
}
