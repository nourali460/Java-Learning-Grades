package com.nour.ali.java_learning_backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "enrollments")
public class Enrollment {

    @EmbeddedId
    private EnrollmentId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("studentId")
    @JoinColumn(name = "student_id")
    private Student student;

    @Column(nullable = false)
    private String course;

    @Column(nullable = false)
    private String semesterId;

    @Column(nullable = false)
    private String admin; // professor who added this enrollment

    public Enrollment() {}

    public Enrollment(EnrollmentId id, Student student, String course, String semesterId, String admin) {
        this.id = id;
        this.student = student;
        this.course = course;
        this.semesterId = semesterId;
        this.admin = admin;
    }

    public EnrollmentId getId() {
        return id;
    }

    public void setId(EnrollmentId id) {
        this.id = id;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
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

    @Override
    public String toString() {
        return "Enrollment{" +
                "id=" + id +
                ", course='" + course + '\'' +
                ", semesterId='" + semesterId + '\'' +
                ", admin='" + admin + '\'' +
                '}';
    }
}
