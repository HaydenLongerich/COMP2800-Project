package org.example.comp2800_sas.repository;

import org.example.comp2800_sas.model.Season;
import org.example.comp2800_sas.model.Semester;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * JPA repository for semester records.
 */
public interface SemesterRepository extends JpaRepository<Semester, Integer> {
    Optional<Semester> findByYearAndSeason(Integer year, Season season);

    Optional<Semester> findBySemesterId(Integer semesterId);
}
