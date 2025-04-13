package com.nour.ali.java_learning_backend.dto;

public class EnrolledStudentDTO {
    private String id;
    private String email;
    private String course;
    private String semesterId;

    public EnrolledStudentDTO(String id, String email, String semesterId, String course) {
        this.id = id;
        this.email = email;
        this.semesterId = semesterId;
        this.course = course;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getCourse() {
        return course;
    }

    public String getSemesterId() {
        return semesterId;
    }
}
