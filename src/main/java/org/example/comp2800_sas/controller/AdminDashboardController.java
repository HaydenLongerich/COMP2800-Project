package org.example.comp2800_sas.controller;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.comp2800_sas.model.PlannerSelection;
import org.example.comp2800_sas.model.Section;
import org.example.comp2800_sas.model.Admin;
import org.example.comp2800_sas.model.Student;
import org.example.comp2800_sas.repository.PlannerSelectionRepository;
import org.example.comp2800_sas.repository.SectionRepository;
import org.example.comp2800_sas.service.EnrollmentCatalogService;
import org.example.comp2800_sas.service.EnrollmentService;
import org.example.comp2800_sas.service.SemesterPlannerService;
import org.example.comp2800_sas.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
// Admin shell controller.
public class AdminDashboardController {

    @Autowired private ConfigurableApplicationContext applicationContext;
    @Autowired private SessionService sessionService;
    @Autowired private EnrollmentCatalogService enrollmentCatalogService;
    @Autowired private SemesterPlannerService semesterPlannerService;
    @Autowired private EnrollmentService enrollmentService;
    @Autowired private PlannerSelectionRepository plannerSelectionRepository;
    @Autowired private SectionRepository sectionRepository;

    @FXML private Label adminNameLabel;
    @FXML private Label previewStudentLabel;
    @FXML private StackPane contentArea;
    @FXML private Button btnStudents;
    @FXML private Button btnEnrollments;
    @FXML private Button btnSections;
    @FXML private Button btnPreviewEnrollment;
    @FXML private Button btnPreviewCalendar;

    private Student previewStudent;
    private String currentView = "";

    public void setAdmin(Admin admin) {
        adminNameLabel.setText(admin.getName());
        // Reset any old preview context when a different admin session starts.
        clearPreviewStudent();
        showStudents();
    }

    private void setActiveButton(Button active) {
        for (Button button : new Button[]{
                btnStudents, btnEnrollments, btnSections,
                btnPreviewEnrollment, btnPreviewCalendar
        }) {
            button.getStyleClass().removeAll("nav-btn-active");
            if (!button.getStyleClass().contains("nav-btn")) {
                button.getStyleClass().add("nav-btn");
            }
        }
        active.getStyleClass().remove("nav-btn");
        if (!active.getStyleClass().contains("nav-btn-active")) {
            active.getStyleClass().add("nav-btn-active");
        }
    }

    private void loadScreen(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            loader.setControllerFactory(applicationContext::getBean);
            Parent screen = loader.load();
            configureScreen(loader.getController());
            showContent(screen);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private void configureScreen(Object controller) {
        if (controller instanceof AdminStudentsController studentsController) {
            studentsController.setPreviewNavigation(
                    this::showPreviewEnrollmentForStudent,
                    this::showPreviewCalendarForStudent
            );
        }
    }

    @FXML public void showStudents() {
        currentView = "students";
        setActiveButton(btnStudents);
        loadScreen("/admin_students.fxml");
    }

    @FXML public void showEnrollments() {
        currentView = "enrollments";
        setActiveButton(btnEnrollments);
        loadScreen("/admin_enrollments.fxml");
    }

    @FXML public void showSections() {
        currentView = "sections";
        setActiveButton(btnSections);
        loadScreen("/admin_sections.fxml");
    }

    @FXML
    public void showPreviewEnrollment() {
        currentView = "previewEnrollment";
        setActiveButton(btnPreviewEnrollment);
        if (!hasPreviewStudent()) {
            showContent(createPreviewPlaceholder(
                    "Pick a student to preview Enrollment.",
                    "Open the Students page and choose who you want to inspect first."
            ));
            return;
        }
        semesterPlannerService.refreshCatalogState();
        EnrollmentViewBuilder builder = new EnrollmentViewBuilder(
                enrollmentCatalogService, semesterPlannerService,
                this::showPreviewCalendar, this::handlePlannerUpdated
        );
        showContent(builder.build(enrollmentCatalogService.loadCatalog()));
    }

    @FXML
    public void showPreviewCalendar() {
        currentView = "previewCalendar";
        setActiveButton(btnPreviewCalendar);
        if (!hasPreviewStudent()) {
            showContent(createPreviewPlaceholder(
                    "Pick a student to preview Calendar.",
                    "Open the Students page and choose who you want to inspect first."
            ));
            return;
        }
        semesterPlannerService.refreshCatalogState();
        PlannerViewBuilder builder = new PlannerViewBuilder(
                enrollmentCatalogService, semesterPlannerService,
                this::showPreviewEnrollment, this::handlePlannerUpdated
        );
        showContent(builder.build(enrollmentCatalogService.loadCatalog()));
    }

    public void showPreviewEnrollmentForStudent(Student student) {
        setPreviewStudent(student);
        currentView = "previewEnrollment";
        setActiveButton(btnPreviewEnrollment);

        StackPane spinnerPane = new StackPane();
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(52, 52);
        spinnerPane.getChildren().add(spinner);
        contentArea.getChildren().setAll(spinnerPane);

        Task<Void> syncTask = new Task<>() {
            @Override
            protected Void call() {
                // Mirror planner selections into enrollments before opening the previewed Enrollment screen.
                doSyncEnrollmentsForStudent(student.getStudentId());
                return null;
            }
        };

        syncTask.setOnSucceeded(e -> {
            EnrollmentViewBuilder builder = new EnrollmentViewBuilder(
                    enrollmentCatalogService, semesterPlannerService,
                    this::showPreviewCalendar, this::handlePlannerUpdated
            );
            showContent(builder.build(enrollmentCatalogService.loadCatalog()));
        });

        syncTask.setOnFailed(e -> {
            syncTask.getException().printStackTrace();
            Label err = new Label("Failed to sync enrollments: " + syncTask.getException().getMessage());
            err.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 13px;");
            err.setWrapText(true);
            VBox errBox = new VBox(err);
            errBox.setPadding(new Insets(24));
            contentArea.getChildren().setAll(errBox);
        });

        Thread t = new Thread(syncTask);
        t.setDaemon(true);
        t.start();
    }

    private void showContent(Parent content) {
        // Match the student shell behavior so preview pages also stay pinned to the top-left.
        StackPane.setAlignment(content, Pos.TOP_LEFT);
        if (content instanceof Region region) {
            region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        }
        contentArea.getChildren().setAll(content);
    }

    public void showPreviewCalendarForStudent(Student student) {
        setPreviewStudent(student);
        showPreviewCalendar();
    }

    private void handlePlannerUpdated() {
        semesterPlannerService.refreshCatalogState();
        if ("previewCalendar".equals(currentView)) {
            showPreviewCalendar();
        }
    }

    // Strips spaces from course codes before lookup so "COMP 1047" matches "COMP1047".
    private void doSyncEnrollmentsForStudent(Integer studentId) {
        List<PlannerSelection> selections =
                plannerSelectionRepository.findByStudent_StudentIdOrderBySessionNameAscCourseCodeAsc(studentId);

        for (PlannerSelection ps : selections) {
            // KEY FIX: strip spaces so "COMP 1047" matches "COMP1047" in COURSE table
            String courseCode = ps.getCourseCode().replace(" ", "");
            List<Section> sections = sectionRepository.findByCourse_Code(courseCode);

            if (sections.isEmpty()) continue;

            Section target = sections.stream()
                    .filter(s -> {
                        try {
                            return s.getSectionNumber() == Integer.parseInt(ps.getOptionNumber());
                        } catch (NumberFormatException ex) {
                            return false;
                        }
                    })
                    .findFirst()
                    .orElse(sections.get(0));

            enrollmentService.adminEnrollStudent(studentId, target.getSectionId());
        }
    }

    private void setPreviewStudent(Student student) {
        // Preview mode reuses the same session/planner services that the student-facing screens already expect.
        previewStudent = student;
        sessionService.setCurrentStudent(student);
        semesterPlannerService.setCurrentStudent(student.getStudentId());
        updatePreviewStudentLabel();
    }

    private void clearPreviewStudent() {
        previewStudent = null;
        sessionService.clearCurrentStudent();
        semesterPlannerService.clearCurrentStudent();
        updatePreviewStudentLabel();
    }

    private boolean hasPreviewStudent() {
        return previewStudent != null;
    }

    private void updatePreviewStudentLabel() {
        if (previewStudentLabel == null) return;
        previewStudentLabel.setText(
                previewStudent == null
                        ? "Preview: select a student"
                        : "Preview: " + previewStudent.getName() + " (#" + previewStudent.getStudentId() + ")"
        );
    }

    private VBox createPreviewPlaceholder(String titleText, String detailText) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(24));
        card.setMaxWidth(560);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 14; -fx-border-radius: 14; " +
                        "-fx-border-color: #d9e3ef; -fx-effect: dropshadow(gaussian, rgba(19,49,77,0.08), 18, 0.18, 0, 6);"
        );

        Label title = new Label(titleText);
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #173b63;");

        Label detail = new Label(detailText);
        detail.setWrapText(true);
        detail.setStyle("-fx-font-size: 13px; -fx-text-fill: #607286;");

        Button openStudents = new Button("Open Students");
        openStudents.setStyle(
                "-fx-background-color: #173b63; -fx-text-fill: white; " +
                        "-fx-font-weight: bold; -fx-background-radius: 999; " +
                        "-fx-padding: 8 16; -fx-cursor: hand;"
        );
        openStudents.setOnAction(event -> showStudents());

        card.getChildren().addAll(title, detail, openStudents);

        VBox wrapper = new VBox(card);
        wrapper.setPadding(new Insets(24));
        wrapper.setAlignment(Pos.TOP_LEFT);
        return wrapper;
    }

    @FXML
    public void handleLogout() {
        try {
            clearPreviewStudent();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(new Scene(root, 560, 520));
            stage.setResizable(false);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
