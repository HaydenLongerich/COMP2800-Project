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

// Service layer for section lookup, scheduling, and assignment flows.
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

    // Creates and saves a new section after verifying its inputs.
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

    @Transactional(readOnly = true)
    public Section getSectionById(Integer sectionId) {
        return sectionRepository.findById(sectionId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Section with ID " + sectionId + " not found.")
                );
    }

    @Transactional(readOnly = true)
    public List<Section> getSectionsForCourse(Integer courseId) {
        return sectionRepository.findByCourse_CourseId(courseId);
    }

    @Transactional(readOnly = true)
    public List<Section> getSectionsForSemester(Integer semesterId) {
        return sectionRepository.findBySemester_SemesterId(semesterId);
    }

    @Transactional(readOnly = true)
    public List<Section> getSectionsForInstructor(Integer instructorId) {
        return sectionRepository.findByInstructor_InstructorId(instructorId);
    }

    // Updates and saves the capacity of a section.
    public Section updateCapacity(Integer sectionId, int newCapacity) {
        Section section = getSectionById(sectionId);

        if (newCapacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than 0.");
        }

        section.setMaxCapacity(newCapacity);

        return sectionRepository.save(section);
    }

    // Updates and saves the section number of a section.
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

    // Removes a section from the database.
    public void deleteSection(Integer sectionId) {
        if (!sectionRepository.existsById(sectionId)) {
            throw new IllegalArgumentException("Section with ID " + sectionId + " was not found.");
        }

        sectionRepository.deleteById(sectionId);
    }

    // Assigns an instructor to a section and saves to database.
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
