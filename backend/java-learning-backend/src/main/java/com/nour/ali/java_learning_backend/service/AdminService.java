package com.nour.ali.java_learning_backend.service;

import com.nour.ali.java_learning_backend.model.Admin;
import com.nour.ali.java_learning_backend.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
// Add this import
import java.util.List;

import java.util.Optional;

@Service
public class AdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AdminService(AdminRepository adminRepository, PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<Admin> validateAdmin(String name, String password) {
        Optional<Admin> adminOpt = adminRepository.findByName(name);
        if (adminOpt.isPresent()) {
            Admin admin = adminOpt.get();
            if (passwordEncoder.matches(password, admin.getPassword())) {
                return Optional.of(admin);
            }
        }
        return Optional.empty();
    }

    public Admin addOrUpdateAdmin(Admin admin) {
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        return adminRepository.save(admin);
    }

    public boolean removeAdmin(String name) {
        if (!adminRepository.existsByName(name)) {
            return false;
        }
        adminRepository.deleteById(name);
        return true;
    }

    public boolean existsByName(String name) {
        return adminRepository.existsByName(name);
    }

    public List<Admin> getAllAdmins() {
        return adminRepository.findAll();
    }
}
