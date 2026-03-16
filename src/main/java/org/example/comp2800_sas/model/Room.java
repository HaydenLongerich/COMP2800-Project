package org.example.comp2800_sas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "ROOM",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"building", "room_number"})
        }
)
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Integer roomId;

    @Column(name = "building", nullable = false, length = 200)
    private String building;

    @Column(name = "room_number", nullable = false, length = 20)
    private String roomNumber;

    @Column(name = "num_seats", nullable = false)
    private Integer numSeats;

    public Room() {
    }

    public Room(String building, String roomNumber, Integer numSeats) {
        this.building = building;
        this.roomNumber = roomNumber;
        this.numSeats = numSeats;
    }
}