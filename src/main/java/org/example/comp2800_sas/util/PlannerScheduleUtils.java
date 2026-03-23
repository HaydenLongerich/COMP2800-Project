package org.example.comp2800_sas.util;

import org.example.comp2800_sas.model.EnrollmentCatalogCourse;
import org.example.comp2800_sas.model.EnrollmentCatalogOption;
import org.example.comp2800_sas.model.EnrollmentCatalogSection;
import org.example.comp2800_sas.model.PlannedCourseOption;
import org.example.comp2800_sas.model.PlannerConflict;
import org.example.comp2800_sas.model.PlannerMeetingBlock;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class PlannerScheduleUtils {

    private static final List<String> DAY_NAMES = List.of(
            "Monday",
            "Tuesday",
            "Wednesday",
            "Thursday",
            "Friday",
            "Saturday",
            "Sunday"
    );

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mma", Locale.US);

    private PlannerScheduleUtils() {
    }

    public static PlannedCourseOption toPlannedOption(EnrollmentCatalogCourse course, EnrollmentCatalogOption option) {
        List<PlannerMeetingBlock> meetingBlocks = buildMeetingBlocks(course, option);

        return new PlannedCourseOption(
                planId(course, option),
                fallback(option.session(), "Unscheduled"),
                fallback(course.courseCode(), "TBA"),
                fallback(course.courseName(), "Untitled Course"),
                fallback(course.units(), "0"),
                fallback(course.grading(), "N/A"),
                fallback(course.components(), "N/A"),
                fallback(course.courseCareer(), "N/A"),
                course.detailUrl(),
                fallback(option.optionNumber(), "N/A"),
                fallback(option.status(), "Unknown"),
                summarizeDeliveryMode(option.sections()),
                option.sections(),
                meetingBlocks
        );
    }

    public static List<PlannerConflict> detectConflicts(List<PlannedCourseOption> plannedOptions) {
        List<PlannerMeetingBlock> blocks = plannedOptions.stream()
                .flatMap(option -> option.meetingBlocks().stream())
                .toList();

        List<PlannerConflict> conflicts = new ArrayList<>();
        for (int i = 0; i < blocks.size(); i++) {
            PlannerMeetingBlock first = blocks.get(i);
            for (int j = i + 1; j < blocks.size(); j++) {
                PlannerMeetingBlock second = blocks.get(j);
                if (first.planId().equals(second.planId())) {
                    continue;
                }
                if (first.overlaps(second)) {
                    conflicts.add(new PlannerConflict(
                            sessionFor(first, plannedOptions),
                            first.dayLabel(),
                            Math.max(first.startMinutes(), second.startMinutes()),
                            Math.min(first.endMinutes(), second.endMinutes()),
                            first,
                            second
                    ));
                }
            }
        }
        return conflicts;
    }

    public static String summarizeDeliveryMode(List<EnrollmentCatalogSection> sections) {
        Set<String> modes = new LinkedHashSet<>();
        for (EnrollmentCatalogSection section : sections) {
            modes.add(inferDeliveryMode(section.room()));
        }
        if (modes.isEmpty()) {
            return "TBA";
        }
        return String.join(" + ", modes);
    }

    public static String inferDeliveryMode(String room) {
        String normalizedRoom = normalize(room);
        if (normalizedRoom.contains("hybrid")) {
            return "Hybrid";
        }
        if (normalizedRoom.contains("online")) {
            return "Online";
        }
        if (normalizedRoom.isBlank() || "not applicable".equals(normalizedRoom)) {
            return "TBA";
        }
        return "In Person";
    }

    public static String cleanDisplay(String value) {
        String cleaned = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (cleaned.isBlank()) {
            return "";
        }

        String[] words = cleaned.split(" ");
        if (words.length % 2 == 0) {
            String firstHalf = String.join(" ", Arrays.copyOfRange(words, 0, words.length / 2));
            String secondHalf = String.join(" ", Arrays.copyOfRange(words, words.length / 2, words.length));
            if (firstHalf.equalsIgnoreCase(secondHalf)) {
                return firstHalf;
            }
        }

        return cleaned;
    }

    private static List<PlannerMeetingBlock> buildMeetingBlocks(
            EnrollmentCatalogCourse course,
            EnrollmentCatalogOption option
    ) {
        List<PlannerMeetingBlock> blocks = new ArrayList<>();

        for (EnrollmentCatalogSection section : option.sections()) {
            List<String> days = parseDays(section.days());
            TimeRange timeRange = parseTimeRange(section.time());
            if (days.isEmpty() || timeRange == null) {
                continue;
            }

            for (String day : days) {
                int dayIndex = DAY_NAMES.indexOf(day);
                if (dayIndex < 0) {
                    continue;
                }

                blocks.add(new PlannerMeetingBlock(
                        planId(course, option) + "|" + fallback(section.sectionType(), "SEC") + "|" + day,
                        planId(course, option),
                        fallback(course.courseCode(), "TBA"),
                        fallback(course.courseName(), "Untitled Course"),
                        fallback(option.optionNumber(), "N/A"),
                        fallback(section.sectionType(), "SEC"),
                        fallback(section.section(), "N/A"),
                        fallback(section.classNbr(), "N/A"),
                        day,
                        dayIndex,
                        timeRange.start(),
                        timeRange.end(),
                        timeRange.start().getHour() * 60 + timeRange.start().getMinute(),
                        timeRange.end().getHour() * 60 + timeRange.end().getMinute(),
                        cleanDisplay(section.room()),
                        cleanDisplay(section.instructor()),
                        inferDeliveryMode(section.room())
                ));
            }
        }

        return blocks;
    }

    private static List<String> parseDays(String daysText) {
        String normalizedDays = normalize(daysText);
        if (normalizedDays.isBlank() || normalizedDays.contains("not applicable")) {
            return List.of();
        }

        List<String> days = new ArrayList<>();
        for (String dayName : DAY_NAMES) {
            if (normalizedDays.contains(dayName.toLowerCase(Locale.ROOT))) {
                days.add(dayName);
            }
        }
        return days;
    }

    private static TimeRange parseTimeRange(String timeText) {
        String cleaned = cleanDisplay(timeText);
        if (cleaned.isBlank() || "Not Applicable".equalsIgnoreCase(cleaned)) {
            return null;
        }

        String[] pieces = cleaned.split("\\s+to\\s+");
        if (pieces.length != 2) {
            return null;
        }

        try {
            LocalTime start = LocalTime.parse(pieces[0].replace(" ", ""), TIME_FORMATTER);
            LocalTime end = LocalTime.parse(pieces[1].replace(" ", ""), TIME_FORMATTER);
            if (!end.isAfter(start)) {
                return null;
            }
            return new TimeRange(start, end);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private static String sessionFor(PlannerMeetingBlock block, List<PlannedCourseOption> plannedOptions) {
        return plannedOptions.stream()
                .filter(option -> option.planId().equals(block.planId()))
                .map(PlannedCourseOption::session)
                .findFirst()
                .orElse("Unknown");
    }

    private static String planId(EnrollmentCatalogCourse course, EnrollmentCatalogOption option) {
        return fallback(option.session(), "Unscheduled") + "|"
                + fallback(course.courseCode(), "TBA") + "|"
                + fallback(option.optionNumber(), "N/A");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private record TimeRange(LocalTime start, LocalTime end) {
    }
}
