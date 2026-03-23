package org.example.comp2800_sas.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.comp2800_sas.model.EnrollmentCatalogCourse;
import org.example.comp2800_sas.model.EnrollmentCatalogData;
import org.example.comp2800_sas.model.EnrollmentCatalogSummary;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class EnrollmentCatalogService {

    private static final String CATALOG_FILE_NAME = "COMP COURSES FINAL.json";

    private final ObjectMapper objectMapper;

    public EnrollmentCatalogService() {
        objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public EnrollmentCatalogData loadCatalog() {
        List<EnrollmentCatalogCourse> courses = readCourses().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(course -> sortKey(course.courseCode())))
                .toList();

        int totalOfferings = courses.stream()
                .mapToInt(course -> course.options().size())
                .sum();

        int totalSections = courses.stream()
                .flatMap(course -> course.options().stream())
                .mapToInt(option -> option.sections().size())
                .sum();

        int openCourses = (int) courses.stream()
                .filter(this::hasOpenOffering)
                .count();

        EnrollmentCatalogSummary summary = new EnrollmentCatalogSummary(
                courses.size(),
                totalOfferings,
                totalSections,
                openCourses,
                Math.max(courses.size() - openCourses, 0)
        );

        List<String> sessions = courses.stream()
                .flatMap(course -> course.options().stream())
                .map(option -> clean(option.session()))
                .filter(value -> !value.isBlank())
                .distinct()
                .sorted(Comparator.comparing(this::sortKey))
                .toList();

        List<String> componentFilters = courses.stream()
                .map(EnrollmentCatalogCourse::components)
                .map(this::formatComponents)
                .filter(value -> !value.isBlank())
                .distinct()
                .sorted(Comparator.comparing(this::sortKey))
                .toList();

        return new EnrollmentCatalogData(courses, summary, sessions, componentFilters);
    }

    public String formatComponents(String components) {
        String cleaned = clean(components);
        if (cleaned.isBlank()) {
            return "";
        }

        String[] pieces = cleaned.split(",");
        for (int i = 0; i < pieces.length; i++) {
            pieces[i] = clean(pieces[i]);
        }
        return String.join(" + ", pieces);
    }

    public boolean hasOpenOffering(EnrollmentCatalogCourse course) {
        return course != null && course.options().stream().anyMatch(option ->
                option.isOpen() || option.sections().stream().anyMatch(section ->
                        section.seatsOpen() != null && section.seatsOpen() > 0
                )
        );
    }

    private List<EnrollmentCatalogCourse> readCourses() {
        Path catalogPath = resolveCatalogPath();
        try (InputStream inputStream = Files.newInputStream(catalogPath)) {
            return objectMapper.readValue(inputStream, new TypeReference<>() {});
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to load enrollment catalog from " + catalogPath.toAbsolutePath(),
                    exception
            );
        }
    }

    private Path resolveCatalogPath() {
        List<Path> candidates = List.of(
                Path.of(CATALOG_FILE_NAME),
                Path.of(System.getProperty("user.dir", ".")).resolve(CATALOG_FILE_NAME)
        );

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate;
            }
        }

        throw new IllegalStateException(
                "Enrollment catalog file not found. Expected " + CATALOG_FILE_NAME + " in the project root."
        );
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String sortKey(String value) {
        return clean(value).toLowerCase(Locale.ROOT);
    }
}
