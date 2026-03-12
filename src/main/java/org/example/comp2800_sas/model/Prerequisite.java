package org.example.comp2800_sas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "PREREQUISITE")
public class Prerequisite {
    @EmbeddedId
    private PrerequisiteId id;

    public Prerequisite() {
    }

    public Prerequisite(PrerequisiteId id) {
        this.id = id;
    }
}
