package org.example.comp2800_sas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "TIMESLOT")
public class Timeslot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "timeslot_id")
    private Integer timeslotId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @ManyToOne(optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Enumerated(EnumType.STRING)
    @Column(name = "day", nullable = false, length = 20)
    private DayOfWeekType day;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    public Timeslot() {
    }

    public Timeslot(Section section, Room room, DayOfWeekType day,
                    LocalTime startTime, LocalTime endTime) {
        this.section = section;
        this.room = room;
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}