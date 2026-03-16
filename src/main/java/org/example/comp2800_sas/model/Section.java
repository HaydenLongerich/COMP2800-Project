package org.example.comp2800_sas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

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
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    @ManyToOne(optional = false)
    @JoinColumn(name = "instructor_id", nullable = false)
    private Instructor instructor;

    @Column(name = "section_number", nullable = false)
    private Integer sectionNumber;

    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity;

    @OneToMany(mappedBy = "section")
    private List<Timeslot> timeslots = new ArrayList<>();

    @OneToMany(mappedBy = "section")
    private List<Enrollment> enrollments = new ArrayList<>();

    public Section() {
    }

    public Section(Course course, Semester semester, Instructor instructor,
                   Integer sectionNumber, Integer maxCapacity) {
        this.course = course;
        this.semester = semester;
        this.instructor = instructor;
        this.sectionNumber = sectionNumber;
        this.maxCapacity = maxCapacity;
    }
}