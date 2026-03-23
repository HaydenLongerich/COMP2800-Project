package org.example.comp2800_sas.model;

public record EnrollmentCatalogSummary(
        int totalCourses,
        int totalOfferings,
        int totalSections,
        int openCourses,
        int closedCourses
) {
}
