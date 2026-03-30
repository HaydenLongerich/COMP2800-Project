package org.example.comp2800_sas.model;

import java.util.List;

// Outcome of a planner add or remove request, including any detected conflicts.
public record PlannerSelectionResult(
        PlannerSelectionStatus status,
        PlannedCourseOption plannedOption,
        List<PlannerConflict> conflicts
) {
    public PlannerSelectionResult {
        conflicts = conflicts == null ? List.of() : List.copyOf(conflicts);
    }

    public boolean hasConflicts() {
        return !conflicts.isEmpty();
    }
}
