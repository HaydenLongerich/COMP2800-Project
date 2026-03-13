package org.example.comp2800_sas.repository;

import org.example.comp2800_sas.model.Timeslot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimeslotRepository extends JpaRepository<Timeslot, Integer> {
    List<Timeslot> findBySection_SectionId(Integer sectionId);
    List<Timeslot> findByRoom_RoomId(Integer roomId);
}