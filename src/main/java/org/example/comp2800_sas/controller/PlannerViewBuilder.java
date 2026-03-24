package org.example.comp2800_sas.controller;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.comp2800_sas.model.EnrollmentCatalogCourse;
import org.example.comp2800_sas.model.EnrollmentCatalogData;
import org.example.comp2800_sas.model.EnrollmentCatalogOption;
import org.example.comp2800_sas.model.PlannedCourseOption;
import org.example.comp2800_sas.model.PlannerConflict;
import org.example.comp2800_sas.model.PlannerMeetingBlock;
import org.example.comp2800_sas.model.PlannerSelectionResult;
import org.example.comp2800_sas.model.PlannerSelectionStatus;
import org.example.comp2800_sas.service.EnrollmentCatalogService;
import org.example.comp2800_sas.service.SemesterPlannerService;
import org.example.comp2800_sas.util.PlannerScheduleUtils;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PlannerViewBuilder {

    private static final String SURFACE_CARD_STYLE =
            "-fx-background-color: white; -fx-background-radius: 18; -fx-border-radius: 18; " +
                    "-fx-border-color: #d9e3ef; -fx-effect: dropshadow(gaussian, rgba(19,49,77,0.08), 18, 0.18, 0, 6);";
    private static final String BADGE_BASE_STYLE =
            "-fx-background-radius: 999; -fx-padding: 5 10; -fx-font-size: 11px; -fx-font-weight: bold;";
    private static final String FIELD_STYLE =
            "-fx-background-color: #f8fbff; -fx-background-radius: 10; -fx-border-radius: 10; " +
                    "-fx-border-color: #cad7e5; -fx-padding: 10 12; -fx-font-size: 12px;";
    private static final List<String> WEEKDAY_HEADERS = List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday");
    private static final int DAY_START_MINUTES = 8 * 60;
    private static final int DAY_END_MINUTES = 21 * 60;
    private static final int HOUR_HEIGHT = 56;
    private static final double PIXELS_PER_MINUTE = HOUR_HEIGHT / 60.0;
    private static final double TIME_COLUMN_WIDTH = 74;
    private static final double DAY_COLUMN_WIDTH = 156;
    private static final double EVENT_INSET = 6;
    private static final DateTimeFormatter TIME_LABEL_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");

    private final EnrollmentCatalogService catalogService;
    private final SemesterPlannerService plannerService;
    private final Runnable openEnrollmentPage;
    private final Runnable onPlannerUpdated;

    public PlannerViewBuilder(
            EnrollmentCatalogService catalogService,
            SemesterPlannerService plannerService,
            Runnable openEnrollmentPage,
            Runnable onPlannerUpdated
    ) {
        this.catalogService = catalogService;
        this.plannerService = plannerService;
        this.openEnrollmentPage = openEnrollmentPage;
        this.onPlannerUpdated = onPlannerUpdated;
    }

    public VBox build(EnrollmentCatalogData catalog) {
        VBox wrapper = new VBox(18);
        wrapper.setPadding(new Insets(24));
        wrapper.setMaxWidth(1260);

        Label title = new Label("Semester Calendar");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #173b63;");

        Label subtitle = new Label(
                "Build a draft weekly calendar, compare course options, and spot timing conflicts before registration."
        );
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-text-fill: #607286; -fx-font-size: 12px;");

        Button browseEnrollment = createActionButton(
                "Browse Enrollment",
                "-fx-background-color: #173b63; -fx-text-fill: white;"
        );
        browseEnrollment.setOnAction(e -> openEnrollmentPage.run());

        VBox heroText = new VBox(8, title, subtitle);
        heroText.setFillWidth(true);

        VBox hero = new VBox(12);
        hero.setPadding(new Insets(22, 24, 22, 24));
        hero.setStyle(SURFACE_CARD_STYLE + "-fx-background-color: linear-gradient(to right, #ffffff, #f6f9fd);");

        HBox heroTop = new HBox(12);
        Region heroSpacer = new Region();
        HBox.setHgrow(heroSpacer, Priority.ALWAYS);
        heroTop.getChildren().addAll(heroText, heroSpacer, browseEnrollment);

        hero.getChildren().add(heroTop);
        wrapper.getChildren().add(hero);

        List<String> sessions = new ArrayList<>(catalog.sessions());
        if (sessions.isEmpty()) {
            sessions.add("Unscheduled");
        }

        ComboBox<String> sessionSelector = new ComboBox<>(FXCollections.observableArrayList(sessions));
        sessionSelector.setStyle(FIELD_STYLE);
        sessionSelector.setValue(defaultSession(sessions));
        sessionSelector.setPrefWidth(240);

        TextField quickSearch = new TextField();
        quickSearch.setPromptText("Search calendar catalog...");
        quickSearch.setStyle(FIELD_STYLE);
        quickSearch.setPrefWidth(220);

        ComboBox<String> statusFilter = new ComboBox<>(FXCollections.observableArrayList("All Statuses", "Open", "Closed"));
        statusFilter.setStyle(FIELD_STYLE);
        statusFilter.setValue("All Statuses");
        statusFilter.setPrefWidth(150);

        ComboBox<String> componentFilter = new ComboBox<>(FXCollections.observableArrayList(buildComponentFilters(catalog)));
        componentFilter.setStyle(FIELD_STYLE);
        componentFilter.setValue("All Components");
        componentFilter.setPrefWidth(190);

        ComboBox<String> deliveryFilter = new ComboBox<>(FXCollections.observableArrayList(buildDeliveryFilters(catalog)));
        deliveryFilter.setStyle(FIELD_STYLE);
        deliveryFilter.setValue("All Delivery");
        deliveryFilter.setPrefWidth(170);

        FlowPane toolbar = new FlowPane(12, 12);
        toolbar.getChildren().addAll(
                labeledControl("Session", sessionSelector),
                labeledControl("Search", quickSearch),
                labeledControl("Status", statusFilter),
                labeledControl("Components", componentFilter),
                labeledControl("Delivery", deliveryFilter)
        );

        wrapper.getChildren().add(toolbar);

        VBox feedbackHost = new VBox();
        wrapper.getChildren().add(feedbackHost);

        String[] feedbackMessage = new String[1];
        String[] feedbackTone = new String[]{"info"};

        HBox mainSplit = new HBox(18);
        VBox.setVgrow(mainSplit, Priority.ALWAYS);

        VBox calendarColumn = new VBox(16);
        HBox.setHgrow(calendarColumn, Priority.ALWAYS);

        VBox sidebar = new VBox(16);
        sidebar.setPrefWidth(340);
        sidebar.setMinWidth(320);

        Label plannedCountValue = new Label();
        Label plannedUnitsValue = new Label();
        Label conflictValue = new Label();

        FlowPane stats = new FlowPane(14, 14);
        stats.getChildren().addAll(
                createStatCard(plannedCountValue, "Calendar Items"),
                createStatCard(plannedUnitsValue, "Total Units"),
                createStatCard(conflictValue, "Conflicts")
        );
        calendarColumn.getChildren().add(stats);

        mainSplit.getChildren().addAll(calendarColumn, sidebar);
        wrapper.getChildren().add(mainSplit);

        Runnable[] renderRef = new Runnable[1];
        renderRef[0] = () -> {
            String session = sessionSelector.getValue();
            List<PlannedCourseOption> plannedOptions = plannerService.getPlanForSession(session);
            List<PlannerConflict> conflicts = plannerService.getConflictsForSession(session);

            plannedCountValue.setText(String.valueOf(plannerService.getPlannedCountForSession(session)));
            plannedUnitsValue.setText(formatUnits(plannerService.getTotalUnitsForSession(session)));
            conflictValue.setText(String.valueOf(conflicts.size()));

            feedbackHost.getChildren().clear();
            if (hasText(feedbackMessage[0])) {
                feedbackHost.getChildren().add(createFeedbackBanner(feedbackMessage[0], feedbackTone[0]));
            }

            calendarColumn.getChildren().setAll(
                    stats,
                    createConflictCard(conflicts),
                    createCalendarCard(session, plannedOptions, conflicts)
            );
            sidebar.getChildren().setAll(
                    createPlannedCoursesCard(
                            session,
                            plannedOptions,
                            (message, tone) -> {
                                feedbackMessage[0] = message;
                                feedbackTone[0] = tone;
                            },
                            renderRef[0]
                    ),
                    createQuickAddCard(
                            catalog,
                            session,
                            quickSearch.getText(),
                            statusFilter.getValue(),
                            componentFilter.getValue(),
                            deliveryFilter.getValue(),
                            (message, tone) -> {
                                feedbackMessage[0] = message;
                                feedbackTone[0] = tone;
                            },
                            renderRef[0]
                    )
            );
        };

        sessionSelector.valueProperty().addListener((obs, oldVal, newVal) -> renderRef[0].run());
        quickSearch.textProperty().addListener((obs, oldVal, newVal) -> renderRef[0].run());
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> renderRef[0].run());
        componentFilter.valueProperty().addListener((obs, oldVal, newVal) -> renderRef[0].run());
        deliveryFilter.valueProperty().addListener((obs, oldVal, newVal) -> renderRef[0].run());

        renderRef[0].run();
        return wrapper;
    }

    private VBox createPlannedCoursesCard(
            String session,
            List<PlannedCourseOption> plannedOptions,
            java.util.function.BiConsumer<String, String> showFeedback,
            Runnable refreshView
    ) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(18, 18, 18, 18));
        card.setStyle(SURFACE_CARD_STYLE);

        Label title = new Label("Planned Courses");
        title.setStyle("-fx-text-fill: #173b63; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label helper = new Label("Manage the draft calendar for " + session + ".");
        helper.setStyle("-fx-text-fill: #607286; -fx-font-size: 12px;");
        helper.setWrapText(true);

        Button clearButton = createActionButton(
                "Clear Calendar",
                "-fx-background-color: #fde7e7; -fx-text-fill: #9f3030;"
        );
        clearButton.setMinWidth(132);
        clearButton.setPrefWidth(132);
        clearButton.setMaxWidth(Region.USE_PREF_SIZE);
        clearButton.setDisable(plannedOptions.isEmpty());
        clearButton.setOnAction(e -> {
            plannerService.clearPlan(session);
            onPlannerUpdated.run();
            showFeedback.accept("Cleared all calendar items for " + session + ".", "info");
            refreshView.run();
        });

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox titleBlock = new VBox(4, title, helper);
        HBox.setHgrow(titleBlock, Priority.ALWAYS);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(titleBlock, spacer, clearButton);

        VBox items = new VBox(10);
        if (plannedOptions.isEmpty()) {
            Label empty = new Label("No course options are added to this calendar yet. Add one from Enrollment or from Quick Add below.");
            empty.setWrapText(true);
            empty.setStyle("-fx-text-fill: #607286; -fx-font-size: 12px;");
            items.getChildren().add(empty);
        } else {
            for (PlannedCourseOption option : plannedOptions) {
                VBox item = new VBox(8);
                item.setPadding(new Insets(12, 12, 12, 12));
                item.setStyle(
                        "-fx-background-color: #f7fafe; -fx-background-radius: 14; -fx-border-radius: 14; " +
                                "-fx-border-color: #d9e3ef;"
                );

                Label code = new Label(option.courseCode());
                code.setStyle("-fx-text-fill: #1c4a86; -fx-font-size: 11px; -fx-font-weight: bold;");

                Label courseTitle = new Label(option.courseTitle());
                courseTitle.setWrapText(true);
                courseTitle.setStyle("-fx-text-fill: #173b63; -fx-font-size: 14px; -fx-font-weight: bold;");

                FlowPane badges = new FlowPane(8, 8);
                badges.getChildren().add(createBadge(
                        "Option " + option.optionNumber(),
                        "-fx-background-color: #eef4fb; -fx-text-fill: #31506f;"
                ));
                badges.getChildren().add(createBadge(
                        option.deliveryMode(),
                        deliveryBadgeStyle(option.deliveryMode())
                ));
                badges.getChildren().add(createBadge(
                        option.hasScheduledMeetings() ? "Scheduled" : "No Timed Meeting",
                        option.hasScheduledMeetings()
                                ? "-fx-background-color: #dcf5e5; -fx-text-fill: #1a6b3d;"
                                : "-fx-background-color: #eef4fb; -fx-text-fill: #31506f;"
                ));

                Label detail = new Label(buildPlannedDetail(option));
                detail.setWrapText(true);
                detail.setStyle("-fx-text-fill: #607286; -fx-font-size: 12px;");

                Button removeButton = createActionButton(
                        "Remove",
                        "-fx-background-color: #173b63; -fx-text-fill: white;"
                );
                removeButton.setMinWidth(104);
                removeButton.setOnAction(e -> {
                    boolean removed = plannerService.removePlannedOption(session, option.planId());
                    if (removed) {
                        onPlannerUpdated.run();
                        showFeedback.accept(
                                "Removed " + option.courseCode() + " option " + option.optionNumber() + " from your calendar.",
                                "info"
                        );
                    } else {
                        showFeedback.accept("That course option is no longer in your calendar.", "warning");
                    }
                    refreshView.run();
                });

                HBox actionRow = new HBox();
                Region actionSpacer = new Region();
                HBox.setHgrow(actionSpacer, Priority.ALWAYS);
                actionRow.getChildren().addAll(actionSpacer, removeButton);

                item.getChildren().addAll(code, courseTitle, badges, detail, actionRow);
                items.getChildren().add(item);
            }
        }

        card.getChildren().addAll(header, items);
        return card;
    }

    private VBox createQuickAddCard(
            EnrollmentCatalogData catalog,
            String session,
            String query,
            String statusFilter,
            String componentFilter,
            String deliveryFilter,
            java.util.function.BiConsumer<String, String> showFeedback,
            Runnable refreshView
    ) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(18, 18, 18, 18));
        card.setStyle(SURFACE_CARD_STYLE);

        Label title = new Label("Quick Add");
        title.setStyle("-fx-text-fill: #173b63; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label helper = new Label("Add an option directly to the " + session + " calendar.");
        helper.setStyle("-fx-text-fill: #607286; -fx-font-size: 12px;");
        helper.setWrapText(true);

        Button openEnrollment = createActionButton(
                "Open Enrollment",
                "-fx-background-color: #eef4fb; -fx-text-fill: #173b63;"
        );
        openEnrollment.setMinWidth(144);
        openEnrollment.setPrefWidth(144);
        openEnrollment.setMaxWidth(Region.USE_PREF_SIZE);
        openEnrollment.setOnAction(e -> openEnrollmentPage.run());

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox titleBlock = new VBox(4, title, helper);
        HBox.setHgrow(titleBlock, Priority.ALWAYS);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(titleBlock, spacer, openEnrollment);

        VBox results = new VBox(10);

        List<EnrollmentCatalogCourse> filteredCourses = catalog.courses().stream()
                .filter(course -> matchesPlannerQuery(course, query))
                .filter(course -> matchesPlannerComponent(course, componentFilter))
                .filter(course -> matchesPlannerDelivery(course, deliveryFilter))
                .filter(course -> course.options().stream().anyMatch(option ->
                        session.equalsIgnoreCase(option.session())
                                && sessionMatches(option, session)
                                && matchesPlannerStatus(option, statusFilter)
                ))
                .sorted(Comparator.comparing(course -> course.courseCode().toLowerCase(Locale.ROOT)))
                .toList();

        if (filteredCourses.isEmpty()) {
            Label empty = new Label("No course options match the current calendar filters.");
            empty.setWrapText(true);
            empty.setStyle("-fx-text-fill: #607286; -fx-font-size: 12px;");
            results.getChildren().add(empty);
        } else {
            for (EnrollmentCatalogCourse course : filteredCourses) {
                VBox item = new VBox(8);
                item.setPadding(new Insets(12, 12, 12, 12));
                item.setStyle(
                        "-fx-background-color: #f7fafe; -fx-background-radius: 14; -fx-border-radius: 14; " +
                                "-fx-border-color: #d9e3ef;"
                );

                Label code = new Label(course.courseCode());
                code.setStyle("-fx-text-fill: #1c4a86; -fx-font-size: 11px; -fx-font-weight: bold;");

                Label courseTitle = new Label(course.courseName());
                courseTitle.setWrapText(true);
                courseTitle.setStyle("-fx-text-fill: #173b63; -fx-font-size: 14px; -fx-font-weight: bold;");

                VBox optionRows = new VBox(8);
                for (EnrollmentCatalogOption option : course.options()) {
                    if (!sessionMatches(option, session) || !matchesPlannerStatus(option, statusFilter)) {
                        continue;
                    }

                    boolean planned = plannerService.isOptionPlanned(session, course.courseCode(), option.optionNumber());
                    boolean courseAlreadyPlanned = plannerService.hasCoursePlannedInSession(session, course.courseCode());
                    HBox row = new HBox(8);
                    row.setAlignment(Pos.CENTER_LEFT);

                    FlowPane badges = new FlowPane(8, 8);
                    badges.getChildren().add(createBadge(
                            "Option " + option.optionNumber(),
                            "-fx-background-color: #eef4fb; -fx-text-fill: #31506f;"
                    ));
                    badges.getChildren().add(createBadge(
                            option.status(),
                            option.isOpen()
                                    ? "-fx-background-color: #dcf5e5; -fx-text-fill: #1a6b3d;"
                                    : "-fx-background-color: #fde7e7; -fx-text-fill: #9f3030;"
                    ));

                    Region rowSpacer = new Region();
                    HBox.setHgrow(rowSpacer, Priority.ALWAYS);

                    Button addButton = createActionButton(
                            option.sections().isEmpty() ? "No Sections" : planned ? "Already Added" : courseAlreadyPlanned ? "Replace in Calendar" : "Add to Calendar",
                            planned
                                    ? "-fx-background-color: #dbe6f3; -fx-text-fill: #5d7087;"
                                    : "-fx-background-color: #173b63; -fx-text-fill: white;"
                    );
                    addButton.setDisable(option.sections().isEmpty());
                    addButton.setMinWidth(130);
                    addButton.setOnAction(e -> {
                        PlannerSelectionResult result = plannerService.addOption(course, option);
                        if (result.status() != PlannerSelectionStatus.DUPLICATE) {
                            onPlannerUpdated.run();
                        }
                        showFeedback.accept(buildAddFeedbackMessage(result), feedbackTone(result));
                        refreshView.run();
                    });

                    row.getChildren().addAll(badges, rowSpacer, addButton);
                    optionRows.getChildren().add(row);
                }

                if (!optionRows.getChildren().isEmpty()) {
                    item.getChildren().addAll(code, courseTitle, optionRows);
                    results.getChildren().add(item);
                }
            }
        }

        ScrollPane scroll = new ScrollPane(results);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPrefViewportHeight(320);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        card.getChildren().addAll(header, scroll);
        return card;
    }

    private VBox createConflictCard(List<PlannerConflict> conflicts) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16, 18, 16, 18));

        if (conflicts.isEmpty()) {
            card.setStyle(
                    "-fx-background-color: #f5fbf7; -fx-background-radius: 16; -fx-border-radius: 16; " +
                            "-fx-border-color: #d7eadc;"
            );

            Label title = new Label("No conflicts detected");
            title.setStyle("-fx-text-fill: #246344; -fx-font-size: 15px; -fx-font-weight: bold;");

            Label detail = new Label("Your current calendar items fit together in the weekly grid.");
            detail.setWrapText(true);
            detail.setStyle("-fx-text-fill: #4d6e5c; -fx-font-size: 12px;");

            card.getChildren().addAll(title, detail);
            return card;
        }

        card.setStyle(
                "-fx-background-color: #fff6f4; -fx-background-radius: 16; -fx-border-radius: 16; " +
                        "-fx-border-color: #f0c8bf;"
        );

        Label title = new Label("Calendar Conflicts");
        title.setStyle("-fx-text-fill: #9f3030; -fx-font-size: 15px; -fx-font-weight: bold;");

        Label detail = new Label("Remove or replace one of the overlapping options below.");
        detail.setWrapText(true);
        detail.setStyle("-fx-text-fill: #7f4a4a; -fx-font-size: 12px;");

        VBox rows = new VBox(6);
        conflicts.stream().limit(4).forEach(conflict -> {
            Label row = new Label(buildConflictSummary(conflict));
            row.setWrapText(true);
            row.setStyle("-fx-text-fill: #7f4a4a; -fx-font-size: 12px;");
            rows.getChildren().add(row);
        });

        if (conflicts.size() > 4) {
            Label more = new Label("+" + (conflicts.size() - 4) + " more conflicts in this session");
            more.setStyle("-fx-text-fill: #7f4a4a; -fx-font-size: 12px; -fx-font-weight: bold;");
            rows.getChildren().add(more);
        }

        card.getChildren().addAll(title, detail, rows);
        return card;
    }

    private VBox createCalendarCard(
            String session,
            List<PlannedCourseOption> plannedOptions,
            List<PlannerConflict> conflicts
    ) {
        VBox card = new VBox(14);
        card.setPadding(new Insets(18, 18, 18, 18));
        card.setStyle(SURFACE_CARD_STYLE);

        Label title = new Label("Weekly Calendar");
        title.setStyle("-fx-text-fill: #173b63; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label helper = new Label("Selected options appear in a timetable view for " + session + ".");
        helper.setStyle("-fx-text-fill: #607286; -fx-font-size: 12px;");

        VBox header = new VBox(4, title, helper);
        card.getChildren().add(header);

        int unscheduledCount = (int) plannedOptions.stream()
                .filter(option -> !option.hasScheduledMeetings())
                .count();
        if (unscheduledCount > 0) {
            Label unscheduled = new Label(
                    unscheduledCount + " calendar item" + (unscheduledCount == 1 ? "" : "s")
                            + " have no scheduled meeting blocks and remain in the added list."
            );
            unscheduled.setWrapText(true);
            unscheduled.setStyle("-fx-text-fill: #607286; -fx-font-size: 12px;");
            card.getChildren().add(unscheduled);
        }

        int totalHeight = (int) Math.round((DAY_END_MINUTES - DAY_START_MINUTES) * PIXELS_PER_MINUTE);

        HBox dayHeader = new HBox(10);
        Region timeSpacer = new Region();
        timeSpacer.setMinWidth(TIME_COLUMN_WIDTH);
        timeSpacer.setPrefWidth(TIME_COLUMN_WIDTH);
        timeSpacer.setMaxWidth(TIME_COLUMN_WIDTH);
        dayHeader.getChildren().add(timeSpacer);

        List<Pane> dayPanes = new ArrayList<>();
        for (String day : WEEKDAY_HEADERS) {
            Pane dayPane = new Pane();
            dayPane.setPrefSize(DAY_COLUMN_WIDTH, totalHeight);
            dayPane.setMinSize(DAY_COLUMN_WIDTH, totalHeight);
            dayPane.setMaxWidth(Double.MAX_VALUE);
            dayPane.setStyle(
                    "-fx-background-color: linear-gradient(to bottom, #ffffff, #fbfdff); " +
                            "-fx-background-radius: 14; -fx-border-radius: 14; -fx-border-color: #dfe7f1;"
            );
            decorateDayPane(dayPane, totalHeight);
            dayPanes.add(dayPane);

            Label dayLabel = new Label(day);
            dayLabel.setMaxWidth(Double.MAX_VALUE);
            dayLabel.setAlignment(Pos.CENTER);
            dayLabel.setStyle(
                    "-fx-background-color: #eef4fb; -fx-background-radius: 10; -fx-padding: 10 0; " +
                            "-fx-text-fill: #173b63; -fx-font-size: 12px; -fx-font-weight: bold;"
            );

            VBox headerPane = new VBox(dayLabel);
            headerPane.setAlignment(Pos.CENTER);
            HBox.setHgrow(headerPane, Priority.ALWAYS);
            headerPane.setMinWidth(DAY_COLUMN_WIDTH);
            headerPane.prefWidthProperty().bind(dayPane.widthProperty());
            dayHeader.getChildren().add(headerPane);
        }

        VBox timeColumn = new VBox();
        timeColumn.setMinWidth(TIME_COLUMN_WIDTH);
        timeColumn.setPrefWidth(TIME_COLUMN_WIDTH);
        timeColumn.setMaxWidth(TIME_COLUMN_WIDTH);
        for (int minutes = DAY_START_MINUTES; minutes < DAY_END_MINUTES; minutes += 60) {
            Label time = new Label(LocalTime.of(minutes / 60, minutes % 60).format(TIME_LABEL_FORMATTER));
            time.setStyle("-fx-text-fill: #607286; -fx-font-size: 11px; -fx-font-weight: bold;");
            VBox slot = new VBox(time);
            slot.setAlignment(Pos.TOP_RIGHT);
            slot.setPrefHeight(HOUR_HEIGHT);
            slot.setPadding(new Insets(0, 10, 0, 0));
            timeColumn.getChildren().add(slot);
        }

        HBox gridRow = new HBox(10);
        gridRow.getChildren().add(timeColumn);
        for (Pane dayPane : dayPanes) {
            HBox.setHgrow(dayPane, Priority.ALWAYS);
            gridRow.getChildren().add(dayPane);
        }

        Set<String> conflictBlockIds = new HashSet<>();
        for (PlannerConflict conflict : conflicts) {
            conflictBlockIds.add(conflict.first().blockId());
            conflictBlockIds.add(conflict.second().blockId());
        }

        for (PlannedCourseOption option : plannedOptions) {
            for (PlannerMeetingBlock block : option.meetingBlocks()) {
                if (!WEEKDAY_HEADERS.contains(block.dayLabel())) {
                    continue;
                }
                int dayIndex = WEEKDAY_HEADERS.indexOf(block.dayLabel());
                Pane dayPane = dayPanes.get(dayIndex);
                dayPane.getChildren().add(createCalendarBlock(
                        dayPane,
                        block,
                        conflictBlockIds.contains(block.blockId())
                ));
            }
        }

        ScrollPane gridScroll = new ScrollPane(gridRow);
        gridScroll.setFitToWidth(true);
        gridScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        gridScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        card.getChildren().addAll(dayHeader, gridScroll);

        if (plannedOptions.isEmpty()) {
            Label empty = new Label("No calendar items for this session yet. Use Quick Add or Enrollment to build your draft calendar.");
            empty.setWrapText(true);
            empty.setStyle("-fx-text-fill: #607286; -fx-font-size: 12px;");
            card.getChildren().add(empty);
        }

        return card;
    }

    private VBox createStatCard(Label valueLabel, String labelText) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(16, 18, 16, 18));
        card.setMinWidth(160);
        card.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #ffffff, #f6f9fd); " +
                        "-fx-background-radius: 16; -fx-border-radius: 16; -fx-border-color: #d9e3ef;"
        );

        valueLabel.setStyle("-fx-text-fill: #173b63; -fx-font-size: 24px; -fx-font-weight: bold;");

        Label label = new Label(labelText);
        label.setStyle("-fx-text-fill: #607286; -fx-font-size: 11px; -fx-font-weight: bold;");

        card.getChildren().addAll(valueLabel, label);
        return card;
    }

    private VBox labeledControl(String labelText, javafx.scene.Node control) {
        VBox field = new VBox(6);
        Label label = new Label(labelText);
        label.setStyle("-fx-text-fill: #607286; -fx-font-size: 11px; -fx-font-weight: bold;");
        field.getChildren().addAll(label, control);
        return field;
    }

    private Button createActionButton(String text, String extraStyle) {
        Button button = new Button(text);
        button.setStyle(
                "-fx-background-radius: 999; -fx-padding: 8 16; -fx-font-size: 12px; " +
                        "-fx-font-weight: bold; -fx-cursor: hand; " + extraStyle
        );
        button.setWrapText(false);
        return button;
    }

    private Label createBadge(String text, String extraStyle) {
        Label badge = new Label(text);
        badge.setStyle(BADGE_BASE_STYLE + extraStyle);
        return badge;
    }

    private String defaultSession(List<String> sessions) {
        List<String> plannedSessions = plannerService.getPlannedSessions();
        if (!plannedSessions.isEmpty() && sessions.contains(plannedSessions.get(0))) {
            return plannedSessions.get(0);
        }
        return sessions.get(0);
    }

    private List<String> buildComponentFilters(EnrollmentCatalogData catalog) {
        List<String> filters = new ArrayList<>();
        filters.add("All Components");
        filters.addAll(catalog.componentFilters());
        return filters;
    }

    private List<String> buildDeliveryFilters(EnrollmentCatalogData catalog) {
        Set<String> deliveryModes = new HashSet<>();
        for (EnrollmentCatalogCourse course : catalog.courses()) {
            for (EnrollmentCatalogOption option : course.options()) {
                deliveryModes.add(PlannerScheduleUtils.summarizeDeliveryMode(option.sections()));
            }
        }

        List<String> filters = new ArrayList<>();
        filters.add("All Delivery");
        filters.addAll(deliveryModes.stream().sorted().toList());
        return filters;
    }

    private String formatUnits(double units) {
        return String.format(Locale.US, "%.2f", units);
    }

    private void decorateDayPane(Pane dayPane, int totalHeight) {
        for (int y = 0; y <= totalHeight; y += HOUR_HEIGHT) {
            Region line = new Region();
            line.setLayoutY(y);
            line.setPrefHeight(1);
            line.prefWidthProperty().bind(dayPane.widthProperty().subtract(12));
            line.setLayoutX(6);
            line.setStyle("-fx-background-color: #edf2f7;");
            dayPane.getChildren().add(line);
        }
    }

    private VBox createCalendarBlock(Pane dayPane, PlannerMeetingBlock block, boolean conflict) {
        VBox event = new VBox(2);
        double topOffset = minutesToOffset(block.startMinutes());
        double blockHeight = Math.max(durationToPixels(block.durationMinutes()), 48);
        double maxHeight = Math.max(dayPane.getPrefHeight() - topOffset - EVENT_INSET, 48);

        event.setLayoutX(EVENT_INSET);
        event.setLayoutY(topOffset);
        event.prefWidthProperty().bind(dayPane.widthProperty().subtract(EVENT_INSET * 2));
        event.maxWidthProperty().bind(dayPane.widthProperty().subtract(EVENT_INSET * 2));
        event.setPrefHeight(Math.min(blockHeight, maxHeight));
        event.setPadding(new Insets(8, 8, 8, 8));
        event.setStyle(buildCalendarBlockStyle(block.courseCode(), conflict));

        Label code = new Label(block.courseCode());
        code.setWrapText(true);
        code.setStyle("-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");

        Label section = new Label(block.sectionType() + " " + block.sectionNumber());
        section.setWrapText(true);
        section.setStyle("-fx-text-fill: rgba(255,255,255,0.92); -fx-font-size: 10px;");

        Label time = new Label(block.startTime().format(TIME_LABEL_FORMATTER) + " - " + block.endTime().format(TIME_LABEL_FORMATTER));
        time.setWrapText(true);
        time.setStyle("-fx-text-fill: rgba(255,255,255,0.92); -fx-font-size: 10px;");

        event.getChildren().addAll(code, section, time);
        return event;
    }

    private String buildCalendarBlockStyle(String courseCode, boolean conflict) {
        if (conflict) {
            return "-fx-background-color: linear-gradient(to bottom, #d95c5c, #b94141); " +
                    "-fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #992a2a;";
        }

        return switch (Math.abs(courseCode.hashCode()) % 4) {
            case 0 -> "-fx-background-color: linear-gradient(to bottom, #173b63, #1f517e); " +
                    "-fx-background-radius: 12; -fx-border-radius: 12;";
            case 1 -> "-fx-background-color: linear-gradient(to bottom, #1c5f93, #2673aa); " +
                    "-fx-background-radius: 12; -fx-border-radius: 12;";
            case 2 -> "-fx-background-color: linear-gradient(to bottom, #315b78, #417091); " +
                    "-fx-background-radius: 12; -fx-border-radius: 12;";
            default -> "-fx-background-color: linear-gradient(to bottom, #88691b, #a88225); " +
                    "-fx-background-radius: 12; -fx-border-radius: 12;";
        };
    }

    private double minutesToOffset(int minutes) {
        return Math.max((minutes - DAY_START_MINUTES) * PIXELS_PER_MINUTE, 0);
    }

    private double durationToPixels(int durationMinutes) {
        return Math.max(durationMinutes * PIXELS_PER_MINUTE, 0);
    }

    private String deliveryBadgeStyle(String deliveryMode) {
        return switch (deliveryMode) {
            case "Online" -> "-fx-background-color: #dff3ff; -fx-text-fill: #1c5f93;";
            case "Hybrid" -> "-fx-background-color: #efe8ff; -fx-text-fill: #5b48a2;";
            case "In Person" -> "-fx-background-color: #e7f5ea; -fx-text-fill: #246344;";
            default -> "-fx-background-color: #eef4fb; -fx-text-fill: #31506f;";
        };
    }

    private String buildPlannedDetail(PlannedCourseOption option) {
        List<String> parts = new ArrayList<>();
        parts.add(option.units() + " units");
        parts.add(catalogService.formatComponents(option.components()));
        parts.add("Sections: " + option.sections().stream()
                .map(section -> section.sectionType() + " " + section.section())
                .reduce((left, right) -> left + ", " + right)
                .orElse("TBA"));
        return String.join("  |  ", parts);
    }

    private boolean matchesPlannerQuery(EnrollmentCatalogCourse course, String query) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isBlank()) {
            return true;
        }
        return normalize(course.courseCode()).contains(normalizedQuery)
                || normalize(course.courseName()).contains(normalizedQuery);
    }

    private boolean matchesPlannerComponent(EnrollmentCatalogCourse course, String componentFilter) {
        if (componentFilter == null || "All Components".equals(componentFilter)) {
            return true;
        }
        return componentFilter.equalsIgnoreCase(catalogService.formatComponents(course.components()));
    }

    private boolean matchesPlannerDelivery(EnrollmentCatalogCourse course, String deliveryFilter) {
        if (deliveryFilter == null || "All Delivery".equals(deliveryFilter)) {
            return true;
        }
        return course.options().stream().anyMatch(option ->
                deliveryFilter.equalsIgnoreCase(PlannerScheduleUtils.summarizeDeliveryMode(option.sections()))
        );
    }

    private boolean matchesPlannerStatus(EnrollmentCatalogOption option, String statusFilter) {
        if (statusFilter == null || "All Statuses".equals(statusFilter)) {
            return true;
        }
        return statusFilter.equalsIgnoreCase(option.status() == null ? "" : option.status());
    }

    private String buildConflictSummary(PlannerConflict conflict) {
        return conflict.first().courseCode() + " and " + conflict.second().courseCode()
                + " overlap on " + conflict.dayLabel()
                + " during " + formatMinutesRange(conflict.startMinutes(), conflict.endMinutes()) + ".";
    }

    private VBox createFeedbackBanner(String message, String tone) {
        VBox banner = new VBox();
        banner.setPadding(new Insets(12, 16, 12, 16));
        banner.setStyle(feedbackBannerStyle(tone));

        Label text = new Label(message);
        text.setWrapText(true);
        text.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + feedbackTextColor(tone) + ";");

        banner.getChildren().add(text);
        return banner;
    }

    private String buildAddFeedbackMessage(PlannerSelectionResult result) {
        String optionLabel = result.plannedOption().courseCode() + " option " + result.plannedOption().optionNumber();

        if (result.status() == PlannerSelectionStatus.DUPLICATE) {
            return "This option has already been added to your calendar.";
        }

        String baseMessage = result.status() == PlannerSelectionStatus.REPLACED
                ? "Updated your calendar to use " + optionLabel + "."
                : "Added " + optionLabel + " to your calendar.";

        if (!result.hasConflicts()) {
            return baseMessage;
        }

        PlannerConflict firstConflict = result.conflicts().get(0);
        PlannerMeetingBlock otherBlock = otherConflictBlock(result, firstConflict);
        String detail = " Conflict detected with " + otherBlock.courseCode()
                + " on " + firstConflict.dayLabel()
                + " during " + formatMinutesRange(firstConflict.startMinutes(), firstConflict.endMinutes()) + ".";

        Set<String> conflictingCourses = new LinkedHashSet<>();
        for (PlannerConflict conflict : result.conflicts()) {
            conflictingCourses.add(otherConflictBlock(result, conflict).courseCode());
        }
        if (conflictingCourses.size() > 1) {
            detail += " " + (conflictingCourses.size() - 1) + " more overlapping course"
                    + (conflictingCourses.size() - 1 == 1 ? "" : "s") + " remain in this calendar.";
        }

        return baseMessage + detail;
    }

    private PlannerMeetingBlock otherConflictBlock(PlannerSelectionResult result, PlannerConflict conflict) {
        return sameValue(conflict.first().planId(), result.plannedOption().planId())
                ? conflict.second()
                : conflict.first();
    }

    private String feedbackTone(PlannerSelectionResult result) {
        if (result.status() == PlannerSelectionStatus.DUPLICATE) {
            return "info";
        }
        return result.hasConflicts() ? "warning" : "success";
    }

    private String feedbackBannerStyle(String tone) {
        return switch (tone) {
            case "warning" -> "-fx-background-color: #fff6f4; -fx-background-radius: 14; " +
                    "-fx-border-radius: 14; -fx-border-color: #f0c8bf;";
            case "success" -> "-fx-background-color: #f5fbf7; -fx-background-radius: 14; " +
                    "-fx-border-radius: 14; -fx-border-color: #d7eadc;";
            default -> "-fx-background-color: #eef4fb; -fx-background-radius: 14; " +
                    "-fx-border-radius: 14; -fx-border-color: #d9e3ef;";
        };
    }

    private String feedbackTextColor(String tone) {
        return switch (tone) {
            case "warning" -> "#7f4a4a";
            case "success" -> "#246344";
            default -> "#31506f";
        };
    }

    private String formatMinutesRange(int startMinutes, int endMinutes) {
        return LocalTime.of(startMinutes / 60, startMinutes % 60).format(TIME_LABEL_FORMATTER)
                + " - "
                + LocalTime.of(endMinutes / 60, endMinutes % 60).format(TIME_LABEL_FORMATTER);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean sameValue(String left, String right) {
        return normalize(left).equals(normalize(right));
    }

    private boolean sessionMatches(EnrollmentCatalogOption option, String session) {
        return session.equalsIgnoreCase(option.session() == null ? "" : option.session());
    }
}
