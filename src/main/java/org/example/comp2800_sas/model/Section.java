package org.example.comp2800_sas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "SECTION")
public class Section {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "section_id")
    private Integer sectionId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(optional = false)
    @JoinColumn(name = "term_id", nullable = false)
    private Semester term;

    @ManyToOne(optional = false)
    @JoinColumn(name = "instructor_id", nullable = false)
    private Instructor instructor;

    @Column(name = "section_number", nullable = false)
    private Integer sectionNumber;

    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity;

    @Column(name = "num_enrolled", nullable = false)
    private Integer numEnrolled;

    public Section() {
    }

    public Section(Course course, Semester term, Instructor instructor,
                   Integer sectionNumber, Integer maxCapacity, Integer numEnrolled) {
        this.course = course;
        this.term = term;
        this.instructor = instructor;
        this.sectionNumber = sectionNumber;
        this.maxCapacity = maxCapacity;
        this.numEnrolled = numEnrolled;
    }
}