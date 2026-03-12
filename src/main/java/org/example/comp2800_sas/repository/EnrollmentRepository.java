package org.example.comp2800_sas.repository;

import org.example.comp2800_sas.model.Enrollment;
import org.example.comp2800_sas.model.EnrollmentId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, EnrollmentId> {
    List<Enrollment> findByIdStudentId(Integer studentId);
    List<Enrollment> findByIdSectionId(Integer sectionId);
    List<Enrollment> findByIdStudentIdAndStatus(Integer studentId, String status);
}