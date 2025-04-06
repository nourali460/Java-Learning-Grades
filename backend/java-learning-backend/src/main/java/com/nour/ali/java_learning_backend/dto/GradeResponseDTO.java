package com.nour.ali.java_learning_backend.dto;

import java.time.Instant;

public class GradeResponseDTO {

    private String studentId;
    private String course;
    private String assignment;
    private String grade;
    private String consoleOutput;
    private Instant timestamp;
    private String admin;

    public GradeResponseDTO() {
    }

    public GradeResponseDTO(String studentId, String course, String assignment, String grade,
                            String consoleOutput, Instant timestamp, String admin) {
        this.studentId = studentId;
        this.course = course;
        this.assignment = assignment;
        this.grade = grade;
        this.consoleOutput = consoleOutput;
        this.timestamp = timestamp;
        this.admin = admin;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public String getAssignment() {
        return assignment;
    }

    public void setAssignment(String assignment) {
        this.assignment = assignment;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getConsoleOutput() {
        return consoleOutput;
    }

    public void setConsoleOutput(String consoleOutput) {
        this.consoleOutput = consoleOutput;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getAdmin() {
        return admin;
    }

    public void setAdmin(String admin) {
        this.admin = admin;
    }
}
