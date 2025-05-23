package com.nour.ali.java_learning_backend.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Entity
@IdClass(GradeId.class)
@Table(name = "grades")
public class Grade {

    @Id
    private String studentId;

    @Id
    private String course;

    @Id
    private String assignment;

    @Id
    private String semesterId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String grade;

    @Column(columnDefinition = "TEXT")
    private String consoleOutput;

    private Instant timestamp;

    private String admin;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "submitted_files",
            joinColumns = {
                    @JoinColumn(name = "student_id", referencedColumnName = "studentId"),
                    @JoinColumn(name = "course", referencedColumnName = "course"),
                    @JoinColumn(name = "assignment", referencedColumnName = "assignment"),
                    @JoinColumn(name = "semester_id", referencedColumnName = "semesterId")
            }
    )
    @MapKeyColumn(name = "filename")
    @Column(name = "file_content", columnDefinition = "TEXT")
    private Map<String, String> submittedFiles = new HashMap<>();

    public Grade() {}

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

    @Override
    public String toString() {
        return "Grade{" +
                "studentId='" + studentId + '\'' +
                ", course='" + course + '\'' +
                ", assignment='" + assignment + '\'' +
                ", grade='" + grade + '\'' +
                ", timestamp=" + timestamp +
                ", admin='" + admin + '\'' +
                ", semesterId='" + semesterId + '\'' +
                ", submittedFiles=" + submittedFiles.keySet() +
                '}';
    }
}
