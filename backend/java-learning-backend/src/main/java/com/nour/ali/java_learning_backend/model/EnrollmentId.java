package com.nour.ali.java_learning_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class EnrollmentId implements Serializable {

    @Column(name = "student_id")
    private String studentId;

    @Column(name = "course")
    private String course;

    @Column(name = "semester_id")
    private String semesterId;

    public EnrollmentId() {}

    public EnrollmentId(String studentId, String course, String semesterId) {
        this.studentId = studentId;
        this.course = course;
        this.semesterId = semesterId;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getCourse() {
        return course;
    }

    public String getSemesterId() {
        return semesterId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EnrollmentId)) return false;
        EnrollmentId that = (EnrollmentId) o;
        return Objects.equals(studentId, that.studentId) &&
                Objects.equals(course, that.course) &&
                Objects.equals(semesterId, that.semesterId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentId, course, semesterId);
    }
}
