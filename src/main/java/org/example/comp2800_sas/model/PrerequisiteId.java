package org.example.comp2800_sas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Setter
@Getter
@Embeddable
public class PrerequisiteId implements Serializable {

    @Column(name = "course_id")
    private Integer courseId;

    @Column(name = "prerequisite_id")
    private Integer prerequisiteId;

    public PrerequisiteId() {
    }

    public PrerequisiteId(Integer courseId, Integer prerequisiteId) {
        this.courseId = courseId;
        this.prerequisiteId = prerequisiteId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PrerequisiteId that)) return false;
        return Objects.equals(courseId, that.courseId)
                && Objects.equals(prerequisiteId, that.prerequisiteId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(courseId, prerequisiteId);
    }
}