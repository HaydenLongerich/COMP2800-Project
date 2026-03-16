package org.example.comp2800_sas.service;

import org.example.comp2800_sas.model.Course;
import org.example.comp2800_sas.model.Instructor;
import org.example.comp2800_sas.model.Section;
import org.example.comp2800_sas.model.Semester;
import org.example.comp2800_sas.repository.CourseRepository;
import org.example.comp2800_sas.repository.InstructorRepository;
import org.example.comp2800_sas.repository.SectionRepository;
import org.example.comp2800_sas.repository.SemesterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SectionService {
    private final SectionRepository sectionRepository;
    private final CourseRepository courseRepository;
    private final SemesterRepository semesterRepository;
    private final InstructorRepository instructorRepository;

    public SectionService(
            SectionRepository sectionRepository,
            CourseRepository courseRepository,
            SemesterRepository semesterRepository,
            InstructorRepository instructorRepository
    ) {
        this.sectionRepository = sectionRepository;
        this.courseRepository = courseRepository;
        this.semesterRepository = semesterRepository;
        this.instructorRepository = instructorRepository;
    }

    /**
     * Creates and saves a new section after verifying its inputs.
     * @param courseId The database ID of the course that the section is under.
     * @param semesterId The database ID of the semester that the section is in.
     * @param instructorId The database ID of the instructor teaching the section.
     * @param sectionNumber The section number.
     * @param maxCapacity The maximum number of students allowed in the section.
     * @return The saved section.
     */
    public Section createSection(Integer courseId, Integer semesterId, Integer instructorId, Integer sectionNumber, Integer maxCapacity) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Course with ID " + courseId + " was not found.")
                );

        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Semester with ID " + semesterId + " was not found.")
                );

        Instructor instructor = instructorRepository.findById(instructorId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Instructor with ID " + instructorId + " was not found.")
                );

        if (sectionNumber <= 0) {
            throw new IllegalArgumentException("Section number must be greater than 0.");
        }

        if (maxCapacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than 0.");
        }

        if (sectionRepository.existsByCourse_CourseIdAndSemester_SemesterIdAndSectionNumber(
                courseId, semesterId, sectionNumber)) {
            throw new IllegalStateException("Section already exists for this course, semester, and section number.");
        }

        Section section = new Section();
        section.setCourse(course);
        section.setSemester(semester);
        section.setInstructor(instructor);
        section.setSectionNumber(sectionNumber);
        section.setMaxCapacity(maxCapacity);

        return sectionRepository.save(section);
    }

    /**
     *
     * @param sectionId The database ID of the section to find.
     * @return The section entity.
     */
    @Transactional(readOnly = true)
    public Section getSectionById(Integer sectionId) {
        return sectionRepository.findById(sectionId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Section with ID " + sectionId + " not found.")
                );
    }

    /**
     *
     * @param courseId The database ID of the course to search.
     * @return A list of all sections in a course.
     */
    @Transactional(readOnly = true)
    public List<Section> getSectionsForCourse(Integer courseId) {
        return sectionRepository.findByCourse_CourseId(courseId);
    }

    /**
     *
     * @param semesterId The database ID of the semester to search.
     * @return A list of all sections in a semester.
     */
    @Transactional(readOnly = true)
    public List<Section> getSectionsForSemester(Integer semesterId) {
        return sectionRepository.findBySemester_SemesterId(semesterId);
    }

    /**
     *
     * @param instructorId The database ID of the instructor
     * @return A list of sections taught by an instructor.
     */
    @Transactional(readOnly = true)
    public List<Section> getSectionsForInstructor(Integer instructorId) {
        return sectionRepository.findByInstructor_InstructorId(instructorId);
    }

    /**
     * Updates and saves the capacity of a section.
     * @param sectionId The database ID of the section to update
     * @param newCapacity The new capacity to set
     * @return The updated section entity.
     */
    public Section updateCapacity(Integer sectionId, int newCapacity) {
        Section section = getSectionById(sectionId);

        if (newCapacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than 0.");
        }

        section.setMaxCapacity(newCapacity);

        return sectionRepository.save(section);
    }

    /**
     * Updates and saves the section number of a section.
     * @param sectionId The database ID of the section
     * @param newSectionNumber The new section number to set to
     * @return The updated section entity.
     */
    public Section updateSectionNumber(Integer sectionId, Integer newSectionNumber) {
        Section section = getSectionById(sectionId);

        if (newSectionNumber <= 0) {
            throw new IllegalArgumentException("Section number must be greater than 0.");
        }

        if (sectionRepository.existsByCourse_CourseIdAndSemester_SemesterIdAndSectionNumber(
                section.getCourse().getCourseId(),
                section.getSemester().getSemesterId(),
                newSectionNumber)) {
            throw new IllegalStateException("Section already exists for this course, semester, and section number.");
        }

        section.setSectionNumber(newSectionNumber);

        return sectionRepository.save(section);
    }

    /**
     * Removes a section from the database.
     * @param sectionId The database ID of the section to remove.
     */
    public void deleteSection(Integer sectionId) {
        if (!sectionRepository.existsById(sectionId)) {
            throw new IllegalArgumentException("Section with ID " + sectionId + " was not found.");
        }

        sectionRepository.deleteById(sectionId);
    }

    /**
     * Assigns an instructor to a section and saves to database.
     * @param sectionId The database ID of the section to update.
     * @param instructorId The database ID of the instructor.
     * @return The updated section entity.
     */
    public Section assignInstructor(Integer sectionId, Integer instructorId) {
        Section section = getSectionById(sectionId);
        Instructor instructor = instructorRepository.findById(instructorId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Instructor with ID " + instructorId + " not found.")
                );

        section.setInstructor(instructor);

        return sectionRepository.save(section);
    }
}