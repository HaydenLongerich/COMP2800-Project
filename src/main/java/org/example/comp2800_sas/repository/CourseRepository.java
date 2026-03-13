package org.example.comp2800_sas.repository;

import org.example.comp2800_sas.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Integer> {
    Optional<Course> findByCode(String code);
    List<Course> findByTitleContainingIgnoreCase(String title);
    boolean existsByCode(String code);
}