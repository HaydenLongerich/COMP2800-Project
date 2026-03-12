package org.example.comp2800_sas.repository;

import org.example.comp2800_sas.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Integer> {
    Optional<Room> findByBuildingAndRoomNumber(String building, String roomNumber);
}