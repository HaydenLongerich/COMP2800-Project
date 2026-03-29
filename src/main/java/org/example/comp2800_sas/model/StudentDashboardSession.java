package org.example.comp2800_sas.model;

public record StudentDashboardSession(
        String sessionName,
        int plannedCourseCount,
        int scheduledCourseCount,
        int meetingBlockCount,
        int conflictCount,
        double totalUnits
) {
}
