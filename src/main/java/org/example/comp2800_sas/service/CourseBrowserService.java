package org.example.comp2800_sas.service;

import org.example.comp2800_sas.model.Course;
import org.example.comp2800_sas.model.CourseBrowserEntry;
import org.example.comp2800_sas.model.Prerequisite;
import org.example.comp2800_sas.repository.CourseRepository;
import org.example.comp2800_sas.repository.PrerequisiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Builds read-only course browser rows from course, section, and prerequisite data.
 */
@Service
@Transactional(readOnly = true)
public class CourseBrowserService {

    private final CourseRepository courseRepository;
    private final PrerequisiteRepository prerequisiteRepository;

    public CourseBrowserService(
            CourseRepository courseRepository,
            PrerequisiteRepository prerequisiteRepository
    ) {
        this.courseRepository = courseRepository;
        this.prerequisiteRepository = prerequisiteRepository;
    }

    public List<CourseBrowserEntry> loadCourses() {
        List<Course> courses = courseRepository.findAll().stream()
                .sorted(Comparator.comparing(course -> normalize(course.getCode())))
                .toList();

        Map<Integer, List<Prerequisite>> prerequisitesByCourseId = prerequisiteRepository.findAll().stream()
                .filter(prerequisite -> prerequisite.getCourse() != null)
                .filter(prerequisite -> prerequisite.getCourse().getCourseId() != null)
                .collect(Collectors.groupingBy(prerequisite -> prerequisite.getCourse().getCourseId()));

        return courses.stream()
                .map(course -> new CourseBrowserEntry(
                        course.getCourseId(),
                        clean(course.getCode()),
                        clean(course.getTitle()),
                        clean(course.getDescription()),
                        prerequisitesByCourseId.getOrDefault(course.getCourseId(), List.of()).stream()
                                .map(Prerequisite::getPrerequisiteCourse)
                                .filter(Objects::nonNull)
                                .map(this::formatCourseLabel)
                                .distinct()
                                .sorted(String.CASE_INSENSITIVE_ORDER)
                                .toList()
                ))
                .toList();
    }

    private String formatCourseLabel(Course course) {
        String code = clean(course.getCode());
        String title = clean(course.getTitle());
        if (code.isBlank()) {
            return title;
        }
        if (title.isBlank()) {
            return code;
        }
        return code + " " + title;
    }

    private String normalize(String value) {
        return clean(value).toLowerCase(Locale.ROOT);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
