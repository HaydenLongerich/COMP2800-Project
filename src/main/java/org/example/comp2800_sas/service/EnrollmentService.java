package org.example.comp2800_sas.service;

import org.example.comp2800_sas.model.*;
import org.example.comp2800_sas.repository.EnrollmentRepository;
import org.example.comp2800_sas.repository.PrerequisiteRepository;
import org.example.comp2800_sas.repository.SectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

// Service layer for student enrollments and enrollment status updates.
@Service
@Transactional
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final SectionRepository sectionRepository;
    private final PrerequisiteRepository prerequisiteRepository;

    public EnrollmentService(
            EnrollmentRepository enrollmentRepository,
            SectionRepository sectionRepository,
            PrerequisiteRepository prerequisiteRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.sectionRepository = sectionRepository;
        this.prerequisiteRepository = prerequisiteRepository;
    }

    // ADMIN ONLY
    // Verifies enrollment constraints and enrolls a student in a section. Saves to database.
    public Enrollment enrollStudent(Integer studentId, Integer sectionId) {
        if (studentInSection(studentId, sectionId)) {
            throw new IllegalStateException("Student already enrolled in section");
        }

        if (sectionFull(sectionId)) {
            throw new IllegalStateException("Section is full");
        }

        Section section = getSectionById(sectionId);

        if (!meetsCoursePrerequisites(studentId, section.getCourse().getCourseId())) {
            throw new IllegalStateException("Student does not meet prerequisites");
        }

        Student student = new Student();
        student.setStudentId(studentId);

        Enrollment enrollment = new Enrollment(student, section, EnrollmentStatus.ENROLLED, null);

        return enrollmentRepository.save(enrollment);
    }

    // ADMIN ONLY
    // Changes the enrollment status of a student in a section to DROPPED. Saves to database.
    public void dropStudent(Integer studentId, Integer sectionId) {
        Enrollment enrollment = getEnrollment(studentId, sectionId);
        enrollment.setStatus(EnrollmentStatus.DROPPED);
        enrollmentRepository.save(enrollment);
    }

    // Finds the entry for a student in a section.
    @Transactional(readOnly = true)
    public Enrollment getEnrollment(Integer studentId, Integer sectionId) {
        return enrollmentRepository
                .findByStudent_StudentIdAndSection_SectionId(studentId, sectionId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Enrollment not found for student " + studentId + " in section " + sectionId + ".")
                );
    }

    // Finds all enrollments for a student across all semesters.
    @Transactional(readOnly = true)
    public List<Enrollment> getEnrollmentsForStudent(Integer studentId) {
        return enrollmentRepository
                .findByStudent_StudentId(studentId);
    }

    // Finds all enrollments for a student in a specific semester.
    @Transactional(readOnly = true)
    public List<Enrollment> getEnrollmentsForStudentInSemester(Integer studentId, Integer semesterId) {
        return enrollmentRepository
                .findByStudent_StudentIdAndSection_Semester_SemesterId(studentId, semesterId);
    }

    // Finds all enrollments in a section.
    @Transactional(readOnly = true)
    public List<Enrollment> getEnrollmentsForSection(Integer sectionId) {
        return enrollmentRepository
                .findBySection_SectionId(sectionId);
    }

    // ADMIN ONLY
    // Updates the EnrollmentStatus of a student in a section and saves it to the database.
    public void updateEnrollmentStatus(Integer studentId, Integer sectionId, EnrollmentStatus status) {
        Enrollment enrollment = getEnrollment(studentId, sectionId);
        enrollment.setStatus(status);
        enrollmentRepository.save(enrollment);
    }

    // Updates the grade of a student in a section and saves it to the database.
    public void updateGrade(Integer studentId, Integer sectionId, BigDecimal grade) {
        Enrollment enrollment = getEnrollment(studentId, sectionId);

        if (grade.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Grade must be greater than or equal to 0.");
        } else if (grade.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalStateException("Grade must be less than 100.");
        }

        enrollment.setGrade(grade);
        enrollmentRepository.save(enrollment);
    }

    @Transactional(readOnly = true)
    public boolean studentInSection(Integer studentId, Integer sectionId) {
        return enrollmentRepository
                .existsByStudent_StudentIdAndSection_SectionIdAndStatus(studentId, sectionId, EnrollmentStatus.ENROLLED);
    }

    // ---------------------------------------------------------------
    // ADMIN SYNC — NEW METHODS
    // ---------------------------------------------------------------

    // Returns true if ANY enrollment row exists for this student+section regardless of status.
    @Transactional(readOnly = true)
    public boolean hasEnrollmentRecord(Integer studentId, Integer sectionId) {
        return enrollmentRepository
                .findByStudent_StudentIdAndSection_SectionId(studentId, sectionId)
                .isPresent();
    }

    // Enrolls a student into a section WITHOUT checking prerequisites or capacity.
    public void adminEnrollStudent(Integer studentId, Integer sectionId) {
        if (hasEnrollmentRecord(studentId, sectionId)) {
            return; // already exists — skip to avoid duplicate key
        }

        Section section = getSectionById(sectionId);

        Student student = new Student();
        student.setStudentId(studentId);

        Enrollment enrollment = new Enrollment(student, section, EnrollmentStatus.ENROLLED, null);
        enrollmentRepository.save(enrollment);
    }

    // ---------------------------------------------------------------

    // Checks the students past courses against the course prerequisites to see if the student is eligible to take the class.
    private boolean meetsCoursePrerequisites(Integer studentId, Integer courseId) {
        List<Prerequisite> prerequisites = prerequisiteRepository.findByCourse_CourseId(courseId);

        for (Prerequisite prerequisite : prerequisites) {
            Integer prerequisiteCourseId = prerequisite.getPrerequisiteCourse().getCourseId();

            boolean passedPrerequisite =
                    enrollmentRepository.existsByStudent_StudentIdAndSection_Course_CourseIdAndStatus(
                            studentId,
                            prerequisiteCourseId,
                            EnrollmentStatus.PASSED
                    );

            if (!passedPrerequisite) {
                return false;
            }
        }

        return true;
    }

    @Transactional(readOnly = true)
    public boolean sectionFull(Integer sectionId) {
        Section section = getSectionById(sectionId);

        int enrolled = enrollmentRepository
                .countBySection_SectionIdAndStatus(sectionId, EnrollmentStatus.ENROLLED);

        return enrolled >= section.getMaxCapacity();
    }

    // Calculates the number of seats remaining in a section.
    @Transactional(readOnly = true)
    public int getSeatsLeftInSection(Integer sectionId) {
        Section section = getSectionById(sectionId);

        int enrolled = enrollmentRepository
                .countBySection_SectionIdAndStatus(sectionId, EnrollmentStatus.ENROLLED);

        return section.getMaxCapacity() - enrolled;
    }

    // Finds a section by its database ID.
    private Section getSectionById(Integer sectionId) {
        return sectionRepository.findById(sectionId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Section with ID " + sectionId + " not found.")
                );
    }
}
