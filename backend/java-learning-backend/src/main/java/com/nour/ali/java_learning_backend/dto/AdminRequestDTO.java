package com.nour.ali.java_learning_backend.dto;

import com.nour.ali.java_learning_backend.model.AdminRole;

public class AdminRequestDTO {

    private String name;
    private String password;
    private AdminRole role; // Now using enum

    public AdminRequestDTO() {
    }

    public AdminRequestDTO(String name, String password, AdminRole role) {
        this.name = name;
        this.password = password;
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public AdminRole getRole() {
        return role;
    }

    public void setRole(AdminRole role) {
        this.role = role;
    }
}
