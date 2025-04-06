package com.nour.ali.java_learning_backend.model;

import java.io.Serializable;
import java.util.Objects;

@SuppressWarnings("serial")
public class GradeId implements Serializable {

    private String studentId;
    private String course;
    private String assignment;

    public GradeId() {
    }

    public GradeId(String studentId, String course, String assignment) {
        this.studentId = studentId;
        this.course = course;
        this.assignment = assignment;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getCourse() {
        return course;
    }

    public String getAssignment() {
        return assignment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GradeId other)) return false;
        return Objects.equals(studentId, other.studentId)
                && Objects.equals(course, other.course)
                && Objects.equals(assignment, other.assignment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentId, course, assignment);
    }
}
