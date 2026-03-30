package org.example.comp2800_sas.repository;

import org.example.comp2800_sas.model.Prerequisite;
import org.example.comp2800_sas.model.PrerequisiteId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * JPA repository for prerequisite relationships.
 */
public interface PrerequisiteRepository extends JpaRepository<Prerequisite, PrerequisiteId> {
    List<Prerequisite> findByIdCourseId(Integer courseId);
    List<Prerequisite> findByIdPrerequisiteId(Integer prerequisiteId);
    List<Prerequisite> findByCourse_CourseId(Integer courseCourseId);
}
