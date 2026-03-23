package org.example.comp2800_sas.model;

public record PlannerConflict(
        String session,
        String dayLabel,
        int startMinutes,
        int endMinutes,
        PlannerMeetingBlock first,
        PlannerMeetingBlock second
) {
}
