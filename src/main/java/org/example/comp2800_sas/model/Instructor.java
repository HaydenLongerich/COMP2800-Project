package org.example.comp2800_sas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Entity
@Table(name = "INSTRUCTOR")
public class Instructor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "instructor_id")
    private Integer instructorId;

    @Setter
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Setter
    @Column(name = "research_topics", nullable = true)
    private String researchTopics;

    public Instructor() {
    }

    public Instructor(Integer instructorId, String name, String researchTopics) {
        this.instructorId = instructorId;
        this.name = name;
        this.researchTopics = researchTopics;
    }
}
