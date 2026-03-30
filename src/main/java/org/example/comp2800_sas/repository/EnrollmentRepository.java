package org.example.comp2800_sas.repository;

import org.example.comp2800_sas.model.Enrollment;
import org.example.comp2800_sas.model.EnrollmentId;
import org.example.comp2800_sas.model.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for enrollment records.
 */
public interface EnrollmentRepository extends JpaRepository<Enrollment, EnrollmentId> {
    Optional<Enrollment> findByStudent_StudentIdAndSection_SectionId(Integer studentId, Integer sectionId);
    List<Enrollment> findByStudent_StudentId(Integer studentId);
    List<Enrollment> findByStudent_StudentIdAndSection_Semester_SemesterId(Integer studentId, Integer semesterId);
    List<Enrollment> findBySection_SectionId(Integer sectionId);
    boolean existsByStudent_StudentIdAndSection_SectionId(Integer studentId, Integer sectionId);
    int countBySection_SectionIdAndStatus(Integer sectionId, EnrollmentStatus status);
    boolean existsByStudent_StudentIdAndSection_SectionIdAndStatus(Integer studentId, Integer sectionId, EnrollmentStatus status);
    boolean existsByStudent_StudentIdAndSection_Course_CourseIdAndStatus(Integer studentId, Integer prerequisiteCourseId, EnrollmentStatus enrollmentStatus);

    @Modifying
    @Transactional
    @Query("DELETE FROM Enrollment e WHERE e.student.studentId = :studentId")
    void deleteByStudentId(Integer studentId);
}
