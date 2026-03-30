package org.example.comp2800_sas.model;

import java.time.LocalTime;

/**
 * Immutable scheduled block rendered on the weekly planner calendar.
 */
public record PlannerMeetingBlock(
        String blockId,
        String planId,
        String courseCode,
        String courseTitle,
        String optionNumber,
        String sectionType,
        String sectionNumber,
        String classNbr,
        String dayLabel,
        int dayIndex,
        LocalTime startTime,
        LocalTime endTime,
        int startMinutes,
        int endMinutes,
        String room,
        String instructor,
        String deliveryMode
) {
    public boolean overlaps(PlannerMeetingBlock other) {
        return other != null
                && dayIndex == other.dayIndex
                && startMinutes < other.endMinutes
                && other.startMinutes < endMinutes;
    }

    public int durationMinutes() {
        return Math.max(endMinutes - startMinutes, 0);
    }
}
