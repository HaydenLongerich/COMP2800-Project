package org.example.comp2800_sas.model;

import jakarta.persistence.*;
import lombok.Setter;
import lombok.Getter;

import java.time.LocalTime;

@Getter
@Entity
public class Timeslot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "timeslot_id")
    private Integer timeslotId;

    @Setter
    @Column(name = "section_id", nullable = false)
    private Integer sectionId;

    @Setter
    @Column(name = "room_id", nullable = false)
    private Integer roomId;

    @Setter
    @Column(name = "day", nullable = false, length = 10)
    private String day;

    @Setter
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Setter
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    public Timeslot() {
    }

    public Timeslot(Integer timeslotId, Integer sectionId, Integer roomId, String day, LocalTime startTime, LocalTime endTime) {
        this.timeslotId = timeslotId;
        this.sectionId = sectionId;
        this.roomId = roomId;
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
