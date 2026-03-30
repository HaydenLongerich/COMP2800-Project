package org.example.comp2800_sas.repository;

import org.example.comp2800_sas.model.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * JPA repository for section records.
 */
public interface SectionRepository extends JpaRepository<Section, Integer> {
    List<Section> findByCourse_CourseId(Integer courseId);
    List<Section> findBySemester_SemesterId(Integer semesterId);
    List<Section> findByInstructor_InstructorId(Integer instructorId);
    boolean existsByCourse_CourseIdAndSemester_SemesterIdAndSectionNumber(Integer courseId, Integer semesterId, Integer sectionNumber);

    // Used by admin sync: look up all sections by course code string (e.g. "COMP1000")
    List<Section> findByCourse_Code(String code);
}
