package org.example.comp2800_sas.model;

// Per-session summary shown on the student home dashboard.
public record StudentDashboardSession(
        String sessionName,
        int plannedCourseCount,
        int scheduledCourseCount,
        int meetingBlockCount,
        int conflictCount,
        double totalUnits
) {
}
