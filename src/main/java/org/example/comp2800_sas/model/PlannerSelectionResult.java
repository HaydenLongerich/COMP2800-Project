package org.example.comp2800_sas.model;

public record PlannerSelectionResult(
        PlannerSelectionStatus status,
        PlannedCourseOption plannedOption
) {
}
