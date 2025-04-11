package com.nour.ali.java_learning_backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "enrollments")
public class Enrollment {

    @EmbeddedId
    private EnrollmentId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Student student;

    @Column(nullable = false)
    private String admin;

    public Enrollment() {}

    public Enrollment(EnrollmentId id, Student student, String admin) {
        this.id = id;
        this.student = student;
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
        return id.getCourse();
    }

    public String getSemesterId() {
        return id.getSemesterId();
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
                ", admin='" + admin + '\'' +
                '}';
    }
}
