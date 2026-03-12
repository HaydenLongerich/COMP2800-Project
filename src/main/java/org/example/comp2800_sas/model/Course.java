package org.example.comp2800_sas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Entity
@Table(name = "COURSE")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private Integer courseId;

    @Setter
    @Column(name = "code", nullable = false, unique = true, length = 10)
    private String code;

    @Setter
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Setter
    @Column(name = "description")
    private String description;

    public Course() {
    }

    public Course(String code, String title, String description) {
        this.code = code;
        this.title = title;
        this.description = description;
    }
}