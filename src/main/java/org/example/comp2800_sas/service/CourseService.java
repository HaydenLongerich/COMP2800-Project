package org.example.comp2800_sas.service;

import org.example.comp2800_sas.model.Course;
import org.example.comp2800_sas.repository.CourseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service layer for creating, updating, and listing courses.
 */
@Service
@Transactional
public class CourseService {

    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    /**
     * Creates a course entry and saves it to the database.
     * @param code The course code.
     * @param title The course title.
     * @param description Optionally, the course description.
     * @return The new course entry.
     */
    // ADMIN ONLY
    public Course createCourse(String code, String title, String description) {
        String normalizedCode = normalizeCode(code);
        String normalizedTitle = normalizeText(title);
        String normalizedDescription = normalizeNullableText(description);

        validateCourseData(normalizedCode, normalizedTitle);

        if (courseRepository.existsByCode(normalizedCode)) {
            throw new IllegalArgumentException(
                    "A course with code '" + normalizedCode + "' already exists."
            );
        }

        Course course = new Course(normalizedCode, normalizedTitle, normalizedDescription);
        return courseRepository.save(course);
    }

    /**
     *
     * @return All courses in the database.
     */
    @Transactional(readOnly = true)
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    /**
     * Finds a course by its database ID.
     * @param courseId The database ID of the course to find.
     * @return The course entity found.
     */
    @Transactional(readOnly = true)
    public Course getCourseById(Integer courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Course with ID " + courseId + " was not found.")
                );
    }

    /**
     * Finds a course by its course code (e.g. COMP2800).
     * @param code The course code of the course.
     * @return The course entity found.
     */
    @Transactional(readOnly = true)
    public Course getCourseByCode(String code) {
        String normalizedCode = normalizeCode(code);

        if (normalizedCode == null || normalizedCode.isBlank()) {
            throw new IllegalArgumentException("Course code cannot be blank.");
        }

        return courseRepository.findByCode(normalizedCode)
                .orElseThrow(() ->
                        new IllegalArgumentException("Course with code '" + normalizedCode + "' was not found.")
                );
    }

    /**
     * Updates the fields of a course entry and saves to the database.
     * @param courseId The database ID of the course to update.
     * @param newTitle The new title of the course.
     * @param newDescription The new description of the course.
     * @return The update course entity.
     */
    // ADMIN ONLY
    public Course updateCourse(Integer courseId, String newTitle, String newDescription) {
        Course course = getCourseById(courseId);

        String normalizedTitle = normalizeText(newTitle);
        String normalizedDescription = normalizeNullableText(newDescription);

        if (normalizedTitle == null || normalizedTitle.isBlank()) {
            throw new IllegalArgumentException("Course title cannot be blank.");
        }

        course.setTitle(normalizedTitle);
        course.setDescription(normalizedDescription);

        return courseRepository.save(course);
    }

    /**
     * Removes a course from the database.
     * @param courseId The database ID of the course to delete.
     */
    // ADMIN ONLY
    public void deleteCourse(Integer courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new IllegalArgumentException("Course with ID " + courseId + " was not found.");
        }

        courseRepository.deleteById(courseId);
    }

    /**
     * Validates course information for creating courses.
     * @param code The course code.
     * @param title The course title.
     */
    private void validateCourseData(String code, String title) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Course code cannot be blank.");
        }

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Course title cannot be blank.");
        }
    }

    /**
     * Trims and uppercases course codes (" comp2800 " -> "COMP2800")
     * @param code The code to be normalized.
     * @return The normalized code.
     */
    private String normalizeCode(String code) {
        if (code == null) {
            return null;
        }
        return code.trim().toUpperCase();
    }

    /**
     * Trims text.
     * @param value Text to be normalized.
     * @return Normalized text, or null if the input is null.
     */
    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        return value.trim();
    }

    /**
     * Trims nullable text.
     * @param value Text to be trimmed.
     * @return Normalized text, or null. No empty strings.
     */
    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
