package org.example.comp2800_sas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Entity
@Table(name = "SECTION")
public class Section {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "section_id")
    private Integer sectionId;

    @Setter
    @Column(name = "course_id", nullable = false)
    private Integer courseId;

    @Setter
    @Column(name = "term_id", nullable = false)
    private Integer termId;

    @Setter
    @Column(name = "instructor_id", nullable = false)
    private Integer instructorId;

    @Setter
    @Column(name = "section_number", nullable = false)
    private Integer sectionNumber;

    @Setter
    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity;

    @Setter
    @Column(name = "num_enrolled")
    private Integer numEnrolled;

    public Section() {
    }

    public Section(Integer sectionId, Integer courseId, Integer termId, Integer instructorId, Integer sectionNumber, Integer maxCapacity, Integer numEnrolled) {
        this.sectionId = sectionId;
        this.courseId = courseId;
        this.termId = termId;
        this.instructorId = instructorId;
        this.sectionNumber = sectionNumber;
        this.maxCapacity = maxCapacity;
        this.numEnrolled = numEnrolled;
    }
}
