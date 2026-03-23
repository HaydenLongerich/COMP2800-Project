package org.example.comp2800_sas.model;

import java.util.List;

public record EnrollmentCatalogData(
        List<EnrollmentCatalogCourse> courses,
        EnrollmentCatalogSummary summary,
        List<String> sessions,
        List<String> componentFilters
) {
    public EnrollmentCatalogData {
        courses = courses == null ? List.of() : List.copyOf(courses);
        sessions = sessions == null ? List.of() : List.copyOf(sessions);
        componentFilters = componentFilters == null ? List.of() : List.copyOf(componentFilters);
    }
}
