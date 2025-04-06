package com.nour.ali.java_learning_backend.service;

import com.nour.ali.java_learning_backend.model.Admin;
import com.nour.ali.java_learning_backend.model.Student;
import com.nour.ali.java_learning_backend.repository.AdminRepository;
import com.nour.ali.java_learning_backend.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
// Add this import
import java.util.List;

import java.util.Optional;

@Service
public class AdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final StudentRepository studentRepository;

    @Autowired
    public AdminService(AdminRepository adminRepository, PasswordEncoder passwordEncoder, StudentRepository studentRepository) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.studentRepository = studentRepository;
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
        if (adminRepository.existsByName(admin.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Admin with this name already exists");
        }
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        return adminRepository.save(admin);
    }


    public boolean removeAdmin(String name) {
        if (!adminRepository.existsByName(name)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin with name '" + name + "' does not exist");
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

    public Optional<Student> getStudentById(String id) {
        return studentRepository.findById(id);
    }

    public void updateStudentPassword(Student student, String rawPassword) {
        student.setPassword(passwordEncoder.encode(rawPassword));
        studentRepository.save(student);
    }

    public Optional<Admin> findByName(String name) {
        return adminRepository.findById(name);
    }

    public void updateAdminPassword(Admin admin, String rawPassword) {
        admin.setPassword(passwordEncoder.encode(rawPassword));
        adminRepository.save(admin);
    }


}
