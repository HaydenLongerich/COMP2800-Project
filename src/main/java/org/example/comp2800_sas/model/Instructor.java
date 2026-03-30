package org.example.comp2800_sas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

// Persistent instructor record.
@Getter
@Setter
@Entity
@Table(name = "INSTRUCTOR")
public class Instructor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "instructor_id")
    private Integer instructorId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "research_topics")
    private String researchTopics;

    @OneToMany(mappedBy = "instructor")
    private List<Section> sections = new ArrayList<>();

    public Instructor() {
    }

    public Instructor(String name, String researchTopics) {
        this.name = name;
        this.researchTopics = researchTopics;
    }
}
