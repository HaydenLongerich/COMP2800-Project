package org.example.comp2800_sas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Entity
@Table(name = "SEMESTER")
public class Semester {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "term_id")
    private Integer termId;

    @Setter
    @Column(name = "year", nullable = false)
    private int year;

    @Setter
    @Column(name = "season", nullable = false, length = 200)
    private String season;

    public Semester() {
    }

    public Semester(Integer termId, int year, String season) {
        this.termId = termId;
        this.year = year;
        this.season = season;
    }
}
