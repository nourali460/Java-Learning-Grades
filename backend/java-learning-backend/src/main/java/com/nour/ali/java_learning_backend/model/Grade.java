package com.nour.ali.java_learning_backend.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "grades")
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String studentId;
    private String course;
    private String assignment;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String grade;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String consoleOutput;

    private Instant timestamp;

    private String admin;

    public Grade() {
    }

    // --- Getters & Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    @Override
    public String toString() {
        return "Grade{" +
                "id=" + id +
                ", studentId='" + studentId + '\'' +
                ", course='" + course + '\'' +
                ", assignment='" + assignment + '\'' +
                ", grade='" + grade + '\'' +
                ", timestamp=" + timestamp +
                ", admin='" + admin + '\'' +
                '}';
    }
}
