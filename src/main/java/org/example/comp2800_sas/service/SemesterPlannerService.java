package org.example.comp2800_sas.service;

import org.example.comp2800_sas.model.EnrollmentCatalogCourse;
import org.example.comp2800_sas.model.EnrollmentCatalogData;
import org.example.comp2800_sas.model.EnrollmentCatalogOption;
import org.example.comp2800_sas.model.PlannedCourseOption;
import org.example.comp2800_sas.model.PlannerConflict;
import org.example.comp2800_sas.model.PlannerSelection;
import org.example.comp2800_sas.model.PlannerSelectionResult;
import org.example.comp2800_sas.model.PlannerSelectionStatus;
import org.example.comp2800_sas.repository.PlannerSelectionRepository;
import org.example.comp2800_sas.repository.StudentRepository;
import org.example.comp2800_sas.util.PlannerScheduleUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional
public class SemesterPlannerService {

    private final PlannerSelectionRepository plannerSelectionRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentCatalogService enrollmentCatalogService;

    private Integer currentStudentId;
    private EnrollmentCatalogData catalogCache;
    private Map<String, List<PlannedCourseOption>> resolvedPlansCache;

    public SemesterPlannerService(
            PlannerSelectionRepository plannerSelectionRepository,
            StudentRepository studentRepository,
            EnrollmentCatalogService enrollmentCatalogService
    ) {
        this.plannerSelectionRepository = plannerSelectionRepository;
        this.studentRepository = studentRepository;
        this.enrollmentCatalogService = enrollmentCatalogService;
    }

    public synchronized void setCurrentStudent(Integer studentId) {
        this.currentStudentId = studentId;
        invalidatePlanCache();
    }

    public synchronized void clearCurrentStudent() {
        this.currentStudentId = null;
        invalidatePlanCache();
    }

    public synchronized PlannerSelectionResult addOption(
            EnrollmentCatalogCourse course,
            EnrollmentCatalogOption option
    ) {
        Integer studentId = requireCurrentStudentId();
        PlannedCourseOption plannedOption = PlannerScheduleUtils.toPlannedOption(course, option);

        Optional<PlannerSelection> existingSelection = plannerSelectionRepository
                .findByStudent_StudentIdAndSessionNameAndCourseCode(
                        studentId,
                        plannedOption.session(),
                        plannedOption.courseCode()
                );

        if (existingSelection.isPresent()
                && sameValue(existingSelection.get().getOptionNumber(), plannedOption.optionNumber())) {
            return new PlannerSelectionResult(PlannerSelectionStatus.DUPLICATE, plannedOption);
        }

        PlannerSelection selection = existingSelection.orElseGet(() ->
                new PlannerSelection(
                        studentRepository.getReferenceById(studentId),
                        plannedOption.session(),
                        plannedOption.courseCode(),
                        plannedOption.optionNumber()
                )
        );

        selection.setSessionName(plannedOption.session());
        selection.setCourseCode(plannedOption.courseCode());
        selection.setOptionNumber(plannedOption.optionNumber());
        plannerSelectionRepository.save(selection);

        invalidatePlanCache();
        PlannerSelectionStatus status = existingSelection.isPresent()
                ? PlannerSelectionStatus.REPLACED
                : PlannerSelectionStatus.ADDED;
        return new PlannerSelectionResult(status, plannedOption);
    }

    public synchronized boolean removePlannedOption(String session, String planId) {
        Integer studentId = currentStudentId;
        if (studentId == null || !hasText(session) || !hasText(planId)) {
            return false;
        }

        PlanKey planKey = parsePlanId(planId);
        if (planKey == null
                || !sameValue(session, planKey.session())
                || !hasText(planKey.courseCode())) {
            return false;
        }

        Optional<PlannerSelection> selection = plannerSelectionRepository
                .findByStudent_StudentIdAndSessionNameAndCourseCode(
                        studentId,
                        session,
                        planKey.courseCode()
                );

        if (selection.isEmpty() || !sameValue(selection.get().getOptionNumber(), planKey.optionNumber())) {
            return false;
        }

        plannerSelectionRepository.delete(selection.get());
        invalidatePlanCache();
        return true;
    }

    public synchronized void clearPlan(String session) {
        Integer studentId = currentStudentId;
        if (studentId == null || !hasText(session)) {
            return;
        }

        plannerSelectionRepository.deleteByStudent_StudentIdAndSessionName(studentId, session);
        invalidatePlanCache();
    }

    public synchronized void clearAllPlans() {
        Integer studentId = currentStudentId;
        if (studentId == null) {
            return;
        }

        plannerSelectionRepository.deleteByStudent_StudentId(studentId);
        invalidatePlanCache();
    }

    @Transactional(readOnly = true)
    public synchronized List<PlannedCourseOption> getPlanForSession(String session) {
        List<PlannedCourseOption> plan = getResolvedPlans().get(session);
        return plan == null ? List.of() : List.copyOf(plan);
    }

    @Transactional(readOnly = true)
    public synchronized List<String> getPlannedSessions() {
        return List.copyOf(getResolvedPlans().keySet());
    }

    @Transactional(readOnly = true)
    public synchronized boolean isOptionPlanned(String session, String courseCode, String optionNumber) {
        return getPlanForSession(session).stream().anyMatch(option ->
                sameValue(option.courseCode(), courseCode)
                        && sameValue(option.optionNumber(), optionNumber)
        );
    }

    @Transactional(readOnly = true)
    public synchronized boolean hasCoursePlannedInSession(String session, String courseCode) {
        return getPlanForSession(session).stream().anyMatch(option ->
                sameValue(option.courseCode(), courseCode)
        );
    }

    @Transactional(readOnly = true)
    public synchronized List<PlannerConflict> getConflictsForSession(String session) {
        return PlannerScheduleUtils.detectConflicts(getPlanForSession(session));
    }

    @Transactional(readOnly = true)
    public synchronized int getTotalPlannedCount() {
        return getResolvedPlans().values().stream()
                .mapToInt(List::size)
                .sum();
    }

    @Transactional(readOnly = true)
    public synchronized int getPlannedCountForSession(String session) {
        return getPlanForSession(session).size();
    }

    @Transactional(readOnly = true)
    public synchronized int getTotalConflictCount() {
        return getResolvedPlans().keySet().stream()
                .mapToInt(session -> PlannerScheduleUtils.detectConflicts(getPlanForSession(session)).size())
                .sum();
    }

    @Transactional(readOnly = true)
    public synchronized double getTotalUnitsForSession(String session) {
        return getPlanForSession(session).stream()
                .mapToDouble(option -> parseUnits(option.units()))
                .sum();
    }

    private Map<String, List<PlannedCourseOption>> getResolvedPlans() {
        if (currentStudentId == null) {
            return Map.of();
        }

        if (resolvedPlansCache == null) {
            resolvedPlansCache = loadResolvedPlans(currentStudentId);
        }

        return resolvedPlansCache;
    }

    private Map<String, List<PlannedCourseOption>> loadResolvedPlans(Integer studentId) {
        EnrollmentCatalogData catalog = getCatalog();
        Map<String, List<PlannedCourseOption>> plansBySession = new LinkedHashMap<>();

        for (PlannerSelection selection : plannerSelectionRepository
                .findByStudent_StudentIdOrderBySessionNameAscCourseCodeAsc(studentId)) {
            PlannedCourseOption plannedOption = resolvePlannedOption(selection, catalog);
            if (plannedOption == null) {
                continue;
            }

            plansBySession.computeIfAbsent(plannedOption.session(), key -> new ArrayList<>())
                    .add(plannedOption);
        }

        for (List<PlannedCourseOption> options : plansBySession.values()) {
            options.sort(Comparator.comparing(option -> normalize(option.courseCode())));
        }

        return plansBySession;
    }

    private PlannedCourseOption resolvePlannedOption(PlannerSelection selection, EnrollmentCatalogData catalog) {
        EnrollmentCatalogCourse course = catalog.courses().stream()
                .filter(entry -> sameValue(entry.courseCode(), selection.getCourseCode()))
                .findFirst()
                .orElse(null);

        if (course == null) {
            return null;
        }

        return course.options().stream()
                .filter(option -> sameValue(option.session(), selection.getSessionName()))
                .filter(option -> sameValue(option.optionNumber(), selection.getOptionNumber()))
                .findFirst()
                .map(option -> PlannerScheduleUtils.toPlannedOption(course, option))
                .orElse(null);
    }

    private EnrollmentCatalogData getCatalog() {
        if (catalogCache == null) {
            catalogCache = enrollmentCatalogService.loadCatalog();
        }
        return catalogCache;
    }

    private Integer requireCurrentStudentId() {
        if (currentStudentId == null) {
            throw new IllegalStateException("No current student is set for the semester planner.");
        }
        return currentStudentId;
    }

    private void invalidatePlanCache() {
        resolvedPlansCache = null;
    }

    private double parseUnits(String units) {
        try {
            return Double.parseDouble(units);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private PlanKey parsePlanId(String planId) {
        String[] parts = planId.split("\\|", 3);
        if (parts.length != 3) {
            return null;
        }
        return new PlanKey(parts[0], parts[1], parts[2]);
    }

    private boolean sameValue(String left, String right) {
        return Objects.equals(normalize(left), normalize(right));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record PlanKey(String session, String courseCode, String optionNumber) {
    }
}
