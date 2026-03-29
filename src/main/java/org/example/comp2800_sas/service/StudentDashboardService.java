package org.example.comp2800_sas.service;

import org.example.comp2800_sas.model.EnrollmentCatalogData;
import org.example.comp2800_sas.model.PlannedCourseOption;
import org.example.comp2800_sas.model.PlannerConflict;
import org.example.comp2800_sas.model.PlannerMeetingBlock;
import org.example.comp2800_sas.model.Student;
import org.example.comp2800_sas.model.StudentDashboardSession;
import org.example.comp2800_sas.model.StudentDashboardSnapshot;
import org.example.comp2800_sas.repository.StudentRepository;
import org.example.comp2800_sas.util.PlannerScheduleUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class StudentDashboardService {

    private final StudentRepository studentRepository;
    private final EnrollmentCatalogService enrollmentCatalogService;
    private final SemesterPlannerService semesterPlannerService;

    public StudentDashboardService(
            StudentRepository studentRepository,
            EnrollmentCatalogService enrollmentCatalogService,
            SemesterPlannerService semesterPlannerService
    ) {
        this.studentRepository = studentRepository;
        this.enrollmentCatalogService = enrollmentCatalogService;
        this.semesterPlannerService = semesterPlannerService;
    }

    public StudentDashboardSnapshot buildSnapshot(Integer studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student with ID " + studentId + " was not found."));
        EnrollmentCatalogData catalog = enrollmentCatalogService.loadCatalog();
        return buildSnapshot(student, catalog);
    }

    public List<StudentDashboardSnapshot> buildSnapshots(List<Student> students) {
        EnrollmentCatalogData catalog = enrollmentCatalogService.loadCatalog();
        return students.stream()
                .map(student -> buildSnapshot(student, catalog))
                .toList();
    }

    private StudentDashboardSnapshot buildSnapshot(Student student, EnrollmentCatalogData catalog) {
        Map<String, List<PlannedCourseOption>> plansBySession =
                semesterPlannerService.getPlanBySessionForStudent(student.getStudentId());

        List<StudentDashboardSession> sessionSummaries = plansBySession.entrySet().stream()
                .map(entry -> buildSessionSummary(entry.getKey(), entry.getValue()))
                .toList();

        List<PlannedCourseOption> plannedOptions = new ArrayList<>();
        for (List<PlannedCourseOption> sessionOptions : plansBySession.values()) {
            plannedOptions.addAll(sessionOptions);
        }

        String primarySession = !sessionSummaries.isEmpty()
                ? sessionSummaries.get(0).sessionName()
                : catalog.sessions().stream().findFirst().orElse("");

        List<PlannedCourseOption> primarySessionOptions = plansBySession.getOrDefault(primarySession, List.of());

        List<PlannedCourseOption> unscheduledOptions = plannedOptions.stream()
                .filter(option -> !option.hasScheduledMeetings())
                .toList();

        List<PlannerConflict> conflicts = plansBySession.entrySet().stream()
                .flatMap(entry -> PlannerScheduleUtils.detectConflicts(entry.getValue()).stream())
                .sorted(Comparator
                        .comparing(PlannerConflict::session)
                        .thenComparing(PlannerConflict::dayLabel)
                        .thenComparingInt(PlannerConflict::startMinutes))
                .toList();

        List<PlannerMeetingBlock> meetingBlocks = plannedOptions.stream()
                .flatMap(option -> option.meetingBlocks().stream())
                .sorted(Comparator
                        .comparing(PlannerMeetingBlock::dayIndex)
                        .thenComparingInt(PlannerMeetingBlock::startMinutes)
                        .thenComparing(PlannerMeetingBlock::courseCode))
                .toList();

        double totalUnits = plannedOptions.stream()
                .mapToDouble(option -> parseUnits(option.units()))
                .sum();

        int scheduledCourseCount = (int) plannedOptions.stream()
                .filter(PlannedCourseOption::hasScheduledMeetings)
                .count();

        return new StudentDashboardSnapshot(
                student.getStudentId(),
                student.getName(),
                catalog.summary(),
                catalog.sessions(),
                primarySession,
                sessionSummaries,
                plannedOptions,
                primarySessionOptions,
                unscheduledOptions,
                conflicts,
                meetingBlocks,
                totalUnits,
                scheduledCourseCount
        );
    }

    private StudentDashboardSession buildSessionSummary(String sessionName, List<PlannedCourseOption> options) {
        List<PlannerConflict> conflicts = PlannerScheduleUtils.detectConflicts(options);
        int scheduledCourseCount = (int) options.stream()
                .filter(PlannedCourseOption::hasScheduledMeetings)
                .count();
        int meetingBlockCount = options.stream()
                .mapToInt(option -> option.meetingBlocks().size())
                .sum();
        double totalUnits = options.stream()
                .mapToDouble(option -> parseUnits(option.units()))
                .sum();

        return new StudentDashboardSession(
                sessionName,
                options.size(),
                scheduledCourseCount,
                meetingBlockCount,
                conflicts.size(),
                totalUnits
        );
    }

    private double parseUnits(String units) {
        try {
            return Double.parseDouble(units);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }
}
