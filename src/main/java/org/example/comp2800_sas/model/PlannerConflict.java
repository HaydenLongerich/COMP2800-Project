package org.example.comp2800_sas.model;

// Immutable pair of meeting blocks that overlap in the planner.
public record PlannerConflict(
        String session,
        String dayLabel,
        int startMinutes,
        int endMinutes,
        PlannerMeetingBlock first,
        PlannerMeetingBlock second
) {
}
