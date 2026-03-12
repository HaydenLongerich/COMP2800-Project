package org.example.comp2800_sas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
public class Enrollment {
    @EmbeddedId
    private EnrollmentId id;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "grade", precision = 5, scale = 2)
    private Double grade = null;

    public Enrollment() {
    }

    public Enrollment(EnrollmentId id, String status, Double grade) {
        this.id = id;
        this.status = status;
        this.grade = grade;
    }
}
