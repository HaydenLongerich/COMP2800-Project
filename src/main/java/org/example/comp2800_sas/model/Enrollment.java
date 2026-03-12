package org.example.comp2800_sas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Setter
@Getter
@Table(name = "ENROLLMENT")
public class Enrollment {
    @EmbeddedId
    private EnrollmentId id;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "grade", precision = 5, scale = 2)
    private BigDecimal grade = null;

    public Enrollment() {
    }

    public Enrollment(EnrollmentId id, String status, BigDecimal grade) {
        this.id = id;
        this.status = status;
        this.grade = grade;
    }
}
