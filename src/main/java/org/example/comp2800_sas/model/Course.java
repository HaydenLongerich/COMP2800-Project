package org.example.comp2800_sas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "COURSE")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private Integer courseId;

    @Column(name = "code", nullable = false, unique = true, length = 10)
    private String code;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description")
    private String description;

    @OneToMany(mappedBy = "course")
    private List<Section> sections = new ArrayList<>();

    public Course() {
    }

    public Course(String code, String title, String description) {
        this.code = code;
        this.title = title;
        this.description = description;
    }
}