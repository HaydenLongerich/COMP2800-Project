package org.example.comp2800_sas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Setter
@Getter
@Embeddable
public class EnrollmentId implements Serializable {

    @Column(name = "student_id")
    private Integer studentId;

    @Column(name = "section_id")
    private Integer sectionId;

    public EnrollmentId() {
    }

    public EnrollmentId(Integer studentId, Integer sectionId) {
        this.studentId = studentId;
        this.sectionId = sectionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EnrollmentId that)) return false;
        return Objects.equals(studentId, that.studentId)
                && Objects.equals(sectionId, that.sectionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentId, sectionId);
    }
}