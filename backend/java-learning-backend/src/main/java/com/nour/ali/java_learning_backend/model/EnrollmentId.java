package com.nour.ali.java_learning_backend.model;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class EnrollmentId implements Serializable {

    private String studentId;
    private String course;
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

    public void setStudentId(String studentId) {
        this.studentId = studentId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EnrollmentId that)) return false;
        return Objects.equals(studentId, that.studentId) &&
                Objects.equals(course, that.course) &&
                Objects.equals(semesterId, that.semesterId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentId, course, semesterId);
    }

    @Override
    public String toString() {
        return "EnrollmentId{" +
                "studentId='" + studentId + '\'' +
                ", course='" + course + '\'' +
                ", semesterId='" + semesterId + '\'' +
                '}';
    }
}
