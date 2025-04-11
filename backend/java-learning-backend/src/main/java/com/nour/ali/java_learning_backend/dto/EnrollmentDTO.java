package com.nour.ali.java_learning_backend.dto;

public class EnrollmentDTO {

    private String course;
    private String semesterId;
    private String admin;

    public EnrollmentDTO() {}

    public EnrollmentDTO(String course, String semesterId, String admin) {
        this.course = course;
        this.semesterId = semesterId;
        this.admin = admin;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public String getSemesterId() {
        return semesterId;
    }

    public void setSemesterId(String semesterId) {
        this.semesterId = semesterId;
    }

    public String getAdmin() {
        return admin;
    }

    public void setAdmin(String admin) {
        this.admin = admin;
    }
}
