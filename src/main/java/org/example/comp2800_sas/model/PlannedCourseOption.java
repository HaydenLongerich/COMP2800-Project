package org.example.comp2800_sas.model;

import java.util.List;

/**
 * Immutable planner entry representing one selected course option.
 */
public record PlannedCourseOption(
        String planId,
        String session,
        String courseCode,
        String courseTitle,
        String units,
        String grading,
        String components,
        String courseCareer,
        String detailUrl,
        String optionNumber,
        String optionStatus,
        String deliveryMode,
        List<EnrollmentCatalogSection> sections,
        List<PlannerMeetingBlock> meetingBlocks
) {
    public PlannedCourseOption {
        sections = sections == null ? List.of() : List.copyOf(sections);
        meetingBlocks = meetingBlocks == null ? List.of() : List.copyOf(meetingBlocks);
    }

    public boolean hasScheduledMeetings() {
        return !meetingBlocks.isEmpty();
    }

    public int sectionCount() {
        return sections.size();
    }
}
