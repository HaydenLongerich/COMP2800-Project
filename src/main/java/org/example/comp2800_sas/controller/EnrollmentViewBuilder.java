package org.example.comp2800_sas.controller;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.comp2800_sas.model.EnrollmentCatalogCourse;
import org.example.comp2800_sas.model.EnrollmentCatalogData;
import org.example.comp2800_sas.model.EnrollmentCatalogOption;
import org.example.comp2800_sas.model.EnrollmentCatalogSection;
import org.example.comp2800_sas.model.EnrollmentCatalogSummary;
import org.example.comp2800_sas.service.EnrollmentCatalogService;

import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class EnrollmentViewBuilder {

    private static final String ALL_STATUSES = "All Statuses";
    private static final String ALL_SESSIONS = "All Sessions";
    private static final String ALL_COMPONENTS = "All Components";
    private static final String SURFACE_CARD_STYLE =
            "-fx-background-color: white; -fx-background-radius: 18; -fx-border-radius: 18; " +
                    "-fx-border-color: #d9e3ef; -fx-effect: dropshadow(gaussian, rgba(19,49,77,0.08), 18, 0.18, 0, 6);";
    private static final String BADGE_BASE_STYLE =
            "-fx-background-radius: 999; -fx-padding: 5 10; -fx-font-size: 11px; -fx-font-weight: bold;";
    private static final String FILTER_FIELD_STYLE =
            "-fx-background-color: #f8fbff; -fx-background-radius: 10; -fx-border-radius: 10; " +
                    "-fx-border-color: #cad7e5; -fx-padding: 10 12; -fx-font-size: 12px;";

    private final EnrollmentCatalogService catalogService;

    public EnrollmentViewBuilder(EnrollmentCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    public VBox build(EnrollmentCatalogData catalog) {
        VBox wrapper = new VBox(18);
        wrapper.setPadding(new Insets(24));
        wrapper.setMaxWidth(1040);

        wrapper.getChildren().add(createHero());
        wrapper.getChildren().add(createSummaryBar(catalog.summary()));

        VBox filtersCard = new VBox(14);
        filtersCard.setPadding(new Insets(18, 20, 18, 20));
        filtersCard.setStyle(SURFACE_CARD_STYLE);

        Label filtersTitle = new Label("Find Courses");
        filtersTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #173b63;");

        Label filtersHint = new Label(
                "Search the uploaded catalog by course code, title, status, session, or component mix. " +
                        "Results stay on this page and sort by course code (A-Z)."
        );
        filtersHint.setWrapText(true);
        filtersHint.setStyle("-fx-text-fill: #607286; -fx-font-size: 12px;");

        TextField codeSearch = createFilterField("e.g. COMP 1400");
        TextField titleSearch = createFilterField("e.g. Algorithms");

        ComboBox<String> statusFilter = createFilterCombo(
                List.of(ALL_STATUSES, "Open", "Closed"),
                ALL_STATUSES
        );

        List<String> sessions = new ArrayList<>();
        sessions.add(ALL_SESSIONS);
        sessions.addAll(catalog.sessions());
        ComboBox<String> sessionFilter = createFilterCombo(sessions, ALL_SESSIONS);

        List<String> components = new ArrayList<>();
        components.add(ALL_COMPONENTS);
        components.addAll(catalog.componentFilters());
        ComboBox<String> componentFilter = createFilterCombo(components, ALL_COMPONENTS);

        Button clearFilters = createActionButton("Clear Filters",
                "-fx-background-color: #eef4fb; -fx-text-fill: #173b63;");
        clearFilters.setOnAction(e -> {
            codeSearch.clear();
            titleSearch.clear();
            statusFilter.setValue(ALL_STATUSES);
            sessionFilter.setValue(ALL_SESSIONS);
            componentFilter.setValue(ALL_COMPONENTS);
        });

        FlowPane filterGrid = new FlowPane(12, 12);
        filterGrid.getChildren().addAll(
                createLabeledControl("Course Code", codeSearch),
                createLabeledControl("Course Title", titleSearch),
                createLabeledControl("Status", statusFilter),
                createLabeledControl("Session", sessionFilter),
                createLabeledControl("Components", componentFilter)
        );

        HBox filterFooter = new HBox(12);
        filterFooter.setAlignment(Pos.CENTER_LEFT);

        Label resultsSummary = new Label();
        resultsSummary.setStyle("-fx-text-fill: #607286; -fx-font-size: 12px; -fx-font-weight: bold;");

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);
        filterFooter.getChildren().addAll(resultsSummary, footerSpacer, clearFilters);

        filtersCard.getChildren().addAll(filtersTitle, filtersHint, filterGrid, filterFooter);

        VBox courseList = new VBox(14);

        Runnable renderList = () -> {
            List<EnrollmentCatalogCourse> filteredCourses = catalog.courses().stream()
                    .filter(course -> matchesCodeFilter(course, codeSearch.getText()))
                    .filter(course -> matchesTitleFilter(course, titleSearch.getText()))
                    .filter(course -> matchesStatusFilter(course, statusFilter.getValue()))
                    .filter(course -> matchesSessionFilter(course, sessionFilter.getValue()))
                    .filter(course -> matchesComponentFilter(course, componentFilter.getValue()))
                    .sorted(Comparator.comparing(course -> normalize(course.courseCode())))
                    .toList();

            resultsSummary.setText(
                    "Showing " + filteredCourses.size() + " of " + catalog.summary().totalCourses() + " courses."
            );

            courseList.getChildren().clear();

            if (filteredCourses.isEmpty()) {
                courseList.getChildren().add(createEmptyState());
                return;
            }

            for (EnrollmentCatalogCourse course : filteredCourses) {
                courseList.getChildren().add(createCourseCard(course));
            }
        };

        codeSearch.textProperty().addListener((obs, oldVal, newVal) -> renderList.run());
        titleSearch.textProperty().addListener((obs, oldVal, newVal) -> renderList.run());
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> renderList.run());
        sessionFilter.valueProperty().addListener((obs, oldVal, newVal) -> renderList.run());
        componentFilter.valueProperty().addListener((obs, oldVal, newVal) -> renderList.run());

        renderList.run();

        ScrollPane scroll = new ScrollPane(courseList);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        wrapper.getChildren().addAll(filtersCard, scroll);
        return wrapper;
    }

    private VBox createHero() {
        VBox hero = new VBox(10);
        hero.setPadding(new Insets(22, 24, 22, 24));
        hero.setStyle(SURFACE_CARD_STYLE + "-fx-background-color: linear-gradient(to right, #ffffff, #f6f9fd);");

        Label phaseChip = createBadge("Phase 1: Planning Foundation",
                "-fx-background-color: #eef4ff; -fx-text-fill: #1c4a86;");

        Label title = new Label("Enrollment");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #173b63;");

        Label subtitle = new Label("Browse available Computer Science courses and section details.");
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-text-fill: #42586e; -fx-font-size: 13px;");

        Label note = new Label(
                "This first release focuses on browsing and filtering the uploaded catalog. " +
                        "Calendar actions are intentionally staged for the next phase."
        );
        note.setWrapText(true);
        note.setStyle("-fx-text-fill: #607286; -fx-font-size: 12px;");

        hero.getChildren().addAll(phaseChip, title, subtitle, note);
        return hero;
    }

    private FlowPane createSummaryBar(EnrollmentCatalogSummary summary) {
        FlowPane statBar = new FlowPane(14, 14);
        statBar.getChildren().addAll(
                createStatCard(String.valueOf(summary.totalCourses()), "Courses"),
                createStatCard(String.valueOf(summary.totalOfferings()), "Offerings"),
                createStatCard(String.valueOf(summary.totalSections()), "Sections"),
                createStatCard(String.valueOf(summary.openCourses()), "Open Courses"),
                createStatCard(String.valueOf(summary.closedCourses()), "Closed Courses")
        );
        return statBar;
    }

    private VBox createStatCard(String value, String labelText) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(16, 18, 16, 18));
        card.setMinWidth(150);
        card.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #ffffff, #f6f9fd); " +
                        "-fx-background-radius: 16; -fx-border-radius: 16; -fx-border-color: #d9e3ef;"
        );

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-text-fill: #173b63; -fx-font-size: 24px; -fx-font-weight: bold;");

        Label label = new Label(labelText);
        label.setStyle("-fx-text-fill: #607286; -fx-font-size: 11px; -fx-font-weight: bold;");

        card.getChildren().addAll(valueLabel, label);
        return card;
    }

    private VBox createLabeledControl(String labelText, Node control) {
        VBox field = new VBox(6);

        Label label = new Label(labelText);
        label.setStyle("-fx-text-fill: #607286; -fx-font-size: 11px; -fx-font-weight: bold;");

        field.getChildren().addAll(label, control);
        return field;
    }

    private TextField createFilterField(String promptText) {
        TextField field = new TextField();
        field.setPromptText(promptText);
        field.setStyle(FILTER_FIELD_STYLE);
        field.setPrefWidth(190);
        return field;
    }

    private ComboBox<String> createFilterCombo(List<String> items, String defaultValue) {
        ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList(items));
        comboBox.setValue(defaultValue);
        comboBox.setStyle(FILTER_FIELD_STYLE);
        comboBox.setPrefWidth(200);
        return comboBox;
    }

    private Button createActionButton(String text, String extraStyle) {
        Button button = new Button(text);
        button.setStyle(
                "-fx-background-radius: 999; -fx-padding: 8 16; -fx-font-size: 12px; " +
                        "-fx-font-weight: bold; -fx-cursor: hand; " + extraStyle
        );
        return button;
    }

    private Label createBadge(String text, String extraStyle) {
        Label badge = new Label(text);
        badge.setStyle(BADGE_BASE_STYLE + extraStyle);
        return badge;
    }

    private VBox createEmptyState() {
        VBox state = new VBox(6);
        state.setPadding(new Insets(24, 24, 24, 24));
        state.setStyle(SURFACE_CARD_STYLE);

        Label title = new Label("No courses match these filters.");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #173b63;");

        Label detail = new Label("Try clearing a filter or broadening the search to see more of the uploaded catalog.");
        detail.setWrapText(true);
        detail.setStyle("-fx-text-fill: #607286; -fx-font-size: 12px;");

        state.getChildren().addAll(title, detail);
        return state;
    }

    private VBox createCourseCard(EnrollmentCatalogCourse course) {
        VBox card = new VBox(16);
        card.setPadding(new Insets(18, 20, 18, 20));
        card.setStyle(SURFACE_CARD_STYLE);

        VBox primaryInfo = new VBox(8);
        primaryInfo.setFillWidth(true);

        Label code = new Label(fallback(course.courseCode(), "TBA"));
        code.setStyle("-fx-text-fill: #1c4a86; -fx-font-size: 12px; -fx-font-weight: bold;");

        Label title = new Label(fallback(course.courseName(), "Course title unavailable"));
        title.setWrapText(true);
        title.setStyle("-fx-text-fill: #173b63; -fx-font-size: 20px; -fx-font-weight: bold;");

        Label summary = new Label(buildCourseSummary(course));
        summary.setWrapText(true);
        summary.setStyle("-fx-text-fill: #607286; -fx-font-size: 12px;");

        FlowPane badges = new FlowPane(8, 8);
        badges.getChildren().add(createBadge(
                catalogService.hasOpenOffering(course) ? "Open Offerings" : "Closed Offerings",
                catalogService.hasOpenOffering(course)
                        ? "-fx-background-color: #dcf5e5; -fx-text-fill: #1a6b3d;"
                        : "-fx-background-color: #fde7e7; -fx-text-fill: #9f3030;"
        ));
        badges.getChildren().add(createBadge(
                course.optionCount() + " option" + pluralize(course.optionCount()),
                "-fx-background-color: #eef4fb; -fx-text-fill: #31506f;"
        ));
        badges.getChildren().add(createBadge(
                countSections(course) + " section" + pluralize(countSections(course)),
                "-fx-background-color: #eef4fb; -fx-text-fill: #31506f;"
        ));

        if (hasLabComponent(course)) {
            badges.getChildren().add(createBadge(
                    "Includes Lab",
                    "-fx-background-color: #fdf2d6; -fx-text-fill: #8d6200;"
            ));
        }

        for (String modality : collectCourseModalities(course)) {
            badges.getChildren().add(createBadge(modality, modalityBadgeStyle(modality)));
        }

        primaryInfo.getChildren().addAll(code, title, summary, badges);
        HBox.setHgrow(primaryInfo, Priority.ALWAYS);

        Button detailsButton = createActionButton(
                hasText(course.detailUrl()) ? "View Details" : "Details Soon",
                "-fx-background-color: #eef4fb; -fx-text-fill: #173b63;"
        );
        detailsButton.setDisable(!hasText(course.detailUrl()));
        detailsButton.setOnAction(e -> openExternalLink(course.detailUrl()));

        Button planButton = createActionButton(
                "Add to Plan",
                "-fx-background-color: #dbe6f3; -fx-text-fill: #5d7087;"
        );
        planButton.setDisable(true);
        planButton.setOpacity(1);
        planButton.setTooltip(new Tooltip("TODO: connect this action to the planner calendar in phase 2."));

        Button toggleButton = createActionButton(
                "Show Offerings",
                "-fx-background-color: #173b63; -fx-text-fill: white;"
        );

        VBox actions = new VBox(8);
        actions.setAlignment(Pos.TOP_RIGHT);
        actions.getChildren().addAll(toggleButton, detailsButton, planButton);

        HBox header = new HBox(16);
        header.setAlignment(Pos.TOP_LEFT);
        header.getChildren().addAll(primaryInfo, actions);

        FlowPane metadata = new FlowPane(10, 10);
        metadata.getChildren().addAll(
                createDetailBlock("Units", fallback(course.units(), "N/A")),
                createDetailBlock("Grading", fallback(course.grading(), "N/A")),
                createDetailBlock("Components", fallback(catalogService.formatComponents(course.components()), "N/A")),
                createDetailBlock("Career", fallback(course.courseCareer(), "N/A")),
                createDetailBlock("Class Options", String.valueOf(course.optionCount()))
        );

        Label description = new Label(fallback(course.description(), "Course description unavailable."));
        description.setWrapText(true);
        description.setStyle("-fx-text-fill: #31475d; -fx-font-size: 12px;");

        VBox offeringsBox = new VBox(12);
        offeringsBox.setVisible(false);
        offeringsBox.setManaged(false);

        Label offeringsTitle = new Label("Offerings");
        offeringsTitle.setStyle("-fx-text-fill: #173b63; -fx-font-size: 14px; -fx-font-weight: bold;");
        offeringsBox.getChildren().add(offeringsTitle);

        if (course.options().isEmpty()) {
            Label noOptions = new Label("No offering data is available for this course yet.");
            noOptions.setStyle("-fx-text-fill: #607286; -fx-font-size: 12px;");
            offeringsBox.getChildren().add(noOptions);
        } else {
            for (EnrollmentCatalogOption option : course.options()) {
                offeringsBox.getChildren().add(createOptionCard(option));
            }
        }

        toggleButton.setOnAction(e -> {
            boolean open = offeringsBox.isVisible();
            offeringsBox.setVisible(!open);
            offeringsBox.setManaged(!open);
            toggleButton.setText(open ? "Show Offerings" : "Hide Offerings");
        });

        card.getChildren().addAll(header, metadata, description, new Separator(), offeringsBox);
        return card;
    }

    private VBox createOptionCard(EnrollmentCatalogOption option) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(16, 16, 16, 16));
        card.setStyle(
                "-fx-background-color: #f7fafe; -fx-background-radius: 14; -fx-border-radius: 14; " +
                        "-fx-border-color: #d9e3ef;"
        );

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Option " + fallback(option.optionNumber(), "N/A"));
        title.setStyle("-fx-text-fill: #173b63; -fx-font-size: 14px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        FlowPane optionBadges = new FlowPane(8, 8);
        optionBadges.getChildren().add(createBadge(
                fallback(option.session(), "Session TBA"),
                "-fx-background-color: #eef4fb; -fx-text-fill: #31506f;"
        ));
        optionBadges.getChildren().add(createBadge(
                option.isOpen() ? "Open" : "Closed",
                option.isOpen()
                        ? "-fx-background-color: #dcf5e5; -fx-text-fill: #1a6b3d;"
                        : "-fx-background-color: #fde7e7; -fx-text-fill: #9f3030;"
        ));
        optionBadges.getChildren().add(createBadge(
                option.sections().size() + " section" + pluralize(option.sections().size()),
                "-fx-background-color: #eef4fb; -fx-text-fill: #31506f;"
        ));

        header.getChildren().addAll(title, spacer, optionBadges);

        VBox sections = new VBox(10);
        if (option.sections().isEmpty()) {
            Label noSections = new Label("Section details are not available for this option.");
            noSections.setStyle("-fx-text-fill: #607286; -fx-font-size: 12px;");
            sections.getChildren().add(noSections);
        } else {
            for (EnrollmentCatalogSection section : option.sections()) {
                sections.getChildren().add(createSectionCard(section));
            }
        }

        card.getChildren().addAll(header, sections);
        return card;
    }

    private VBox createSectionCard(EnrollmentCatalogSection section) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(14, 14, 14, 14));
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12; -fx-border-radius: 12; " +
                        "-fx-border-color: #e2e9f2;"
        );

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        FlowPane sectionBadges = new FlowPane(8, 8);
        sectionBadges.getChildren().add(createBadge(
                fallback(section.sectionType(), "TBA"),
                "-fx-background-color: #e8f0fb; -fx-text-fill: #173b63;"
        ));
        sectionBadges.getChildren().add(createBadge(
                "Section " + fallback(section.section(), "N/A"),
                "-fx-background-color: #eef4fb; -fx-text-fill: #31506f;"
        ));
        sectionBadges.getChildren().add(createBadge(
                "Class " + fallback(section.classNbr(), "N/A"),
                "-fx-background-color: #eef4fb; -fx-text-fill: #31506f;"
        ));
        sectionBadges.getChildren().add(createBadge(
                inferModality(section.room()),
                modalityBadgeStyle(inferModality(section.room()))
        ));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label seatSummary = new Label(buildSeatSummary(section));
        seatSummary.setStyle("-fx-text-fill: #173b63; -fx-font-size: 12px; -fx-font-weight: bold;");

        header.getChildren().addAll(sectionBadges, spacer, seatSummary);

        FlowPane detailGrid = new FlowPane(10, 10);
        detailGrid.getChildren().addAll(
                createDetailBlock("Meeting Dates", displayMeetingDates(section)),
                createDetailBlock("Days", displayDays(section)),
                createDetailBlock("Time", displayTime(section)),
                createDetailBlock("Room", displayRoom(section)),
                createDetailBlock("Instructor", displayInstructor(section)),
                createDetailBlock("Seats", displaySeatsText(section))
        );

        if (section.hasSeatCounts()) {
            detailGrid.getChildren().add(createDetailBlock("Seat Count", buildSeatCount(section)));
        }

        card.getChildren().addAll(header, detailGrid);

        if (shouldShowSectionNote(section)) {
            Label notes = new Label(cleanDisplay(section.notes()));
            notes.setWrapText(true);
            notes.setStyle(
                    "-fx-background-color: #fff8e6; -fx-background-radius: 10; -fx-padding: 10 12; " +
                            "-fx-border-color: #f5d482; -fx-border-radius: 10; " +
                            "-fx-text-fill: #7a5a00; -fx-font-size: 11px;"
            );
            card.getChildren().add(notes);
        }

        return card;
    }

    private VBox createDetailBlock(String labelText, String valueText) {
        VBox block = new VBox(4);
        block.setPadding(new Insets(10, 12, 10, 12));
        block.setPrefWidth(180);
        block.setStyle("-fx-background-color: #f7f9fc; -fx-background-radius: 10;");

        Label label = new Label(labelText);
        label.setStyle("-fx-text-fill: #6f8093; -fx-font-size: 10px; -fx-font-weight: bold;");

        Label value = new Label(fallback(valueText, "N/A"));
        value.setWrapText(true);
        value.setStyle("-fx-text-fill: #283a4d; -fx-font-size: 12px;");

        block.getChildren().addAll(label, value);
        return block;
    }

    private boolean matchesCodeFilter(EnrollmentCatalogCourse course, String query) {
        return matchesText(query, course.courseCode());
    }

    private boolean matchesTitleFilter(EnrollmentCatalogCourse course, String query) {
        return matchesText(query, course.courseName());
    }

    private boolean matchesStatusFilter(EnrollmentCatalogCourse course, String filter) {
        if (!hasText(filter) || ALL_STATUSES.equals(filter)) {
            return true;
        }

        boolean isOpen = catalogService.hasOpenOffering(course);
        return "Open".equals(filter) ? isOpen : !isOpen;
    }

    private boolean matchesSessionFilter(EnrollmentCatalogCourse course, String filter) {
        if (!hasText(filter) || ALL_SESSIONS.equals(filter)) {
            return true;
        }

        return course.options().stream()
                .map(EnrollmentCatalogOption::session)
                .map(this::cleanDisplay)
                .anyMatch(filter::equalsIgnoreCase);
    }

    private boolean matchesComponentFilter(EnrollmentCatalogCourse course, String filter) {
        if (!hasText(filter) || ALL_COMPONENTS.equals(filter)) {
            return true;
        }

        return filter.equalsIgnoreCase(catalogService.formatComponents(course.components()));
    }

    private boolean matchesText(String query, String value) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isBlank()) {
            return true;
        }
        return normalize(value).contains(normalizedQuery);
    }

    private int countSections(EnrollmentCatalogCourse course) {
        return course.options().stream()
                .mapToInt(option -> option.sections().size())
                .sum();
    }

    private boolean hasLabComponent(EnrollmentCatalogCourse course) {
        String components = normalize(course.components());
        if (components.contains("laboratory")) {
            return true;
        }

        return course.options().stream()
                .flatMap(option -> option.sections().stream())
                .map(EnrollmentCatalogSection::sectionType)
                .map(this::normalize)
                .anyMatch(type -> type.contains("lab"));
    }

    private List<String> collectCourseModalities(EnrollmentCatalogCourse course) {
        Set<String> modalities = new LinkedHashSet<>();

        for (EnrollmentCatalogOption option : course.options()) {
            for (EnrollmentCatalogSection section : option.sections()) {
                modalities.add(inferModality(section.room()));
            }
        }

        if (modalities.isEmpty()) {
            modalities.add("TBA");
        }

        return modalities.stream()
                .sorted(Comparator.comparingInt(this::modalityOrder))
                .toList();
    }

    private int modalityOrder(String modality) {
        return switch (modality) {
            case "Online" -> 0;
            case "Hybrid" -> 1;
            case "In Person" -> 2;
            default -> 3;
        };
    }

    private String modalityBadgeStyle(String modality) {
        return switch (modality) {
            case "Online" -> "-fx-background-color: #dff3ff; -fx-text-fill: #1c5f93;";
            case "Hybrid" -> "-fx-background-color: #efe8ff; -fx-text-fill: #5b48a2;";
            case "In Person" -> "-fx-background-color: #e7f5ea; -fx-text-fill: #246344;";
            default -> "-fx-background-color: #eef4fb; -fx-text-fill: #31506f;";
        };
    }

    private String inferModality(String room) {
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

    private String buildCourseSummary(EnrollmentCatalogCourse course) {
        List<String> parts = new ArrayList<>();

        if (hasText(course.units())) {
            parts.add(cleanDisplay(course.units()) + " units");
        }
        if (hasText(course.grading())) {
            parts.add(cleanDisplay(course.grading()));
        }
        if (hasText(course.courseCareer())) {
            parts.add(cleanDisplay(course.courseCareer()));
        }

        return parts.isEmpty() ? "Course details are still being prepared." : String.join("  |  ", parts);
    }

    private String displayMeetingDates(EnrollmentCatalogSection section) {
        return fallback(cleanDisplay(section.meetingDates()), "TBA");
    }

    private String displayDays(EnrollmentCatalogSection section) {
        String days = cleanDisplay(section.days());
        if (hasText(days) && !"Not Applicable".equalsIgnoreCase(days)) {
            return days;
        }

        String room = normalize(section.room());
        if (room.contains("asynchronous")) {
            return "Self-paced";
        }
        if (room.contains("online")) {
            return "Online";
        }
        return "TBA";
    }

    private String displayTime(EnrollmentCatalogSection section) {
        String time = cleanDisplay(section.time());
        if (hasText(time) && !"Not Applicable".equalsIgnoreCase(time)) {
            return time;
        }

        String room = normalize(section.room());
        if (room.contains("asynchronous")) {
            return "Asynchronous";
        }
        if (room.contains("synchronous")) {
            return "Scheduled Online";
        }
        return "TBA";
    }

    private String displayRoom(EnrollmentCatalogSection section) {
        String room = cleanDisplay(section.room());
        if (hasText(room) && !"Not Applicable".equalsIgnoreCase(room)) {
            return room;
        }
        return "Online".equals(inferModality(section.room())) ? "Online" : "TBA";
    }

    private String displayInstructor(EnrollmentCatalogSection section) {
        return fallback(cleanDisplay(section.instructor()), "TBA");
    }

    private String displaySeatsText(EnrollmentCatalogSection section) {
        String seatsText = cleanDisplay(section.seatsText());
        if (hasText(seatsText) && !"Not Applicable".equalsIgnoreCase(seatsText)) {
            return seatsText;
        }
        return section.hasSeatCounts() ? buildSeatSummary(section) : "Seat information unavailable";
    }

    private String buildSeatSummary(EnrollmentCatalogSection section) {
        if (section.seatsOpen() != null && section.seatsCapacity() != null) {
            return section.seatsOpen() + " of " + section.seatsCapacity() + " open";
        }
        if (section.seatsOpen() != null) {
            return section.seatsOpen() + " open";
        }
        if (section.seatsCapacity() != null) {
            return "Capacity " + section.seatsCapacity();
        }
        return "Seat info unavailable";
    }

    private String buildSeatCount(EnrollmentCatalogSection section) {
        if (section.seatsOpen() != null && section.seatsCapacity() != null) {
            return section.seatsOpen() + " / " + section.seatsCapacity();
        }
        if (section.seatsOpen() != null) {
            return String.valueOf(section.seatsOpen());
        }
        if (section.seatsCapacity() != null) {
            return String.valueOf(section.seatsCapacity());
        }
        return "N/A";
    }

    private boolean shouldShowSectionNote(EnrollmentCatalogSection section) {
        String normalizedNotes = normalize(section.notes());
        return hasText(section.notes())
                && !Boolean.TRUE.equals(section.manualEntryRequired())
                && !normalizedNotes.contains("placeholder added automatically");
    }

    private void openExternalLink(String url) {
        if (!hasText(url)) {
            return;
        }

        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(url));
                return;
            }
        } catch (Exception ignored) {
            // Keep the UI stable if the link cannot be opened on the current device.
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String cleanDisplay(String value) {
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

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String fallback(String value, String fallback) {
        return hasText(value) ? value : fallback;
    }

    private String pluralize(int count) {
        return count == 1 ? "" : "s";
    }
}
