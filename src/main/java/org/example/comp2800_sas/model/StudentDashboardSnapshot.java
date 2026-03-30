package org.example.comp2800_sas.model;

import java.util.List;

/**
 * Combined dashboard model containing the student's progress and upcoming sessions.
 */
public record StudentDashboardSnapshot(
        Integer studentId,
        String studentName,
        EnrollmentCatalogSummary catalogSummary,
        List<String> availableSessions,
        String primarySession,
        List<StudentDashboardSession> sessionSummaries,
        List<PlannedCourseOption> plannedOptions,
        List<PlannedCourseOption> primarySessionOptions,
        List<PlannedCourseOption> unscheduledOptions,
        List<PlannerConflict> conflicts,
        List<PlannerMeetingBlock> meetingBlocks,
        double totalUnits,
        int scheduledCourseCount
) {
    public StudentDashboardSnapshot {
        availableSessions = availableSessions == null ? List.of() : List.copyOf(availableSessions);
        sessionSummaries = sessionSummaries == null ? List.of() : List.copyOf(sessionSummaries);
        plannedOptions = plannedOptions == null ? List.of() : List.copyOf(plannedOptions);
        primarySessionOptions = primarySessionOptions == null ? List.of() : List.copyOf(primarySessionOptions);
        unscheduledOptions = unscheduledOptions == null ? List.of() : List.copyOf(unscheduledOptions);
        conflicts = conflicts == null ? List.of() : List.copyOf(conflicts);
        meetingBlocks = meetingBlocks == null ? List.of() : List.copyOf(meetingBlocks);
    }

    public int totalPlannedCourseCount() {
        return plannedOptions.size();
    }

    public int totalConflictCount() {
        return conflicts.size();
    }

    public int totalMeetingBlockCount() {
        return meetingBlocks.size();
    }

    public int unscheduledCourseCount() {
        return unscheduledOptions.size();
    }

    public int sessionCount() {
        return sessionSummaries.size();
    }

    public boolean hasPlans() {
        return !plannedOptions.isEmpty();
    }
}
