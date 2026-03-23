package org.example.comp2800_sas.service;

import org.example.comp2800_sas.model.EnrollmentCatalogCourse;
import org.example.comp2800_sas.model.EnrollmentCatalogOption;
import org.example.comp2800_sas.model.PlannedCourseOption;
import org.example.comp2800_sas.model.PlannerConflict;
import org.example.comp2800_sas.model.PlannerSelectionResult;
import org.example.comp2800_sas.model.PlannerSelectionStatus;
import org.example.comp2800_sas.util.PlannerScheduleUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class SemesterPlannerService {

    private final Map<String, List<PlannedCourseOption>> plansBySession = new LinkedHashMap<>();

    public synchronized PlannerSelectionResult addOption(
            EnrollmentCatalogCourse course,
            EnrollmentCatalogOption option
    ) {
        PlannedCourseOption plannedOption = PlannerScheduleUtils.toPlannedOption(course, option);
        List<PlannedCourseOption> sessionPlan = plansBySession.computeIfAbsent(
                plannedOption.session(),
                key -> new ArrayList<>()
        );

        if (sessionPlan.stream().anyMatch(existing -> existing.planId().equals(plannedOption.planId()))) {
            return new PlannerSelectionResult(PlannerSelectionStatus.DUPLICATE, plannedOption);
        }

        PlannerSelectionStatus status = PlannerSelectionStatus.ADDED;
        for (int i = 0; i < sessionPlan.size(); i++) {
            PlannedCourseOption existing = sessionPlan.get(i);
            if (existing.courseCode().equalsIgnoreCase(plannedOption.courseCode())) {
                sessionPlan.remove(i);
                status = PlannerSelectionStatus.REPLACED;
                break;
            }
        }

        sessionPlan.add(plannedOption);
        sessionPlan.sort(Comparator.comparing(optionEntry -> optionEntry.courseCode().toLowerCase(Locale.ROOT)));
        return new PlannerSelectionResult(status, plannedOption);
    }

    public synchronized boolean removePlannedOption(String session, String planId) {
        List<PlannedCourseOption> sessionPlan = plansBySession.get(session);
        if (sessionPlan == null) {
            return false;
        }

        boolean removed = sessionPlan.removeIf(option -> option.planId().equals(planId));
        if (sessionPlan.isEmpty()) {
            plansBySession.remove(session);
        }
        return removed;
    }

    public synchronized void clearPlan(String session) {
        plansBySession.remove(session);
    }

    public synchronized void clearAllPlans() {
        plansBySession.clear();
    }

    public synchronized List<PlannedCourseOption> getPlanForSession(String session) {
        List<PlannedCourseOption> plan = plansBySession.get(session);
        return plan == null ? List.of() : List.copyOf(plan);
    }

    public synchronized List<String> getPlannedSessions() {
        return List.copyOf(plansBySession.keySet());
    }

    public synchronized boolean isOptionPlanned(String session, String courseCode, String optionNumber) {
        return getPlanForSession(session).stream().anyMatch(option ->
                option.courseCode().equalsIgnoreCase(courseCode)
                        && option.optionNumber().equalsIgnoreCase(optionNumber)
        );
    }

    public synchronized boolean hasCoursePlannedInSession(String session, String courseCode) {
        return getPlanForSession(session).stream().anyMatch(option ->
                option.courseCode().equalsIgnoreCase(courseCode)
        );
    }

    public synchronized List<PlannerConflict> getConflictsForSession(String session) {
        return PlannerScheduleUtils.detectConflicts(getPlanForSession(session));
    }

    public synchronized int getTotalPlannedCount() {
        return plansBySession.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    public synchronized int getPlannedCountForSession(String session) {
        return getPlanForSession(session).size();
    }

    public synchronized int getTotalConflictCount() {
        return plansBySession.keySet().stream()
                .mapToInt(session -> getConflictsForSession(session).size())
                .sum();
    }

    public synchronized double getTotalUnitsForSession(String session) {
        return getPlanForSession(session).stream()
                .mapToDouble(option -> parseUnits(option.units()))
                .sum();
    }

    private double parseUnits(String units) {
        try {
            return Double.parseDouble(units);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }
}
