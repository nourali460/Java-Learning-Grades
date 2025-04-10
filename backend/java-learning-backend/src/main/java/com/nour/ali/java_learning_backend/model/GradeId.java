package com.nour.ali.java_learning_backend.model;

import java.io.Serializable;
import java.util.Objects;

public class GradeId implements Serializable {

    private String studentId;
    private String course;
    private String assignment;
    private String semesterId; // ✅ NEW

    public GradeId() {
    }

    public GradeId(String studentId, String course, String assignment, String semesterId) {
        this.studentId = studentId;
        this.course = course;
        this.assignment = assignment;
        this.semesterId = semesterId;
    }

    // ✅ equals and hashCode must use ALL FOUR FIELDS

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GradeId)) return false;
        GradeId that = (GradeId) o;
        return Objects.equals(studentId, that.studentId) &&
                Objects.equals(course, that.course) &&
                Objects.equals(assignment, that.assignment) &&
                Objects.equals(semesterId, that.semesterId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentId, course, assignment, semesterId);
    }

    // Getters and setters (if needed)
}
