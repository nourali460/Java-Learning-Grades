package com.nour.ali.java_learning_backend.dto;

import java.time.Instant;
import java.util.List;

public class StudentResponseDTO {

    private String id;
    private String email;
    private boolean paid;
    private String paymentLink;
    private boolean active;
    private Instant createdAt;
    private Instant paymentDate;

    private List<EnrollmentDTO> enrollments; // ✅ new field

    public StudentResponseDTO() {}

    public StudentResponseDTO(String id, String email, boolean paid, String paymentLink,
                              boolean active, Instant createdAt, Instant paymentDate,
                              List<EnrollmentDTO> enrollments) {
        this.id = id;
        this.email = email;
        this.paid = paid;
        this.paymentLink = paymentLink;
        this.active = active;
        this.createdAt = createdAt;
        this.paymentDate = paymentDate;
        this.enrollments = enrollments;
    }

    // --- Getters & Setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public List<EnrollmentDTO> getEnrollments() {
        return enrollments;
    }

    public void setEnrollments(List<EnrollmentDTO> enrollments) {
        this.enrollments = enrollments;
    }
}
