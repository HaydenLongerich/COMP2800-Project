package org.example.comp2800_sas.controller;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.example.comp2800_sas.model.Enrollment;
import org.example.comp2800_sas.model.EnrollmentStatus;
import org.example.comp2800_sas.model.PlannedCourseOption;
import org.example.comp2800_sas.model.Student;
import org.example.comp2800_sas.model.StudentDashboardSession;
import org.example.comp2800_sas.model.StudentDashboardSnapshot;
import org.example.comp2800_sas.service.EnrollmentService;
import org.example.comp2800_sas.service.SessionService;
import org.example.comp2800_sas.service.StudentDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * JavaFX controller for the reports dashboard.
 */
@Component
@Scope("prototype")
public class ReportsController {

    private static final String SURFACE_CARD_STYLE =
            "-fx-background-color: white; -fx-background-radius: 18; -fx-border-radius: 18; " +
                    "-fx-border-color: #d9e3ef; -fx-effect: dropshadow(gaussian, rgba(19,49,77,0.08), 18, 0.18, 0, 6);";

    @Autowired private EnrollmentService enrollmentService;
    @Autowired private SessionService sessionService;
    @Autowired private StudentDashboardService studentDashboardService;

    @FXML private VBox root;

    private Runnable openEnrollmentAction = () -> {};
    private Runnable openCalendarAction = () -> {};

    public void setNavigationActions(Runnable openEnrollmentAction, Runnable openCalendarAction) {
        this.openEnrollmentAction = openEnrollmentAction == null ? () -> {} : openEnrollmentAction;
        this.openCalendarAction = openCalendarAction == null ? () -> {} : openCalendarAction;
    }

    @FXML
    public void initialize() {
        loadReports();
    }

    private void loadReports() {
        root.getChildren().clear();

        Student currentStudent = sessionService.getCurrentStudent();
        if (currentStudent == null) {
            Label empty = new Label("No active student session is available. Sign in again to open reports.");
            empty.setStyle("-fx-text-fill: #607286; -fx-font-size: 13px;");
            root.getChildren().add(empty);
            return;
        }

        StackPane spinnerPane = new StackPane();
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(50, 50);
        spinnerPane.getChildren().add(spinner);
        VBox.setVgrow(spinnerPane, Priority.ALWAYS);
        root.getChildren().add(spinnerPane);

        Task<ReportsData> task = new Task<>() {
            @Override
            protected ReportsData call() {
                Integer studentId = currentStudent.getStudentId();
                return new ReportsData(
                        studentDashboardService.buildSnapshot(studentId),
                        enrollmentService.getEnrollmentsForStudent(studentId)
                );
            }
        };

        task.setOnSucceeded(event -> {
            root.getChildren().clear();
            buildReports(task.getValue());
        });

        task.setOnFailed(event -> {
            root.getChildren().clear();
            Label error = new Label("Failed to load reports.");
            error.setStyle("-fx-text-fill: #b94141; -fx-font-size: 13px; -fx-font-weight: bold;");
            root.getChildren().add(error);
        });

        Thread loader = new Thread(task);
        loader.setDaemon(true);
        loader.start();
    }

    private void buildReports(ReportsData reportsData) {
        StudentDashboardSnapshot snapshot = reportsData.snapshot();
        List<Enrollment> recordedEnrollments = reportsData.recordedEnrollments();

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox content = new VBox(18);
        content.setPadding(new Insets(24));

        content.getChildren().add(createHero(snapshot, recordedEnrollments));

        HBox statsRow = new HBox(14,
                createStatCard(String.valueOf(snapshot.totalPlannedCourseCount()), "Planned Courses", "Shared planner selections", "#173b63"),
                createStatCard(formatUnits(snapshot.totalUnits()), "Total Units", "Across your current draft", "#1c5f93"),
                createStatCard(String.valueOf(snapshot.totalConflictCount()), "Conflicts", "Calendar overlaps detected", snapshot.totalConflictCount() > 0 ? "#b94141" : "#88691b"),
                createStatCard(String.valueOf(recordedEnrollments.size()), "Recorded Enrollments", "Database history on file", "#246344")
        );
        content.getChildren().add(statsRow);

        HBox planningRow = new HBox(18,
                createPlanningSummaryCard(snapshot),
                createPlannedCoursesCard(snapshot)
        );
        planningRow.setAlignment(Pos.TOP_LEFT);
        content.getChildren().add(planningRow);

        List<Enrollment> passed = recordedEnrollments.stream()
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.PASSED)
                .collect(Collectors.toList());
        List<Enrollment> failed = recordedEnrollments.stream()
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.FAILED)
                .collect(Collectors.toList());
        List<Enrollment> activeRecords = recordedEnrollments.stream()
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.ENROLLED || enrollment.getStatus() == EnrollmentStatus.DROPPED)
                .collect(Collectors.toList());

        content.getChildren().add(buildRecordedTable("Recorded Enrollments", "#173b63", activeRecords));
        content.getChildren().add(buildRecordedTable("Passed Courses", "#246344", passed));
        content.getChildren().add(buildRecordedTable("Failed Courses", "#b94141", failed));

        scroll.setContent(content);
        root.getChildren().add(scroll);
    }

    private VBox createHero(StudentDashboardSnapshot snapshot, List<Enrollment> recordedEnrollments) {
        VBox card = new VBox(14);
        card.setPadding(new Insets(22, 24, 22, 24));
        card.setStyle(SURFACE_CARD_STYLE + "-fx-background-color: linear-gradient(to right, #ffffff, #f6f9fd);");


        Label title = new Label(snapshot.studentName());
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #173b63;");



        FlowPane sessionChips = new FlowPane(8, 8);
        if (snapshot.sessionSummaries().isEmpty()) {
            sessionChips.getChildren().add(createBadge("No planned sessions", "-fx-background-color: #eef4fb; -fx-text-fill: #31506f;"));
        } else {
            snapshot.sessionSummaries().forEach(session ->
                    sessionChips.getChildren().add(createBadge(
                            session.sessionName() + " - " + session.plannedCourseCount() + " planned",
                            "-fx-background-color: #eef4fb; -fx-text-fill: #31506f;"
                    ))
            );
        }

        HBox actions = new HBox(10);
        Button enrollmentButton = createActionButton("Open Enrollment", "#f5c518", "#173b63");
        enrollmentButton.setOnAction(event -> openEnrollmentAction.run());
        Button calendarButton = createActionButton("Open Calendar", "#173b63", "white");
        calendarButton.setOnAction(event -> openCalendarAction.run());
        actions.getChildren().addAll(enrollmentButton, calendarButton);

        VBox textBlock = new VBox(10, title, sessionChips, actions);
        HBox.setHgrow(textBlock, Priority.ALWAYS);

        VBox metric = new VBox(6);
        metric.setPadding(new Insets(14, 18, 14, 18));
        metric.setStyle("-fx-background-color: #173b63; -fx-background-radius: 16;");
        metric.setAlignment(Pos.CENTER_LEFT);

        Label metricValue = new Label(calculateAverageGrade(recordedEnrollments));
        metricValue.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #f5c518;");

        Label metricLabel = new Label("Average grade on file");
        metricLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #b6cee6;");

        Label metricSubtext = new Label(snapshot.primarySession() == null || snapshot.primarySession().isBlank()
                ? "Planner-first view"
                : snapshot.primarySession());
        metricSubtext.setStyle("-fx-font-size: 11px; -fx-text-fill: #d7e4f1;");

        metric.getChildren().addAll(metricValue, metricLabel, metricSubtext);

        HBox row = new HBox(18, textBlock, metric);
        row.setAlignment(Pos.TOP_LEFT);
        card.getChildren().add(row);
        return card;
    }

    private VBox createPlanningSummaryCard(StudentDashboardSnapshot snapshot) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(18, 18, 18, 18));
        card.setStyle(SURFACE_CARD_STYLE);
        HBox.setHgrow(card, Priority.ALWAYS);

        Label title = new Label("Session Planning Summary");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #173b63;");

        Label helper = new Label(
                snapshot.catalogSummary().openCourses() + " open catalog courses are currently available in the shared catalog."
        );
        helper.setWrapText(true);
        helper.setStyle("-fx-font-size: 12px; -fx-text-fill: #607286;");

        VBox rows = new VBox(10);
        if (snapshot.sessionSummaries().isEmpty()) {
            rows.getChildren().add(createEmptyMessage(
                    "No planner sessions exist yet. Start in Enrollment, then return here for live reporting."
            ));
        } else {
            for (StudentDashboardSession session : snapshot.sessionSummaries()) {
                VBox row = new VBox(8);
                row.setPadding(new Insets(12, 14, 12, 14));
                row.setStyle("-fx-background-color: #f7fafe; -fx-background-radius: 14; -fx-border-radius: 14; -fx-border-color: #d9e3ef;");

                HBox header = new HBox(10);
                Label name = new Label(session.sessionName());
                name.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #173b63;");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Label units = new Label(formatUnits(session.totalUnits()) + " units");
                units.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #31506f;");
                header.getChildren().addAll(name, spacer, units);

                FlowPane metrics = new FlowPane(8, 8);
                metrics.getChildren().addAll(
                        createBadge(session.plannedCourseCount() + " planned", "-fx-background-color: #eef4fb; -fx-text-fill: #31506f;"),
                        createBadge(session.scheduledCourseCount() + " scheduled", "-fx-background-color: #eef4fb; -fx-text-fill: #31506f;"),
                        createBadge(session.conflictCount() + " conflicts",
                                session.conflictCount() > 0
                                        ? "-fx-background-color: #fff1f1; -fx-text-fill: #9f3030;"
                                        : "-fx-background-color: #f3fbf5; -fx-text-fill: #246344;"),
                        createBadge(session.meetingBlockCount() + " class blocks", "-fx-background-color: #eef4fb; -fx-text-fill: #31506f;")
                );

                row.getChildren().addAll(header, metrics);
                rows.getChildren().add(row);
            }
        }

        card.getChildren().addAll(title, helper, rows);
        return card;
    }

    private VBox createPlannedCoursesCard(StudentDashboardSnapshot snapshot) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(18, 18, 18, 18));
        card.setStyle(SURFACE_CARD_STYLE);
        HBox.setHgrow(card, Priority.ALWAYS);

        Label title = new Label("Courses in Your Planner");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #173b63;");

        Label helper = new Label("These entries are pulled directly from the same shared planner selections used by Calendar.");
        helper.setWrapText(true);
        helper.setStyle("-fx-font-size: 12px; -fx-text-fill: #607286;");

        VBox rows = new VBox(10);
        if (snapshot.plannedOptions().isEmpty()) {
            rows.getChildren().add(createEmptyMessage(
                    "No courses are currently in your planner. Use Enrollment to add an option first."
            ));
        } else {
            snapshot.plannedOptions().stream().limit(10).forEach(option -> rows.getChildren().add(createPlannerRow(option)));
            if (snapshot.plannedOptions().size() > 10) {
                Label more = new Label("+" + (snapshot.plannedOptions().size() - 10) + " more planned courses");
                more.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #607286;");
                rows.getChildren().add(more);
            }
        }

        card.getChildren().addAll(title, helper, rows);
        return card;
    }

    private VBox createPlannerRow(PlannedCourseOption option) {
        VBox row = new VBox(8);
        row.setPadding(new Insets(12, 14, 12, 14));
        row.setStyle("-fx-background-color: #f7fafe; -fx-background-radius: 14; -fx-border-radius: 14; -fx-border-color: #d9e3ef;");

        HBox header = new HBox(10);
        Label code = new Label(option.courseCode());
        code.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #1c4a86;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label session = new Label(option.session());
        session.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #31506f;");
        header.getChildren().addAll(code, spacer, session);

        Label title = new Label(option.courseTitle());
        title.setWrapText(true);
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #173b63;");

        FlowPane badges = new FlowPane(8, 8);
        badges.getChildren().addAll(
                createBadge("Option " + option.optionNumber(), "-fx-background-color: #eef4fb; -fx-text-fill: #31506f;"),
                createBadge(option.deliveryMode(), "-fx-background-color: #eef4fb; -fx-text-fill: #31506f;"),
                "Online".equalsIgnoreCase(option.deliveryMode())
                        ? createBadge("Online – Async", "-fx-background-color: #dff3ff; -fx-text-fill: #1c5f93;")
                        : createBadge(option.hasScheduledMeetings() ? "Scheduled" : "Needs timing",
                        option.hasScheduledMeetings()
                                ? "-fx-background-color: #dcf5e5; -fx-text-fill: #246344;"
                                : "-fx-background-color: #fff8e6; -fx-text-fill: #7a5a00;")
        );

        Label detail = new Label(
                formatUnits(parseUnits(option.units())) + " units  |  " +
                        option.sections().size() + " sections  |  " +
                        option.meetingBlocks().size() + " class blocks"
        );
        detail.setStyle("-fx-font-size: 12px; -fx-text-fill: #607286;");

        row.getChildren().addAll(header, title, badges, detail);
        return row;
    }

    private VBox buildRecordedTable(String heading, String color, List<Enrollment> enrollments) {
        VBox card = new VBox(0);
        card.setStyle(SURFACE_CARD_STYLE);

        HBox cardHeader = new HBox();
        cardHeader.setPadding(new Insets(14, 20, 14, 20));
        cardHeader.setAlignment(Pos.CENTER_LEFT);
        cardHeader.setStyle("-fx-border-color: transparent transparent #f0f0f0 transparent;");

        Label headingLabel = new Label(heading);
        headingLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label countLabel = new Label(enrollments.size() + " record" + (enrollments.size() == 1 ? "" : "s"));
        countLabel.setStyle("-fx-text-fill: #607286; -fx-font-size: 12px;");

        cardHeader.getChildren().addAll(headingLabel, spacer, countLabel);
        card.getChildren().add(cardHeader);

        if (enrollments.isEmpty()) {
            HBox emptyRow = new HBox();
            emptyRow.setPadding(new Insets(16, 20, 16, 20));
            Label empty = new Label("No records in this section yet.");
            empty.setStyle("-fx-text-fill: #607286; -fx-font-size: 12px;");
            emptyRow.getChildren().add(empty);
            card.getChildren().add(emptyRow);
            return card;
        }

        HBox colHeader = new HBox(16);
        colHeader.setPadding(new Insets(8, 20, 8, 20));
        colHeader.setStyle("-fx-background-color: #f8f9fa;");
        colHeader.getChildren().addAll(
                col("CODE", 100),
                col("TITLE", 300),
                col("SECTION", 90),
                col("STATUS", 100),
                col("GRADE", 90)
        );
        card.getChildren().add(colHeader);

        for (int i = 0; i < enrollments.size(); i++) {
            Enrollment enrollment = enrollments.get(i);
            HBox row = new HBox(16);
            row.setPadding(new Insets(12, 20, 12, 20));
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: " + (i % 2 == 0 ? "white" : "#fafbff") + "; " +
                    "-fx-border-color: transparent transparent #f0f0f0 transparent;");

            Label code = value(enrollment.getSection().getCourse().getCode(), 100, true);
            Label title = value(enrollment.getSection().getCourse().getTitle(), 300, false);
            Label section = value("Section " + enrollment.getSection().getSectionNumber(), 90, false);
            Label status = value(enrollment.getStatus().name(), 100, false);
            Label grade = value(enrollment.getGrade() == null ? "-" : enrollment.getGrade() + "%", 90, false);

            row.getChildren().addAll(code, title, section, status, grade);
            card.getChildren().add(row);
        }

        return card;
    }

    private VBox createStatCard(String value, String labelText, String subText, String accentColor) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(16, 18, 16, 18));
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16; -fx-border-radius: 16; " +
                        "-fx-border-color: #d9e3ef; -fx-effect: dropshadow(gaussian, rgba(19,49,77,0.06), 14, 0.16, 0, 4);"
        );
        HBox.setHgrow(card, Priority.ALWAYS);

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: " + accentColor + ";");

        Label label = new Label(labelText);
        label.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #173b63;");

        Label note = new Label(subText);
        note.setWrapText(true);
        note.setStyle("-fx-font-size: 11px; -fx-text-fill: #607286;");

        card.getChildren().addAll(valueLabel, label, note);
        return card;
    }

    private Label createBadge(String text, String extraStyle) {
        Label badge = new Label(text);
        badge.setStyle(
                "-fx-background-radius: 999; -fx-padding: 5 10; -fx-font-size: 11px; -fx-font-weight: bold; " +
                        extraStyle
        );
        return badge;
    }

    private Button createActionButton(String text, String backgroundColor, String textColor) {
        Button button = new Button(text);
        button.setStyle(
                "-fx-background-color: " + backgroundColor + "; " +
                        "-fx-text-fill: " + textColor + "; " +
                        "-fx-font-weight: bold; -fx-background-radius: 999; " +
                        "-fx-padding: 8 16; -fx-cursor: hand;"
        );
        return button;
    }

    private Label createEmptyMessage(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: #607286;");
        return label;
    }

    private Label col(String text, double width) {
        Label label = new Label(text);
        label.setPrefWidth(width);
        label.setStyle("-fx-text-fill: #173b63; -fx-font-size: 11px; -fx-font-weight: bold;");
        return label;
    }

    private Label value(String text, double width, boolean bold) {
        Label label = new Label(text);
        label.setPrefWidth(width);
        label.setStyle(
                "-fx-text-fill: #333; -fx-font-size: 12px; " +
                        (bold ? "-fx-font-weight: bold;" : "")
        );
        return label;
    }

    private String calculateAverageGrade(List<Enrollment> enrollments) {
        List<Enrollment> graded = enrollments.stream()
                .filter(enrollment -> enrollment.getGrade() != null)
                .collect(Collectors.toList());
        if (graded.isEmpty()) {
            return "N/A";
        }

        double average = graded.stream()
                .mapToDouble(enrollment -> enrollment.getGrade().doubleValue())
                .average()
                .orElse(0);
        return String.format(Locale.US, "%.1f%%", average);
    }

    private String formatUnits(double units) {
        return String.format(Locale.US, "%.2f", units);
    }

    private double parseUnits(String units) {
        try {
            return Double.parseDouble(units);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private record ReportsData(
            StudentDashboardSnapshot snapshot,
            List<Enrollment> recordedEnrollments
    ) {
    }
}
