package com.nour.ali.java_learning_backend.dto;

import java.time.Instant;

public class StudentRequestDTO {

    private String id;
    private String password;
    private String email;
    private String admin;
    private boolean paid;
    private String paymentLink;
    private boolean active;
    private Instant createdAt;
    private Instant paymentDate;

    public StudentRequestDTO() {
    }

    public StudentRequestDTO(String id, String password, String email, String admin,
                             boolean paid, String paymentLink, boolean active,
                             Instant createdAt, Instant paymentDate) {
        this.id = id;
        this.password = password;
        this.email = email;
        this.admin = admin;
        this.paid = paid;
        this.paymentLink = paymentLink;
        this.active = active;
        this.createdAt = createdAt;
        this.paymentDate = paymentDate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAdmin() {
        return admin;
    }

    public void setAdmin(String admin) {
        this.admin = admin;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public String getPaymentLink() {
        return paymentLink;
    }

    public void setPaymentLink(String paymentLink) {
        this.paymentLink = paymentLink;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(Instant paymentDate) {
        this.paymentDate = paymentDate;
    }
}
