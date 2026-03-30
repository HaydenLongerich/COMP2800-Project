package org.example.comp2800_sas.model;

/**
 * Lightweight summary metadata for a generated enrollment catalog.
 */
public record EnrollmentCatalogSummary(
        int totalCourses,
        int totalOfferings,
        int totalSections,
        int openCourses,
        int closedCourses
) {
}
