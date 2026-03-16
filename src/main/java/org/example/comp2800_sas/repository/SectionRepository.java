package org.example.comp2800_sas.repository;

import org.example.comp2800_sas.model.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SectionRepository extends JpaRepository<Section, Integer> {
    List<Section> findByCourse_CourseId(Integer courseId);
    List<Section> findBySemester_SemesterId(Integer semesterId);
    List<Section> findByInstructor_InstructorId(Integer instructorId);
    boolean existsByCourse_CourseIdAndSemester_SemesterIdAndSectionNumber(Integer courseId, Integer semesterId, Integer sectionNumber);
}