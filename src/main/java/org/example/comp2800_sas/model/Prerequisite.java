package org.example.comp2800_sas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

// Persistent prerequisite relationship between two courses.
@Getter
@Setter
@Entity
@Table(name = "PREREQUISITE")
public class Prerequisite {

    @EmbeddedId
    private PrerequisiteId id = new PrerequisiteId();

    @ManyToOne(optional = false)
    @MapsId("courseId")
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(optional = false)
    @MapsId("prerequisiteId")
    @JoinColumn(name = "prerequisite_id", nullable = false)
    private Course prerequisiteCourse;

    public Prerequisite() {
    }

    public Prerequisite(Course course, Course prerequisiteCourse) {
        this.course = course;
        this.prerequisiteCourse = prerequisiteCourse;
        this.id = new PrerequisiteId(course.getCourseId(), prerequisiteCourse.getCourseId());
    }
}
