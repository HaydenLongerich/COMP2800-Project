package org.example.comp2800_sas.model;

import jakarta.persistence.*;
import lombok.Setter;
import lombok.Getter;

@Getter
@Entity
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "student_id")
    private Integer studentId;

    @Setter
    @Column(name = "name", nullable = false)
    private String name;

    public Student() {
    }

    public Student(Integer studentId, String name) {
        this.studentId = studentId;
        this.name = name;
    }
}
