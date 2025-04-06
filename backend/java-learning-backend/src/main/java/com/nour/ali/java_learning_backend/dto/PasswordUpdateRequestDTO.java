// File: dto/PasswordUpdateRequestDTO.java
package com.nour.ali.java_learning_backend.dto;

public class PasswordUpdateRequestDTO {
    private String targetId; // can be student ID or admin name
    private String newPassword;
    private String type; // "admin" or "student"

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
