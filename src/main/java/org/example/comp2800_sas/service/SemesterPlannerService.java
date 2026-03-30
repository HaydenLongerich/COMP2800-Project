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
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional
// Owns the student's draft calendar state.
public class SemesterPlannerService {

    private final PlannerSelectionRepository plannerSelectionRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentCatalogService enrollmentCatalogService;

    private Integer currentStudentId;
    private Map<String, List<PlannedCourseOption>> resolvedPlansCache;
    private PlannerMetrics plannerMetricsCache;

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

    @Transactional(readOnly = true)
    public synchronized Map<String, List<PlannedCourseOption>> getPlanBySessionForStudent(Integer studentId) {
        if (studentId == null) {
            return Map.of();
        }
        return copyPlans(loadResolvedPlans(studentId));
    }

    @Transactional(readOnly = true)
    public synchronized Map<String, List<PlannedCourseOption>> getCurrentPlanBySession() {
        return copyPlans(getResolvedPlans());
    }

    @Transactional(readOnly = true)
    public synchronized List<PlannedCourseOption> getAllPlannedOptionsForStudent(Integer studentId) {
        return getPlanBySessionForStudent(studentId).values().stream()
                .flatMap(List::stream)
                .toList();
    }

    public synchronized void refreshCatalogState() {
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
            return new PlannerSelectionResult(PlannerSelectionStatus.DUPLICATE, plannedOption, List.of());
        }

        // Compute conflicts against the would-be plan before saving so the UI can show immediate feedback.
        List<PlannedCourseOption> previewPlan = new ArrayList<>(getPlanForSession(plannedOption.session()));
        previewPlan.removeIf(existing -> sameValue(existing.courseCode(), plannedOption.courseCode()));
        previewPlan.add(plannedOption);
        List<PlannerConflict> candidateConflicts = detectCandidateConflicts(previewPlan, plannedOption.planId());

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
        return new PlannerSelectionResult(status, plannedOption, candidateConflicts);
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
        SessionMetrics sessionMetrics = getPlannerMetrics().sessions().get(session);
        return sessionMetrics == null ? List.of() : sessionMetrics.conflicts();
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
        return getPlannerMetrics().totalConflictCount();
    }

    @Transactional(readOnly = true)
    public synchronized double getTotalUnitsForSession(String session) {
        SessionMetrics sessionMetrics = getPlannerMetrics().sessions().get(session);
        return sessionMetrics == null ? 0 : sessionMetrics.totalUnits();
    }

    private Map<String, List<PlannedCourseOption>> getResolvedPlans() {
        if (currentStudentId == null) {
            return Map.of();
        }

        // Resolve the current student's saved selections only once until planner state changes.
        if (resolvedPlansCache == null) {
            resolvedPlansCache = loadResolvedPlans(currentStudentId);
        }

        return resolvedPlansCache;
    }

    private PlannerMetrics getPlannerMetrics() {
        if (currentStudentId == null) {
            return PlannerMetrics.empty();
        }

        // Conflict detection and unit totals are reused heavily by Home, Enrollment, and Calendar.
        if (plannerMetricsCache == null) {
            plannerMetricsCache = buildPlannerMetrics(getResolvedPlans());
        }

        return plannerMetricsCache;
    }

    private PlannerMetrics buildPlannerMetrics(Map<String, List<PlannedCourseOption>> plansBySession) {
        Map<String, SessionMetrics> sessionMetrics = new LinkedHashMap<>();
        int totalConflictCount = 0;

        for (Map.Entry<String, List<PlannedCourseOption>> entry : plansBySession.entrySet()) {
            List<PlannerConflict> conflicts = PlannerScheduleUtils.detectConflicts(entry.getValue());
            double totalUnits = entry.getValue().stream()
                    .mapToDouble(option -> parseUnits(option.units()))
                    .sum();

            sessionMetrics.put(
                    entry.getKey(),
                    new SessionMetrics(List.copyOf(conflicts), totalUnits)
            );
            totalConflictCount += conflicts.size();
        }

        return new PlannerMetrics(Collections.unmodifiableMap(sessionMetrics), totalConflictCount);
    }

    private Map<String, List<PlannedCourseOption>> loadResolvedPlans(Integer studentId) {
        EnrollmentCatalogData catalog = enrollmentCatalogService.loadCatalog();
        Map<String, List<PlannedCourseOption>> plansBySession = new LinkedHashMap<>();

        // Rebuild saved planner rows into rich UI-ready options using the latest catalog snapshot.
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

    private Integer requireCurrentStudentId() {
        if (currentStudentId == null) {
            throw new IllegalStateException("No current student is set for the semester planner.");
        }
        return currentStudentId;
    }

    private void invalidatePlanCache() {
        resolvedPlansCache = null;
        plannerMetricsCache = null;
    }

    private Map<String, List<PlannedCourseOption>> copyPlans(Map<String, List<PlannedCourseOption>> source) {
        Map<String, List<PlannedCourseOption>> copiedPlans = new LinkedHashMap<>();
        for (Map.Entry<String, List<PlannedCourseOption>> entry : source.entrySet()) {
            copiedPlans.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(copiedPlans);
    }

    private double parseUnits(String units) {
        try {
            return Double.parseDouble(units);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private List<PlannerConflict> detectCandidateConflicts(List<PlannedCourseOption> previewPlan, String candidatePlanId) {
        return PlannerScheduleUtils.detectConflicts(previewPlan).stream()
                .filter(conflict ->
                        sameValue(conflict.first().planId(), candidatePlanId)
                                || sameValue(conflict.second().planId(), candidatePlanId)
                )
                .toList();
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

    private record SessionMetrics(List<PlannerConflict> conflicts, double totalUnits) {
    }

    private record PlannerMetrics(Map<String, SessionMetrics> sessions, int totalConflictCount) {
        private static PlannerMetrics empty() {
            return new PlannerMetrics(Map.of(), 0);
        }
    }
}
