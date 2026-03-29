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
import org.example.comp2800_sas.model.PlannedCourseOption;
import org.example.comp2800_sas.model.PlannerConflict;
import org.example.comp2800_sas.model.PlannerMeetingBlock;
import org.example.comp2800_sas.model.Student;
import org.example.comp2800_sas.model.StudentDashboardSession;
import org.example.comp2800_sas.model.StudentDashboardSnapshot;
import org.example.comp2800_sas.service.SemesterPlannerService;
import org.example.comp2800_sas.service.SessionService;
import org.example.comp2800_sas.service.StudentDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
@Scope("prototype")
public class HomeController {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a", Locale.US);
    private static final String SURFACE_CARD_STYLE =
            "-fx-background-color: white; -fx-background-radius: 18; -fx-border-radius: 18; " +
                    "-fx-border-color: #d9e3ef; -fx-effect: dropshadow(gaussian, rgba(19,49,77,0.08), 18, 0.18, 0, 6);";

    @Autowired private SessionService sessionService;
    @Autowired private StudentDashboardService studentDashboardService;
    @Autowired private SemesterPlannerService semesterPlannerService;

    @FXML private VBox root;

    private Runnable openEnrollmentAction = () -> {};
    private Runnable openCalendarAction = () -> {};

    public void setNavigationActions(Runnable openEnrollmentAction, Runnable openCalendarAction) {
        this.openEnrollmentAction = openEnrollmentAction == null ? () -> {} : openEnrollmentAction;
        this.openCalendarAction = openCalendarAction == null ? () -> {} : openCalendarAction;
    }

    @FXML
    public void initialize() {
        loadHomeData();
    }

    private void loadHomeData() {
        root.getChildren().clear();

        Student currentStudent = sessionService.getCurrentStudent();
        if (currentStudent == null) {
            Label empty = new Label("No active student session is available. Sign in again to open the dashboard.");
            empty.setStyle("-fx-text-fill: #607286; -fx-font-size: 13px;");
            root.getChildren().add(empty);
            return;
        }

        StackPane spinnerPane = new StackPane();
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(52, 52);
        spinnerPane.getChildren().add(spinner);
        VBox.setVgrow(spinnerPane, Priority.ALWAYS);
        root.getChildren().add(spinnerPane);

        Task<StudentDashboardSnapshot> task = new Task<>() {
            @Override
            protected StudentDashboardSnapshot call() {
                return studentDashboardService.buildSnapshot(currentStudent.getStudentId());
            }
        };

        task.setOnSucceeded(event -> {
            root.getChildren().clear();
            buildHome(task.getValue());
        });

        task.setOnFailed(event -> {
            root.getChildren().clear();
            Label error = new Label("Failed to load dashboard.");
            error.setStyle("-fx-text-fill: #b94141; -fx-font-size: 13px; -fx-font-weight: bold;");
            root.getChildren().add(error);
        });

        Thread loader = new Thread(task);
        loader.setDaemon(true);
        loader.start();
    }

    private void buildHome(StudentDashboardSnapshot snapshot) {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox content = new VBox(18);
        content.setPadding(new Insets(24));
        content.setMaxWidth(1260);

        content.getChildren().add(createHeroCard(snapshot));

        HBox statsRow = new HBox(14,
                createStatCard(String.valueOf(snapshot.totalPlannedCourseCount()), "Planned Courses", "Live selections in your calendar", "#173b63"),
                createStatCard(formatUnits(snapshot.totalUnits()), "Total Units", "Across all planned sessions", "#1c5f93"),
                createStatCard(String.valueOf(snapshot.scheduledCourseCount()), "Scheduled Courses", "Options with timed meetings", "#246344"),
                createStatCard(String.valueOf(snapshot.totalConflictCount()), "Conflicts", "Detected from your current draft", snapshot.totalConflictCount() > 0 ? "#b94141" : "#88691b")
        );
        content.getChildren().add(statsRow);

        HBox overviewRow = new HBox(18,
                createSessionSummaryCard(snapshot),
                createScheduleCard(snapshot)
        );
        overviewRow.setAlignment(Pos.TOP_LEFT);
        content.getChildren().add(overviewRow);

        HBox detailRow = new HBox(18,
                createPlannedCoursesCard(snapshot),
                createNextStepsCard(snapshot)
        );
        detailRow.setAlignment(Pos.TOP_LEFT);
        content.getChildren().add(detailRow);

        scroll.setContent(content);
        root.getChildren().add(scroll);
    }

    private VBox createHeroCard(StudentDashboardSnapshot snapshot) {
        VBox card = new VBox(14);
        card.setPadding(new Insets(22, 24, 22, 24));
        card.setStyle(SURFACE_CARD_STYLE + "-fx-background-color: linear-gradient(to right, #ffffff, #f6f9fd);");

        Label chip = createBadge("Enrollment + Calendar Hub", "-fx-background-color: #eef4ff; -fx-text-fill: #1c4a86;");

        Label title = new Label("Welcome back, " + firstName(snapshot.studentName()) + ".");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #173b63;");

        String sessionLabel = snapshot.primarySession() == null || snapshot.primarySession().isBlank()
                ? "No active session selected yet"
                : snapshot.primarySession();
        Label subtitle = new Label(
                snapshot.hasPlans()
                        ? "Your dashboard is now driven by the same catalog and planner data powering Enrollment and Calendar."
                        : "Start from Enrollment to build a draft calendar. Everything here updates from that same shared data."
        );
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #607286;");

        FlowPane sessionChips = new FlowPane(8, 8);
        if (snapshot.availableSessions().isEmpty()) {
            sessionChips.getChildren().add(createBadge("Catalog sessions unavailable", "-fx-background-color: #eef4fb; -fx-text-fill: #31506f;"));
        } else {
            snapshot.availableSessions().forEach(session ->
                    sessionChips.getChildren().add(createBadge(
                            session.equalsIgnoreCase(sessionLabel)
                                    ? session + " active"
                                    : session,
                            session.equalsIgnoreCase(sessionLabel)
                                    ? "-fx-background-color: #dcf5e5; -fx-text-fill: #246344;"
                                    : "-fx-background-color: #eef4fb; -fx-text-fill: #31506f;"
                    ))
            );
        }

        Label helper = new Label(
                snapshot.catalogSummary().openCourses() + " open catalog courses are available right now. " +
                        "Use Enrollment to add options and Calendar to resolve timing conflicts."
        );
        helper.setWrapText(true);
        helper.setStyle("-fx-font-size: 12px; -fx-text-fill: #607286;");

        VBox textBlock = new VBox(10, chip, title, subtitle, sessionChips, helper);
        HBox.setHgrow(textBlock, Priority.ALWAYS);

        VBox actionBlock = new VBox(10);
        actionBlock.setAlignment(Pos.TOP_RIGHT);

        Button enrollmentButton = createActionButton("Open Enrollment", "#f5c518", "#173b63");
        enrollmentButton.setOnAction(event -> openEnrollmentAction.run());

        Button calendarButton = createActionButton("Open Calendar", "#173b63", "white");
        calendarButton.setOnAction(event -> openCalendarAction.run());

        VBox snapshotCard = new VBox(6);
        snapshotCard.setPadding(new Insets(14, 18, 14, 18));
        snapshotCard.setStyle("-fx-background-color: #173b63; -fx-background-radius: 16;");

        Label snapshotValue = new Label(String.valueOf(snapshot.totalMeetingBlockCount()));
        snapshotValue.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #f5c518;");

        Label snapshotText = new Label("Scheduled class blocks");
        snapshotText.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #b6cee6;");

        Label snapshotSubtext = new Label(snapshot.hasPlans()
                ? sessionLabel
                : "Nothing added yet");
        snapshotSubtext.setStyle("-fx-font-size: 11px; -fx-text-fill: #d7e4f1;");

        snapshotCard.getChildren().addAll(snapshotValue, snapshotText, snapshotSubtext);
        actionBlock.getChildren().addAll(enrollmentButton, calendarButton, snapshotCard);

        HBox header = new HBox(18, textBlock, actionBlock);
        header.setAlignment(Pos.TOP_LEFT);
        card.getChildren().add(header);
        return card;
    }

    private VBox createSessionSummaryCard(StudentDashboardSnapshot snapshot) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(18, 18, 18, 18));
        card.setStyle(SURFACE_CARD_STYLE);
        HBox.setHgrow(card, Priority.ALWAYS);

        Label title = new Label("Planning by Session");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #173b63;");

        Label helper = new Label("Live totals from your current planner selections.");
        helper.setStyle("-fx-font-size: 12px; -fx-text-fill: #607286;");

        VBox rows = new VBox(10);
        if (snapshot.sessionSummaries().isEmpty()) {
            rows.getChildren().add(createEmptyMessage(
                    "No planned sessions yet. Open Enrollment to start building your draft schedule."
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

    private VBox createScheduleCard(StudentDashboardSnapshot snapshot) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(18, 18, 18, 18));
        card.setStyle(SURFACE_CARD_STYLE);
        HBox.setHgrow(card, Priority.ALWAYS);

        Label title = new Label("Weekly Schedule Snapshot");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #173b63;");

        Label helper = new Label(snapshot.primarySession() == null || snapshot.primarySession().isBlank()
                ? "Add a course option to Calendar to see weekly timing here."
                : "Scheduled meetings for " + snapshot.primarySession() + ".");
        helper.setStyle("-fx-font-size: 12px; -fx-text-fill: #607286;");

        VBox list = new VBox(8);
        if (snapshot.meetingBlocks().isEmpty()) {
            list.getChildren().add(createEmptyMessage(
                    "No timed class meetings are in your draft yet. Online or unscheduled options will appear under Next Steps."
            ));
        } else {
            snapshot.meetingBlocks().stream().limit(8).forEach(block -> list.getChildren().add(createMeetingRow(block)));
            if (snapshot.meetingBlocks().size() > 8) {
                Label more = new Label("+" + (snapshot.meetingBlocks().size() - 8) + " more scheduled blocks in your planner");
                more.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #607286;");
                list.getChildren().add(more);
            }
        }

        card.getChildren().addAll(title, helper, list);
        return card;
    }

    private VBox createPlannedCoursesCard(StudentDashboardSnapshot snapshot) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(18, 18, 18, 18));
        card.setStyle(SURFACE_CARD_STYLE);
        HBox.setHgrow(card, Priority.ALWAYS);

        HBox header = new HBox(10);
        Label title = new Label(snapshot.primarySession() == null || snapshot.primarySession().isBlank()
                ? "Courses in Your Calendar"
                : "Courses in " + snapshot.primarySession());
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #173b63;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button openCalendar = createActionButton("Manage in Calendar", "#eef4fb", "#173b63");
        openCalendar.setOnAction(event -> openCalendarAction.run());
        header.getChildren().addAll(title, spacer, openCalendar);

        Label helper = new Label("Remove or review the options currently driving your draft schedule.");
        helper.setStyle("-fx-font-size: 12px; -fx-text-fill: #607286;");

        VBox list = new VBox(10);
        if (snapshot.primarySessionOptions().isEmpty()) {
            list.getChildren().add(createEmptyMessage(
                    "No options are in this session yet. Use Enrollment to add a course into the shared planner."
            ));
        } else {
            snapshot.primarySessionOptions().stream().limit(6).forEach(option -> list.getChildren().add(createPlannedCourseCard(option)));
            if (snapshot.primarySessionOptions().size() > 6) {
                Label more = new Label("+" + (snapshot.primarySessionOptions().size() - 6) + " more planned courses in " + snapshot.primarySession());
                more.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #607286;");
                list.getChildren().add(more);
            }
        }

        card.getChildren().addAll(header, helper, list);
        return card;
    }

    private VBox createNextStepsCard(StudentDashboardSnapshot snapshot) {
        VBox card = new VBox(14);
        card.setPadding(new Insets(18, 18, 18, 18));
        card.setStyle(SURFACE_CARD_STYLE);
        HBox.setHgrow(card, Priority.ALWAYS);

        Label title = new Label("Next Steps");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #173b63;");

        VBox sections = new VBox(12);
        sections.getChildren().add(createInsightBlock(
                "Conflicts",
                snapshot.conflicts().isEmpty()
                        ? List.of("No schedule conflicts are currently detected in your draft.")
                        : snapshot.conflicts().stream()
                        .limit(4)
                        .map(this::formatConflict)
                        .toList(),
                snapshot.conflicts().isEmpty()
                        ? "#f3fbf5"
                        : "#fff1f1",
                snapshot.conflicts().isEmpty()
                        ? "#246344"
                        : "#9f3030"
        ));

        sections.getChildren().add(createInsightBlock(
                "Needs Scheduling",
                snapshot.unscheduledOptions().isEmpty()
                        ? List.of("Every planned course currently has scheduled meeting times.")
                        : snapshot.unscheduledOptions().stream()
                        .limit(4)
                        .map(option -> option.courseCode() + " " + option.courseTitle() + " is in your planner but has no timed meeting blocks yet.")
                        .toList(),
                snapshot.unscheduledOptions().isEmpty()
                        ? "#f3fbf5"
                        : "#fff8e6",
                snapshot.unscheduledOptions().isEmpty()
                        ? "#246344"
                        : "#7a5a00"
        ));

        Button enrollmentButton = createActionButton("Browse More Courses", "#173b63", "white");
        enrollmentButton.setOnAction(event -> openEnrollmentAction.run());

        card.getChildren().addAll(title, sections, enrollmentButton);
        return card;
    }

    private VBox createPlannedCourseCard(PlannedCourseOption option) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12, 14, 12, 14));
        card.setStyle("-fx-background-color: #f7fafe; -fx-background-radius: 14; -fx-border-radius: 14; -fx-border-color: #d9e3ef;");

        HBox header = new HBox(10);
        VBox textBlock = new VBox(4);
        Label code = new Label(option.courseCode());
        code.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #1c4a86;");

        Label title = new Label(option.courseTitle());
        title.setWrapText(true);
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #173b63;");
        textBlock.getChildren().addAll(code, title);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button remove = createActionButton("Remove", "#eef4fb", "#173b63");
        remove.setOnAction(event -> {
            semesterPlannerService.removePlannedOption(option.session(), option.planId());
            loadHomeData();
        });

        header.getChildren().addAll(textBlock, spacer, remove);

        FlowPane badges = new FlowPane(8, 8);
        badges.getChildren().addAll(
                createBadge(option.session(), "-fx-background-color: #eef4fb; -fx-text-fill: #31506f;"),
                createBadge("Option " + option.optionNumber(), "-fx-background-color: #eef4fb; -fx-text-fill: #31506f;"),
                createBadge(option.deliveryMode(), "-fx-background-color: #eef4fb; -fx-text-fill: #31506f;"),
                createBadge(
                        option.hasScheduledMeetings() ? "Scheduled" : "Needs timing",
                        option.hasScheduledMeetings()
                                ? "-fx-background-color: #dcf5e5; -fx-text-fill: #246344;"
                                : "-fx-background-color: #fff8e6; -fx-text-fill: #7a5a00;"
                )
        );

        Label details = new Label(
                formatUnits(parseUnits(option.units())) + " units  |  " +
                        option.sections().size() + " sections  |  " +
                        option.meetingBlocks().size() + " class blocks"
        );
        details.setStyle("-fx-font-size: 12px; -fx-text-fill: #607286;");

        card.getChildren().addAll(header, badges, details);
        return card;
    }

    private VBox createMeetingRow(PlannerMeetingBlock block) {
        VBox row = new VBox(4);
        row.setPadding(new Insets(10, 12, 10, 12));
        row.setStyle("-fx-background-color: #f7fafe; -fx-background-radius: 14; -fx-border-radius: 14; -fx-border-color: #d9e3ef;");

        HBox titleRow = new HBox(10);
        Label code = new Label(block.courseCode());
        code.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #173b63;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label day = new Label(block.dayLabel());
        day.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #31506f;");
        titleRow.getChildren().addAll(code, spacer, day);

        Label detail = new Label(
                block.startTime().format(TIME_FORMATTER) + " - " + block.endTime().format(TIME_FORMATTER) +
                        "  |  " + block.sectionType() + " " + block.sectionNumber() +
                        "  |  " + fallback(block.room(), block.deliveryMode())
        );
        detail.setWrapText(true);
        detail.setStyle("-fx-font-size: 12px; -fx-text-fill: #607286;");

        row.getChildren().addAll(titleRow, detail);
        return row;
    }

    private VBox createInsightBlock(
            String titleText,
            java.util.List<String> lines,
            String backgroundColor,
            String textColor
    ) {
        VBox block = new VBox(8);
        block.setPadding(new Insets(14, 14, 14, 14));
        block.setStyle("-fx-background-color: " + backgroundColor + "; -fx-background-radius: 14;");

        Label title = new Label(titleText);
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + textColor + ";");

        VBox rows = new VBox(6);
        for (String line : lines) {
            Label label = new Label(line);
            label.setWrapText(true);
            label.setStyle("-fx-font-size: 12px; -fx-text-fill: " + textColor + ";");
            rows.getChildren().add(label);
        }

        block.getChildren().addAll(title, rows);
        return block;
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
        Label empty = new Label(text);
        empty.setWrapText(true);
        empty.setStyle("-fx-font-size: 12px; -fx-text-fill: #607286;");
        return empty;
    }

    private String formatConflict(PlannerConflict conflict) {
        return conflict.first().courseCode() + " overlaps with " + conflict.second().courseCode() +
                " on " + conflict.dayLabel() + " during " +
                formatMinutes(conflict.startMinutes()) + " - " + formatMinutes(conflict.endMinutes()) + ".";
    }

    private String formatMinutes(int minutes) {
        int hours = minutes / 60;
        int minuteValue = minutes % 60;
        return java.time.LocalTime.of(hours, minuteValue).format(TIME_FORMATTER);
    }

    private String formatUnits(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private double parseUnits(String units) {
        try {
            return Double.parseDouble(units);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private String firstName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "student";
        }
        return fullName.trim().split("\\s+")[0];
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
