package org.example.comp2800_sas.repository;

import org.example.comp2800_sas.model.Instructor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InstructorRepository extends JpaRepository<Instructor, Integer> {
    List<Instructor> findByNameContainingIgnoreCase(String name);
}