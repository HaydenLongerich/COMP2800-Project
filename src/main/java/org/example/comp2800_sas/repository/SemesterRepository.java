package org.example.comp2800_sas.repository;

import org.example.comp2800_sas.model.Semester;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SemesterRepository extends JpaRepository<Semester, Integer> {
    Optional<Semester> findByYearAndSeason(int year, String season);
}