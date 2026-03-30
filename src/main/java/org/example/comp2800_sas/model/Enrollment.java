package org.example.comp2800_sas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Persistent enrollment record tying a student to a section.
 */
@Getter
@Setter
@Entity
@Table(name = "ENROLLMENT")
public class Enrollment {

    @EmbeddedId
    private EnrollmentId id = new EnrollmentId();

    @ManyToOne(optional = false)
    @MapsId("studentId")
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(optional = false)
    @MapsId("sectionId")
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EnrollmentStatus status;

    @Column(name = "grade", precision = 5, scale = 2)
    private BigDecimal grade;

    public Enrollment() {
    }

    public Enrollment(Student student, Section section, EnrollmentStatus status, BigDecimal grade) {
        this.student = student;
        this.section = section;
        this.status = status;
        this.grade = grade;
        this.id = new EnrollmentId(student.getStudentId(), section.getSectionId());
    }
}
