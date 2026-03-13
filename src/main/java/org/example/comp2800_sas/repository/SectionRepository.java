package org.example.comp2800_sas.repository;

import org.example.comp2800_sas.model.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectionRepository extends JpaRepository<Section, Integer> {
    List<Section> findByCourse_CourseId(Integer courseCourseId);
    List<Section> findByTerm_TermId(Integer termId);
    List<Section> findByCourse_CourseIdAndTerm_TermId(Integer courseId, Integer termId);
    List<Section> findByCourse_CourseIdAndTerm_TermIdOrderBySectionNumberAsc(Integer courseId, Integer termId);
}