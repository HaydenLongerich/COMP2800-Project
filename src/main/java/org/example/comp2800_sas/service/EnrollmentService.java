package org.example.comp2800_sas.service;

import org.example.comp2800_sas.model.*;
import org.example.comp2800_sas.repository.EnrollmentRepository;
import org.example.comp2800_sas.repository.PrerequisiteRepository;
import org.example.comp2800_sas.repository.SectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

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
    /**
     * Verifies enrollment constraints and enrolls a student in a section. Saves to database.
     * @param studentId The database ID of the student
     * @param sectionId The database ID of the section
     * @return A new Enrollment object with values filled if successful.
     */
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
    /**
     * Changes the enrollment status of a student in a section to DROPPED. Saves to database.
     * Does not delete the enrollment row.
     * @param studentId The student to drop
     * @param sectionId The section to drop from
     */
    public void dropStudent(Integer studentId, Integer sectionId) {
        Enrollment enrollment = getEnrollment(studentId, sectionId);
        enrollment.setStatus(EnrollmentStatus.DROPPED);
        enrollmentRepository.save(enrollment);
    }

    /**
     * Finds the entry for a student in a section.
     * @param studentId The database ID of the student in the section
     * @param sectionId The database ID of the section
     * @return An Enrollment object
     */
    @Transactional(readOnly = true)
    public Enrollment getEnrollment(Integer studentId, Integer sectionId) {
        return enrollmentRepository
                .findByStudent_StudentIdAndSection_SectionId(studentId, sectionId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Enrollment not found for student " + studentId + " in section " + sectionId + ".")
                );
    }

    /**
     * Finds all enrollments for a student across all semesters.
     * @param studentId The database ID of the student to search for.
     * @return A list of enrollments for the student.
     */
    @Transactional(readOnly = true)
    public List<Enrollment> getEnrollmentsForStudent(Integer studentId) {
        return enrollmentRepository
                .findByStudent_StudentId(studentId);
    }

    /**
     * Finds all enrollments for a student in a specific semester.
     * @param studentId The database ID of the student to search for.
     * @param semesterId The database ID of the semester to search in.
     * @return A list of enrollments for the student in the semester.
     */
    @Transactional(readOnly = true)
    public List<Enrollment> getEnrollmentsForStudentInSemester(Integer studentId, Integer semesterId) {
        return enrollmentRepository
                .findByStudent_StudentIdAndSection_Semester_SemesterId(studentId, semesterId);
    }

    /**
     * Finds all enrollments in a section.
     * @param sectionId The database ID of the section to search.
     * @return A list of enrollments for the section.
     */
    @Transactional(readOnly = true)
    public List<Enrollment> getEnrollmentsForSection(Integer sectionId) {
        return enrollmentRepository
                .findBySection_SectionId(sectionId);
    }

    // ADMIN ONLY
    /**
     * Updates the EnrollmentStatus of a student in a section and saves it to the database.
     * @param studentId The database ID of the student to update.
     * @param sectionId The database ID of the section to update.
     * @param status The EnrollmentStatus to update to.
     */
    public void updateEnrollmentStatus(Integer studentId, Integer sectionId, EnrollmentStatus status) {
        Enrollment enrollment = getEnrollment(studentId, sectionId);
        enrollment.setStatus(status);
        enrollmentRepository.save(enrollment);
    }

    /**
     * Updates the grade of a student in a section and saves it to the database.
     * @param studentId The database ID of the student to update.
     * @param sectionId The database ID of the section to update.
     * @param grade A BigDecimal representation of the grade to set.
     */
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

    /**
     *
     * @param studentId The student to search for.
     * @param sectionId The section to search in.
     * @return True, if the student is in the section, otherwise false.
     */
    @Transactional(readOnly = true)
    public boolean studentInSection(Integer studentId, Integer sectionId) {
        return enrollmentRepository
                .existsByStudent_StudentIdAndSection_SectionIdAndStatus(studentId, sectionId, EnrollmentStatus.ENROLLED);
    }

    /**
     * Checks the students past courses against the course prerequisites to see if the student is eligible to take the class.
     * @param studentId The database ID of the student whose records will be searched.
     * @param courseId The database ID of the course the student wants to take.
     * @return True, if the student meets the prerequisites for the course, otherwise false.
     */
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

    /**
     *
     * @param sectionId The database ID of the section to check.
     * @return True, if the section is full, otherwise false.
     */
    @Transactional(readOnly = true)
    public boolean sectionFull(Integer sectionId) {
        Section section = getSectionById(sectionId);

        int enrolled = enrollmentRepository
                .countBySection_SectionIdAndStatus(sectionId, EnrollmentStatus.ENROLLED);

        return enrolled >= section.getMaxCapacity();
    }

    /**
     * Calculates the number of seats remaining in a section.
     * @param sectionId The database ID of the section to check.
     * @return The number of seats remaining in a section.
     */
    @Transactional(readOnly = true)
    public int getSeatsLeftInSection(Integer sectionId) {
        Section section = getSectionById(sectionId);

        int enrolled = enrollmentRepository
                .countBySection_SectionIdAndStatus(sectionId, EnrollmentStatus.ENROLLED);

        return section.getMaxCapacity() - enrolled;
    }

    /**
     * Finds a section by its database ID.
     * @param sectionId The database ID of the section to search for.
     * @return A section object corresponding to the database ID.
     */
    private Section getSectionById(Integer sectionId) {
        return sectionRepository.findById(sectionId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Section with ID " + sectionId + " not found.")
                );
    }
}