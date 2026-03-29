package org.example.comp2800_sas.repository;

import org.example.comp2800_sas.model.PlannerSelection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface PlannerSelectionRepository extends JpaRepository<PlannerSelection, Long> {
    List<PlannerSelection> findByStudent_StudentIdOrderBySessionNameAscCourseCodeAsc(Integer studentId);
    List<PlannerSelection> findByStudent_StudentIdAndSessionNameOrderByCourseCodeAsc(Integer studentId, String sessionName);
    Optional<PlannerSelection> findByStudent_StudentIdAndSessionNameAndCourseCode(Integer studentId, String sessionName, String courseCode);

    @Modifying
    @Transactional
    void deleteByStudent_StudentId(Integer studentId);

    boolean existsByStudent_StudentIdAndSessionNameAndCourseCodeAndOptionNumber(
            Integer studentId,
            String sessionName,
            String courseCode,
            String optionNumber
    );
    int countByStudent_StudentId(Integer studentId);

    @Modifying
    @Transactional
    void deleteByStudent_StudentIdAndSessionName(Integer studentId, String sessionName);
}