package com.nour.ali.java_learning_backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "admins")
public class Admin {

    @Id
    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdminRole role = AdminRole.ADMIN;

    public Admin() {}

    public Admin(String name, String password, AdminRole role) {
        this.name = name;
        this.password = password;
        this.role = (role != null) ? role : AdminRole.ADMIN;
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
        this.role = (role != null) ? role : AdminRole.ADMIN;
    }

    @Override
    public String toString() {
        return "Admin{name='%s', role='%s'}".formatted(name, role);
    }
}
