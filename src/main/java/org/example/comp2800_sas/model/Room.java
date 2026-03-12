package org.example.comp2800_sas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Entity
public class Room {
    @Setter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Integer roomId;

    @Setter
    @Column(name = "building", nullable = false, length = 100)
    private String building;

    @Setter
    @Column(name = "room_number", nullable = false, length = 20)
    private String roomNumber;

    @Setter
    @Column(name = "num_seats", nullable = false)
    private Integer numSeats;

    public Room() {
    }

    public Room(Integer roomId, String building, String roomNumber, Integer numSeats) {
        this.roomId = roomId;
        this.building = building;
        this.roomNumber = roomNumber;
        this.numSeats = numSeats;
    }
}
