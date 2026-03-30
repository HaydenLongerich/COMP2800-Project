package org.example.comp2800_sas.controller;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.example.comp2800_sas.model.EnrollmentCatalogCourse;
import org.example.comp2800_sas.model.EnrollmentCatalogData;
import org.example.comp2800_sas.model.EnrollmentCatalogOption;
import org.example.comp2800_sas.model.EnrollmentCatalogSummary;
import org.example.comp2800_sas.service.EnrollmentCatalogService;
import org.example.comp2800_sas.service.SemesterPlannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * JavaFX controller for the admin sections management screen.
 */
@Component
@Scope("prototype")
public class AdminSectionsController {

    @Autowired private EnrollmentCatalogService enrollmentCatalogService;
    @Autowired private SemesterPlannerService semesterPlannerService;

    @FXML private VBox root;

    private EnrollmentCatalogData catalogData = new EnrollmentCatalogData(
            List.of(),
            new EnrollmentCatalogSummary(0, 0, 0, 0, 0),
            List.of(),
            List.of()
    );
    private String selectedCourseCode;
    private String searchFilter = "";

    private VBox courseListBox;
    private VBox editorContainer;
    private Label feedbackLabel;

    @FXML
    public void initialize() {
        loadCatalog(null, true);
    }

    private void loadCatalog(String feedbackMessage, boolean success) {
        root.getChildren().clear();

        StackPane spinnerPane = new StackPane();
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(50, 50);
        spinnerPane.getChildren().add(spinner);
        VBox.setVgrow(spinnerPane, Priority.ALWAYS);
        root.getChildren().add(spinnerPane);

        Task<EnrollmentCatalogData> task = new Task<>() {
            @Override
            protected EnrollmentCatalogData call() {
                return enrollmentCatalogService.loadCatalog();
            }
        };

        task.setOnSucceeded(event -> {
            catalogData = task.getValue();

            if (selectedCourseCode != null && findSelectedCourse() == null) {
                selectedCourseCode = null;
            }
            if (selectedCourseCode == null && !catalogData.courses().isEmpty()) {
                selectedCourseCode = catalogData.courses().get(0).courseCode();
            }

            buildView(feedbackMessage, success);
        });

        task.setOnFailed(event -> {
            root.getChildren().clear();
            Label error = new Label("Failed to load the shared course catalog.");
            error.setStyle("-fx-text-fill: #b94141; -fx-font-size: 13px; -fx-font-weight: bold;");
            root.getChildren().add(error);
        });

        Thread loader = new Thread(task);
        loader.setDaemon(true);
        loader.start();
    }

    private void buildView(String feedbackMessage, boolean success) {
        root.getChildren().clear();

        VBox wrapper = new VBox(18);
        wrapper.setPadding(new Insets(24));
        VBox.setVgrow(wrapper, Priority.ALWAYS);

        VBox titleBlock = new VBox(4);
        Label title = new Label("Catalog Management");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1a3a5c;");

        Label subtitle = new Label(
                "Edit the shared catalog powering Enrollment, Calendar, Home, Advisors, and Reports."
        );
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #607286;");
        titleBlock.getChildren().addAll(title, subtitle);

        HBox statsRow = new HBox(12,
                createStatCard(String.valueOf(catalogData.summary().totalCourses()), "Courses", "#173b63"),
                createStatCard(String.valueOf(catalogData.summary().openCourses()), "Open Courses", "#246344"),
                createStatCard(String.valueOf(catalogData.summary().totalOfferings()), "Offerings", "#1c5f93"),
                createStatCard(String.valueOf(catalogData.summary().totalSections()), "Section Rows", "#88691b")
        );

        HBox toolsRow = new HBox(12);
        toolsRow.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField(searchFilter);
        searchField.setPromptText("Search by code, title, session, or instructor...");
        searchField.setPrefWidth(340);
        searchField.setStyle(
                "-fx-background-radius: 6; -fx-border-radius: 6; " +
                        "-fx-border-color: #cfd8dc; -fx-padding: 8 12; -fx-font-size: 13px;"
        );

        Button addCourseButton = createActionButton("Add Course", "#f5c518", "#173b63");
        addCourseButton.setOnAction(event -> {
            selectedCourseCode = null;
            updateFeedback("Creating a new shared catalog draft.", true);
            renderCourseList();
            renderEditor();
        });

        Region toolSpacer = new Region();
        HBox.setHgrow(toolSpacer, Priority.ALWAYS);
        toolsRow.getChildren().addAll(searchField, toolSpacer, addCourseButton);

        Label sessionLabel = new Label(catalogData.sessions().isEmpty()
                ? "No catalog sessions are loaded yet."
                : "Catalog sessions: " + String.join(", ", catalogData.sessions()));
        sessionLabel.setWrapText(true);
        sessionLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #607286;");

        feedbackLabel = new Label();
        feedbackLabel.setWrapText(true);
        updateFeedback(feedbackMessage, success);

        courseListBox = new VBox(10);
        ScrollPane listScroll = new ScrollPane(courseListBox);
        listScroll.setFitToWidth(true);
        listScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        listScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox listPanel = new VBox(12);
        listPanel.setPadding(new Insets(18));
        listPanel.setPrefWidth(390);
        listPanel.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16; -fx-border-radius: 16; " +
                        "-fx-border-color: #d9e3ef; -fx-effect: dropshadow(gaussian, rgba(19,49,77,0.06), 14, 0.16, 0, 4);"
        );
        VBox.setVgrow(listScroll, Priority.ALWAYS);

        Label listTitle = new Label("Catalog Courses");
        listTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #173b63;");
        Label listHelper = new Label("Select a course to edit its live catalog record.");
        listHelper.setStyle("-fx-font-size: 12px; -fx-text-fill: #607286;");
        listPanel.getChildren().addAll(listTitle, listHelper, listScroll);

        editorContainer = new VBox();
        ScrollPane editorScroll = new ScrollPane(editorContainer);
        editorScroll.setFitToWidth(true);
        editorScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        editorScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox editorPanel = new VBox(12);
        editorPanel.setPadding(new Insets(18));
        editorPanel.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16; -fx-border-radius: 16; " +
                        "-fx-border-color: #d9e3ef; -fx-effect: dropshadow(gaussian, rgba(19,49,77,0.06), 14, 0.16, 0, 4);"
        );
        VBox.setVgrow(editorScroll, Priority.ALWAYS);
        HBox.setHgrow(editorPanel, Priority.ALWAYS);

        Label editorTitle = new Label("Shared Course Record");
        editorTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #173b63;");
        Label editorHelper = new Label("Save here to immediately update the student-facing course source of truth.");
        editorHelper.setWrapText(true);
        editorHelper.setStyle("-fx-font-size: 12px; -fx-text-fill: #607286;");
        editorPanel.getChildren().addAll(editorTitle, editorHelper, editorScroll);

        HBox contentRow = new HBox(18, listPanel, editorPanel);
        contentRow.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(editorPanel, Priority.ALWAYS);
        VBox.setVgrow(contentRow, Priority.ALWAYS);

        wrapper.getChildren().addAll(titleBlock, statsRow, toolsRow, sessionLabel, feedbackLabel, contentRow);
        root.getChildren().add(wrapper);

        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            searchFilter = clean(newValue);
            renderCourseList();
        });

        renderCourseList();
        renderEditor();
    }

    private void renderCourseList() {
        if (courseListBox == null) {
            return;
        }

        courseListBox.getChildren().clear();
        List<EnrollmentCatalogCourse> filteredCourses = catalogData.courses().stream()
                .filter(this::matchesFilter)
                .sorted(Comparator.comparing(course -> normalize(course.courseCode())))
                .toList();

        if (filteredCourses.isEmpty()) {
            Label empty = new Label("No catalog courses match the current search.");
            empty.setWrapText(true);
            empty.setStyle("-fx-font-size: 12px; -fx-text-fill: #607286;");
            courseListBox.getChildren().add(empty);
            return;
        }

        for (EnrollmentCatalogCourse course : filteredCourses) {
            boolean selected = sameCode(course.courseCode(), selectedCourseCode);
            int sectionCount = course.options().stream().mapToInt(option -> option.sections().size()).sum();
            long openOfferings = course.options().stream().filter(this::hasOpenSeatSignal).count();

            VBox card = new VBox(8);
            card.setPadding(new Insets(14, 16, 14, 16));
            card.setCursor(Cursor.HAND);
            card.setStyle(
                    "-fx-background-color: " + (selected ? "#eef4ff" : "#f7fafe") + "; " +
                            "-fx-background-radius: 14; -fx-border-radius: 14; " +
                            "-fx-border-color: " + (selected ? "#7aa2d0" : "#d9e3ef") + ";"
            );
            card.setOnMouseClicked(event -> selectCourse(course.courseCode()));

            HBox header = new HBox(10);
            header.setAlignment(Pos.CENTER_LEFT);

            VBox textBlock = new VBox(4);
            Label code = new Label(clean(course.courseCode()));
            code.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #1c4a86;");

            Label name = new Label(clean(course.courseName()));
            name.setWrapText(true);
            name.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #173b63;");
            textBlock.getChildren().addAll(code, name);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label statusBadge = createBadge(
                    openOfferings > 0 ? openOfferings + " open" : "No open offerings",
                    openOfferings > 0
                            ? "-fx-background-color: #dcf5e5; -fx-text-fill: #246344;"
                            : "-fx-background-color: #fff1f1; -fx-text-fill: #9f3030;"
            );

            header.getChildren().addAll(textBlock, spacer, statusBadge);

            FlowPane metadata = new FlowPane(8, 8);
            metadata.getChildren().addAll(
                    createBadge(course.optionCount() + " offerings", "-fx-background-color: #eef4fb; -fx-text-fill: #31506f;"),
                    createBadge(sectionCount + " section rows", "-fx-background-color: #eef4fb; -fx-text-fill: #31506f;")
            );
            if (!clean(course.units()).isBlank()) {
                metadata.getChildren().add(createBadge(course.units() + " units", "-fx-background-color: #eef4fb; -fx-text-fill: #31506f;"));
            }

            String sessions = course.options().stream()
                    .map(EnrollmentCatalogOption::session)
                    .filter(Objects::nonNull)
                    .map(this::clean)
                    .filter(value -> !value.isBlank())
                    .distinct()
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("No sessions listed");

            Label detail = new Label(sessions);
            detail.setWrapText(true);
            detail.setStyle("-fx-font-size: 12px; -fx-text-fill: #607286;");

            card.getChildren().addAll(header, metadata, detail);
            courseListBox.getChildren().add(card);
        }
    }

    private void renderEditor() {
        if (editorContainer == null) {
            return;
        }

        EnrollmentCatalogCourse selectedCourse = findSelectedCourse();
        boolean editingExisting = selectedCourse != null;

        TextField codeField = createTextField(editingExisting ? selectedCourse.courseCode() : "");
        codeField.setPromptText("COMP 0000");

        TextField nameField = createTextField(editingExisting ? selectedCourse.courseName() : "");
        nameField.setPromptText("Course title");

        TextField unitsField = createTextField(editingExisting ? clean(selectedCourse.units()) : "");
        unitsField.setPromptText("3.00");

        TextField componentsField = createTextField(editingExisting ? clean(selectedCourse.components()) : "");
        componentsField.setPromptText("Lecture, Laboratory");

        TextField careerField = createTextField(editingExisting ? clean(selectedCourse.courseCareer()) : "");
        careerField.setPromptText("Undergraduate");

        TextField gradingField = createTextField(editingExisting ? clean(selectedCourse.grading()) : "");
        gradingField.setPromptText("Graded");

        TextField detailUrlField = createTextField(editingExisting ? clean(selectedCourse.detailUrl()) : "");
        detailUrlField.setPromptText("Course details URL");

        TextArea descriptionArea = createTextArea(editingExisting ? clean(selectedCourse.description()) : "", 5);
        descriptionArea.setPromptText("Catalog description");

        TextArea optionsArea = createTextArea(
                editingExisting ? enrollmentCatalogService.formatOptionsJson(selectedCourse.options()) : "[]",
                18
        );
        optionsArea.setPromptText("Use the same option and section JSON structure consumed by Enrollment and Calendar.");

        VBox editorCard = new VBox(14);
        editorCard.setPadding(new Insets(6, 2, 6, 2));

        Label formTitle = new Label(editingExisting ? "Edit Catalog Course" : "Add Catalog Course");
        formTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #173b63;");

        Label formSubtitle = new Label(
                editingExisting
                        ? "Saving here updates every student-facing page that uses the shared catalog."
                        : "Create a new course record in the shared catalog. It will be available everywhere after save."
        );
        formSubtitle.setWrapText(true);
        formSubtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #607286;");

        FlowPane badges = new FlowPane(8, 8);
        if (editingExisting) {
            badges.getChildren().addAll(
                    createBadge(selectedCourse.optionCount() + " offerings", "-fx-background-color: #eef4fb; -fx-text-fill: #31506f;"),
                    createBadge(
                            countSections(selectedCourse) + " section rows",
                            "-fx-background-color: #eef4fb; -fx-text-fill: #31506f;"
                    ),
                    createBadge(
                            enrollmentCatalogService.hasOpenOffering(selectedCourse) ? "Open in catalog" : "No open seats",
                            enrollmentCatalogService.hasOpenOffering(selectedCourse)
                                    ? "-fx-background-color: #dcf5e5; -fx-text-fill: #246344;"
                                    : "-fx-background-color: #fff1f1; -fx-text-fill: #9f3030;"
                    )
            );
        }

        HBox identityRow = new HBox(12);
        VBox codeBox = createField("Course Code", codeField);
        VBox nameBox = createField("Course Name", nameField);
        HBox.setHgrow(nameBox, Priority.ALWAYS);
        identityRow.getChildren().addAll(codeBox, nameBox);

        HBox metadataRow = new HBox(12);
        VBox unitsBox = createField("Units", unitsField);
        VBox componentsBox = createField("Components", componentsField);
        VBox careerBox = createField("Career", careerField);
        VBox gradingBox = createField("Grading", gradingField);
        HBox.setHgrow(componentsBox, Priority.ALWAYS);
        HBox.setHgrow(careerBox, Priority.ALWAYS);
        metadataRow.getChildren().addAll(unitsBox, componentsBox, careerBox, gradingBox);

        VBox urlBox = createField("Detail URL", detailUrlField);
        VBox descriptionBox = createField("Description", descriptionArea);

        VBox optionsBox = createField("Offerings JSON", optionsArea);
        Label optionsHelper = new Label(
                "Edit offerings in the exact structure used by Enrollment and Calendar. Status, sessions, sections, instructors, rooms, and seat counts all flow from this JSON."
        );
        optionsHelper.setWrapText(true);
        optionsHelper.setStyle("-fx-font-size: 12px; -fx-text-fill: #607286;");
        optionsBox.getChildren().add(optionsHelper);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button saveButton = createActionButton(editingExisting ? "Save Catalog Course" : "Create Catalog Course", "#173b63", "white");
        saveButton.setOnAction(event -> saveCourse(
                editingExisting ? selectedCourse.courseCode() : null,
                codeField.getText(),
                nameField.getText(),
                unitsField.getText(),
                componentsField.getText(),
                careerField.getText(),
                gradingField.getText(),
                detailUrlField.getText(),
                descriptionArea.getText(),
                optionsArea.getText()
        ));

        Button resetButton = createActionButton(editingExisting ? "New Blank Draft" : "Reset Draft", "#eef2ff", "#173b63");
        resetButton.setOnAction(event -> {
            selectedCourseCode = editingExisting ? null : selectedCourseCode;
            renderCourseList();
            renderEditor();
            updateFeedback(editingExisting
                    ? "Creating a new blank catalog draft."
                    : "Draft fields were reset.", true);
        });

        actions.getChildren().addAll(saveButton, resetButton);

        if (editingExisting) {
            Button deleteButton = createActionButton("Delete Course", "#fff1f1", "#9f3030");
            deleteButton.setOnAction(event -> confirmDelete(selectedCourse));
            actions.getChildren().add(deleteButton);
        }

        editorCard.getChildren().addAll(
                formTitle,
                formSubtitle,
                badges,
                identityRow,
                metadataRow,
                urlBox,
                descriptionBox,
                optionsBox,
                actions
        );

        editorContainer.getChildren().setAll(editorCard);
    }

    private void selectCourse(String courseCode) {
        selectedCourseCode = courseCode;
        updateFeedback(null, true);
        renderCourseList();
        renderEditor();
    }

    private void saveCourse(
            String originalCourseCode,
            String courseCode,
            String courseName,
            String units,
            String components,
            String courseCareer,
            String grading,
            String detailUrl,
            String description,
            String optionsJson
    ) {
        String normalizedCode = clean(courseCode).toUpperCase(Locale.ROOT);
        String normalizedName = clean(courseName);

        if (normalizedCode.isBlank() || normalizedName.isBlank()) {
            updateFeedback("Course code and course name are required.", false);
            return;
        }

        List<EnrollmentCatalogOption> parsedOptions;
        try {
            parsedOptions = enrollmentCatalogService.parseOptionsJson(optionsJson);
        } catch (IllegalArgumentException exception) {
            updateFeedback(exception.getMessage(), false);
            return;
        }

        boolean duplicateCode = catalogData.courses().stream().anyMatch(course ->
                !sameCode(course.courseCode(), originalCourseCode) && sameCode(course.courseCode(), normalizedCode)
        );
        if (duplicateCode) {
            updateFeedback("A course with code " + normalizedCode + " already exists in the shared catalog.", false);
            return;
        }

        EnrollmentCatalogCourse updatedCourse = new EnrollmentCatalogCourse(
                normalizedCode,
                normalizedName,
                parsedOptions.size(),
                blankToNull(detailUrl),
                blankToNull(description),
                blankToNull(units),
                blankToNull(grading),
                blankToNull(components),
                blankToNull(courseCareer),
                parsedOptions
        );

        List<EnrollmentCatalogCourse> updatedCourses = new ArrayList<>();
        boolean replaced = false;
        for (EnrollmentCatalogCourse course : catalogData.courses()) {
            if (sameCode(course.courseCode(), originalCourseCode)) {
                updatedCourses.add(updatedCourse);
                replaced = true;
            } else {
                updatedCourses.add(course);
            }
        }
        if (!replaced) {
            updatedCourses.add(updatedCourse);
        }

        persistCourses(
                updatedCourses,
                updatedCourse.courseCode(),
                replaced
                        ? "Catalog course saved. Student-facing pages now use the updated shared data."
                        : "Catalog course created. It is now available across the shared student flow."
        );
    }

    private void confirmDelete(EnrollmentCatalogCourse course) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Catalog Course");
        alert.setHeaderText("Remove " + clean(course.courseCode()) + " from the shared catalog?");
        alert.setContentText(
                "This removes the course from Enrollment, Calendar, Home, Advisors, and Reports. " +
                        "Existing planner selections for this course will no longer resolve after save."
        );

        if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        List<EnrollmentCatalogCourse> remainingCourses = catalogData.courses().stream()
                .filter(existing -> !sameCode(existing.courseCode(), course.courseCode()))
                .toList();

        selectedCourseCode = null;
        persistCourses(
                remainingCourses,
                null,
                "Catalog course removed. Student-facing pages now reflect the remaining shared catalog."
        );
    }

    private void persistCourses(List<EnrollmentCatalogCourse> courses, String nextSelectedCourseCode, String successMessage) {
        try {
            enrollmentCatalogService.saveCourses(courses);
            semesterPlannerService.refreshCatalogState();
            selectedCourseCode = nextSelectedCourseCode;
            loadCatalog(successMessage, true);
        } catch (Exception exception) {
            updateFeedback("Failed to save the shared catalog: " + exception.getMessage(), false);
        }
    }

    private EnrollmentCatalogCourse findSelectedCourse() {
        if (selectedCourseCode == null) {
            return null;
        }

        return catalogData.courses().stream()
                .filter(course -> sameCode(course.courseCode(), selectedCourseCode))
                .findFirst()
                .orElse(null);
    }

    private boolean matchesFilter(EnrollmentCatalogCourse course) {
        if (searchFilter.isBlank()) {
            return true;
        }

        String optionDetails = course.options().stream()
                .map(option -> clean(option.session()) + " " + clean(option.status()) + " " +
                        option.sections().stream()
                                .map(section -> clean(section.instructor()) + " " + clean(section.room()))
                                .reduce("", (left, right) -> left + " " + right))
                .reduce("", (left, right) -> left + " " + right);

        String haystack = normalize(
                clean(course.courseCode()) + " " +
                        clean(course.courseName()) + " " +
                        clean(course.components()) + " " +
                        clean(course.courseCareer()) + " " +
                        clean(course.description()) + " " +
                        optionDetails
        );
        return haystack.contains(normalize(searchFilter));
    }

    private boolean hasOpenSeatSignal(EnrollmentCatalogOption option) {
        return option.isOpen() || option.sections().stream().anyMatch(section ->
                section.seatsOpen() != null && section.seatsOpen() > 0
        );
    }

    private int countSections(EnrollmentCatalogCourse course) {
        return course.options().stream()
                .mapToInt(option -> option.sections().size())
                .sum();
    }

    private VBox createField(String labelText, Control control) {
        Label label = new Label(labelText);
        label.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #173b63;");
        VBox box = new VBox(6, label, control);
        VBox.setVgrow(control, Priority.NEVER);
        return box;
    }

    private TextField createTextField(String value) {
        TextField field = new TextField(value);
        field.setStyle(
                "-fx-background-radius: 8; -fx-border-radius: 8; " +
                        "-fx-border-color: #cfd8dc; -fx-padding: 8 12; -fx-font-size: 13px;"
        );
        return field;
    }

    private TextArea createTextArea(String value, int rows) {
        TextArea area = new TextArea(value);
        area.setPrefRowCount(rows);
        area.setWrapText(true);
        area.setStyle(
                "-fx-background-radius: 8; -fx-border-radius: 8; " +
                        "-fx-border-color: #cfd8dc; -fx-padding: 8 12; -fx-font-size: 13px;"
        );
        return area;
    }

    private VBox createStatCard(String value, String labelText, String accentColor) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14, 16, 14, 16));
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 14; -fx-border-radius: 14; " +
                        "-fx-border-color: #d9e3ef; -fx-effect: dropshadow(gaussian, rgba(19,49,77,0.05), 12, 0.14, 0, 4);"
        );
        HBox.setHgrow(card, Priority.ALWAYS);

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + accentColor + ";");

        Label label = new Label(labelText);
        label.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #173b63;");

        card.getChildren().addAll(valueLabel, label);
        return card;
    }

    private Label createBadge(String text, String extraStyle) {
        Label badge = new Label(text);
        badge.setStyle(
                "-fx-background-radius: 999; -fx-padding: 5 10; " +
                        "-fx-font-size: 11px; -fx-font-weight: bold; " +
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

    private void updateFeedback(String message, boolean success) {
        if (feedbackLabel == null) {
            return;
        }

        feedbackLabel.setText(message == null ? "" : message);
        feedbackLabel.setStyle(success
                ? "-fx-text-fill: #246344; -fx-font-size: 12px;"
                : "-fx-text-fill: #b94141; -fx-font-size: 12px;");
    }

    private boolean sameCode(String left, String right) {
        return normalize(left).equals(normalize(right));
    }

    private String blankToNull(String value) {
        String cleaned = clean(value);
        return cleaned.isBlank() ? null : cleaned;
    }

    private String normalize(String value) {
        return clean(value).toLowerCase(Locale.ROOT);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
