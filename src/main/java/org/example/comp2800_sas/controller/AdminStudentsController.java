package org.example.comp2800_sas.controller;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.example.comp2800_sas.model.EnrollmentCatalogSummary;
import org.example.comp2800_sas.model.Student;
import org.example.comp2800_sas.model.StudentDashboardSnapshot;
import org.example.comp2800_sas.repository.EnrollmentRepository;
import org.example.comp2800_sas.repository.PlannerSelectionRepository;
import org.example.comp2800_sas.repository.StudentRepository;
import org.example.comp2800_sas.service.StudentDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

// JavaFX controller for the admin students management screen.
@Component
@Scope("prototype")
public class AdminStudentsController {

    @Autowired private StudentRepository studentRepository;
    @Autowired private StudentDashboardService studentDashboardService;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private PlannerSelectionRepository plannerSelectionRepository;

    @FXML private VBox root;

    private Consumer<Student> openPreviewEnrollmentAction = student -> {};
    private Consumer<Student> openPreviewCalendarAction = student -> {};

    public void setPreviewNavigation(
            Consumer<Student> openPreviewEnrollmentAction,
            Consumer<Student> openPreviewCalendarAction
    ) {
        this.openPreviewEnrollmentAction =
                openPreviewEnrollmentAction == null ? student -> {} : openPreviewEnrollmentAction;
        this.openPreviewCalendarAction =
                openPreviewCalendarAction == null ? student -> {} : openPreviewCalendarAction;
    }

    @FXML
    public void initialize() {
        loadStudents(null, true);
    }

    private void loadStudents(String feedbackMessage, boolean success) {
        root.getChildren().clear();

        StackPane spinnerPane = new StackPane();
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(50, 50);
        spinnerPane.getChildren().add(spinner);
        VBox.setVgrow(spinnerPane, Priority.ALWAYS);
        root.getChildren().add(spinnerPane);

        Task<AdminStudentData> task = new Task<>() {
            @Override
            protected AdminStudentData call() {
                List<Student> students = studentRepository.findAll().stream()
                        .sorted(Comparator.comparing(Student::getStudentId, Comparator.nullsLast(Integer::compareTo)))
                        .toList();

                Map<Integer, StudentDashboardSnapshot> snapshotsByStudentId = studentDashboardService
                        .buildSnapshots(students)
                        .stream()
                        .collect(Collectors.toMap(StudentDashboardSnapshot::studentId, snapshot -> snapshot));

                return new AdminStudentData(students, snapshotsByStudentId);
            }
        };

        task.setOnSucceeded(event -> {
            root.getChildren().clear();
            buildView(task.getValue(), feedbackMessage, success);
        });

        task.setOnFailed(event -> {
            root.getChildren().clear();
            Label error = new Label("Failed to load student planning data.");
            error.setStyle("-fx-text-fill: #b94141; -fx-font-size: 13px; -fx-font-weight: bold;");
            root.getChildren().add(error);
        });

        Thread loader = new Thread(task);
        loader.setDaemon(true);
        loader.start();
    }

    private void buildView(AdminStudentData data, String feedbackMessage, boolean success) {
        VBox wrapper = new VBox(16);
        wrapper.setPadding(new Insets(24));
        VBox.setVgrow(wrapper, Priority.ALWAYS);

        VBox titleBlock = new VBox(4);
        Label title = new Label("Student Planning Overview (" + data.students().size() + ")");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1a3a5c;");

        Label subtitle = new Label(
                "Planner counts now come from the same shared catalog and calendar selections used on Home, Enrollment, Advisors, and Reports."
        );
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #607286;");
        titleBlock.getChildren().addAll(title, subtitle);

        VBox addCard = new VBox(12);
        addCard.setPadding(new Insets(16, 20, 16, 20));
        addCard.setStyle(
                "-fx-background-color: white; -fx-background-radius: 8; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 4, 0, 0, 1);"
        );

        Label addTitle = new Label("Add New Student");
        addTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1a3a5c;");

        HBox formRow = new HBox(12);
        formRow.setAlignment(Pos.CENTER_LEFT);

        TextField nameField = new TextField();
        TextField passwordField = new TextField();
        passwordField.setPromptText("Enter password...");
        passwordField.setPrefWidth(180);
        passwordField.setStyle(
                "-fx-background-radius: 6; -fx-border-radius: 6; " +
                        "-fx-border-color: #cfd8dc; -fx-padding: 8 12; -fx-font-size: 13px;"
        );

        nameField.setPromptText("Enter full name...");
        nameField.setPrefWidth(280);
        nameField.setStyle(
                "-fx-background-radius: 6; -fx-border-radius: 6; " +
                        "-fx-border-color: #cfd8dc; -fx-padding: 8 12; -fx-font-size: 13px;"
        );

        Button addBtn = new Button("Add Student");
        addBtn.setStyle(
                "-fx-background-color: #f5c518; -fx-text-fill: #1a3a5c; -fx-font-weight: bold; " +
                        "-fx-background-radius: 6; -fx-padding: 8 16; -fx-cursor: hand;"
        );

        Label feedbackLabel = new Label(feedbackMessage == null ? "" : feedbackMessage);
        feedbackLabel.setWrapText(true);
        feedbackLabel.setStyle(success
                ? "-fx-text-fill: #246344; -fx-font-size: 12px;"
                : "-fx-text-fill: #b94141; -fx-font-size: 12px;");

        addBtn.setOnAction(event -> {
            String name = clean(nameField.getText());
            String password = clean(passwordField.getText());

            if (name.isBlank()) {
                feedbackLabel.setText("Please enter a student name before saving.");
                feedbackLabel.setStyle("-fx-text-fill: #b94141; -fx-font-size: 12px;");
                return;
            }

            if (password.isBlank()) {
                feedbackLabel.setText("Please enter a password before saving.");
                feedbackLabel.setStyle("-fx-text-fill: #b94141; -fx-font-size: 12px;");
                return;
            }

            try {
                studentRepository.save(new Student(name));
                nameField.clear();
                passwordField.clear();
                loadStudents("Student '" + name + "' was added to the shared student roster.", true);
            } catch (Exception exception) {
                feedbackLabel.setText("Failed to add the student.");
                feedbackLabel.setStyle("-fx-text-fill: #b94141; -fx-font-size: 12px;");
            }
        });

        formRow.getChildren().addAll(nameField, passwordField, addBtn, feedbackLabel);
        addCard.getChildren().addAll(addTitle, formRow);

        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        TextField search = new TextField();
        search.setPromptText("Search by student, ID, or session...");
        search.setPrefWidth(300);
        search.setStyle(
                "-fx-background-radius: 6; -fx-border-radius: 6; " +
                        "-fx-border-color: #cfd8dc; -fx-padding: 8 12; -fx-font-size: 13px;"
        );

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        headerRow.getChildren().addAll(titleBlock, headerSpacer, search);

        VBox card = new VBox(0);
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 8; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 4, 0, 0, 1);"
        );

        HBox colHeader = new HBox(16);
        colHeader.setPadding(new Insets(10, 20, 10, 20));
        colHeader.setStyle("-fx-background-color: #eef2ff; -fx-background-radius: 8 8 0 0;");
        colHeader.getChildren().addAll(
                col("ID", 70),
                col("NAME", 180),
                col("PASSWORD", 140),
                col("PLANNED", 110),
                col("UNITS", 90),
                col("SESSIONS", 90),
                col("CONFLICTS", 90),
                col("ENROLLMENT", 130),
                col("CALENDAR", 130),
                col("REMOVE", 110)
        );
        card.getChildren().add(colHeader);

        VBox rowsBox = new VBox(0);

        Runnable renderRows = () -> {
            rowsBox.getChildren().clear();
            String filter = normalize(search.getText());
            int rowIndex = 0;

            for (Student student : data.students()) {
                StudentDashboardSnapshot snapshot = data.snapshotsByStudentId()
                        .getOrDefault(student.getStudentId(), emptySnapshot(student));

                if (!matchesFilter(student, snapshot, filter)) {
                    continue;
                }

                HBox row = new HBox(16);
                row.setPadding(new Insets(12, 20, 12, 20));
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle(
                        "-fx-background-color: " + (rowIndex % 2 == 0 ? "white" : "#fafbff") + "; " +
                                "-fx-border-color: transparent transparent #f0f0f0 transparent;"
                );

                Label idLabel       = value(String.valueOf(student.getStudentId()), 70, false, "#888");
                Label nameLabel     = value(student.getName(), 180, true, "#1a3a5c");
                Label passwordLabel = value(generateFPassword(), 140, false, "#888");
                Label plannedLabel  = value(snapshot.totalPlannedCourseCount() + " course" +
                        (snapshot.totalPlannedCourseCount() == 1 ? "" : "s"), 110, false, "#246344");
                Label unitsLabel     = value(formatUnits(snapshot.totalUnits()), 90, false, "#31506f");
                Label sessionsLabel  = value(String.valueOf(snapshot.sessionCount()), 90, false, "#31506f");
                Label conflictsLabel = value(
                        String.valueOf(snapshot.totalConflictCount()),
                        90,
                        true,
                        snapshot.totalConflictCount() > 0 ? "#b94141" : "#246344"
                );

                Button enrollmentButton = createActionButton("Open Enrollment", "#eef2ff", "#1a3a5c");
                enrollmentButton.setPrefWidth(130);
                enrollmentButton.setOnAction(event -> openPreviewEnrollmentAction.accept(student));

                Button calendarButton = createActionButton("Open Calendar", "#1a3a5c", "white");
                calendarButton.setPrefWidth(130);
                calendarButton.setOnAction(event -> openPreviewCalendarAction.accept(student));

                Button removeButton = createActionButton("Remove", "#b94141", "white");
                removeButton.setPrefWidth(110);

                removeButton.setOnAction(event -> {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                            javafx.scene.control.Alert.AlertType.CONFIRMATION
                    );
                    alert.setTitle("Confirm Removal");
                    alert.setHeaderText("Remove Student");
                    alert.setContentText("Are you sure you want to remove '" + student.getName() + "'?");

                    alert.showAndWait().ifPresent(response -> {
                        if (response == javafx.scene.control.ButtonType.OK) {

                            Task<Void> deleteTask = new Task<>() {
                                @Override
                                protected Void call() {
                                    // 1. Delete planner selections first
                                    plannerSelectionRepository.deleteByStudent_StudentId(student.getStudentId());

                                    // 2. Delete enrollments via JPQL @Query (avoids FK constraint)
                                    enrollmentRepository.deleteByStudentId(student.getStudentId());

                                    // 3. Safe to delete the student row now
                                    studentRepository.deleteById(student.getStudentId());
                                    return null;
                                }
                            };

                            deleteTask.setOnSucceeded(e ->
                                    loadStudents("Student '" + student.getName() + "' was removed.", true)
                            );

                            deleteTask.setOnFailed(e -> {
                                Throwable ex = deleteTask.getException();
                                ex.printStackTrace();
                                loadStudents("Failed to remove student: " + ex.getMessage(), false);
                            });

                            Thread t = new Thread(deleteTask);
                            t.setDaemon(true);
                            t.start();
                        }
                    });
                });

                row.getChildren().addAll(
                        idLabel,
                        nameLabel,
                        passwordLabel,
                        plannedLabel,
                        unitsLabel,
                        sessionsLabel,
                        conflictsLabel,
                        enrollmentButton,
                        calendarButton,
                        removeButton
                );
                rowsBox.getChildren().add(row);
                rowIndex++;
            }

            if (rowsBox.getChildren().isEmpty()) {
                HBox emptyRow = new HBox();
                emptyRow.setPadding(new Insets(16, 20, 16, 20));
                Label empty = new Label("No students match the current filter.");
                empty.setStyle("-fx-text-fill: #607286; -fx-font-size: 12px;");
                emptyRow.getChildren().add(empty);
                rowsBox.getChildren().add(emptyRow);
            }
        };

        renderRows.run();
        search.textProperty().addListener((obs, oldValue, newValue) -> renderRows.run());

        card.getChildren().add(rowsBox);

        ScrollPane scroll = new ScrollPane(card);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        wrapper.getChildren().addAll(headerRow, addCard, scroll);
        root.getChildren().add(wrapper);
    }

    private StudentDashboardSnapshot emptySnapshot(Student student) {
        return new StudentDashboardSnapshot(
                student.getStudentId(),
                student.getName(),
                new EnrollmentCatalogSummary(0, 0, 0, 0, 0),
                List.of(),
                "",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                0,
                0
        );
    }

    private String generateFPassword() {
        int length = 6 + (int)(Math.random() * 5);
        return "•".repeat(length);
    }

    private boolean matchesFilter(Student student, StudentDashboardSnapshot snapshot, String filter) {
        if (filter.isBlank()) {
            return true;
        }
        String haystack = normalize(
                student.getStudentId() + " " +
                        clean(student.getName()) + " " +
                        clean(snapshot.primarySession())
        );
        return haystack.contains(filter);
    }

    private Label col(String text, double width) {
        Label label = new Label(text);
        label.setPrefWidth(width);
        label.setStyle("-fx-text-fill: #1a3a5c; -fx-font-size: 11px; -fx-font-weight: bold;");
        return label;
    }

    private Label value(String text, double width, boolean bold, String color) {
        Label label = new Label(text);
        label.setPrefWidth(width);
        label.setStyle(
                "-fx-text-fill: " + color + "; -fx-font-size: 13px; " +
                        (bold ? "-fx-font-weight: bold;" : "")
        );
        return label;
    }

    private Button createActionButton(String text, String backgroundColor, String textColor) {
        Button button = new Button(text);
        button.setStyle(
                "-fx-background-color: " + backgroundColor + "; " +
                        "-fx-text-fill: " + textColor + "; " +
                        "-fx-font-weight: bold; -fx-background-radius: 999; " +
                        "-fx-padding: 8 12; -fx-cursor: hand;"
        );
        return button;
    }

    private String formatUnits(double units) {
        return String.format(Locale.US, "%.2f", units);
    }

    private String normalize(String value) {
        return clean(value).toLowerCase(Locale.ROOT);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private record AdminStudentData(
            List<Student> students,
            Map<Integer, StudentDashboardSnapshot> snapshotsByStudentId
    ) {}
}
