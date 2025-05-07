package com.nour.ali.java_learning_backend.dto;

import java.time.Instant;
import java.util.Map;

public class GradeResponseDTO {

    private String studentId;
    private String course;
    private String assignment;
    private String grade;
    private String consoleOutput;
    private Instant timestamp;
    private String admin;
    private String semesterId;

    private Map<String, String> submittedFiles; // ✅ NEW FIELD

    public GradeResponseDTO() {
    }

    public GradeResponseDTO(String studentId, String course, String assignment, String grade,
                            String consoleOutput, Instant timestamp, String admin,
                            String semesterId, Map<String, String> submittedFiles) {
        this.studentId = studentId;
        this.course = course;
        this.assignment = assignment;
        this.grade = grade;
        this.consoleOutput = consoleOutput;
        this.timestamp = timestamp;
        this.admin = admin;
        this.semesterId = semesterId;
        this.submittedFiles = submittedFiles;
    }

    // --- Getters & Setters ---

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

    public String getSemesterId() {
        return semesterId;
    }

    public void setSemesterId(String semesterId) {
        this.semesterId = semesterId;
    }

    public Map<String, String> getSubmittedFiles() {
        return submittedFiles;
    }

    public void setSubmittedFiles(Map<String, String> submittedFiles) {
        this.submittedFiles = submittedFiles;
    }
}
