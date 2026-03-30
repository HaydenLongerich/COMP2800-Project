package org.example.comp2800_sas.controller;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.example.comp2800_sas.model.EnrollmentCatalogData;
import org.example.comp2800_sas.model.Student;
import org.example.comp2800_sas.service.EnrollmentCatalogService;
import org.example.comp2800_sas.service.SemesterPlannerService;
import org.example.comp2800_sas.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
/**
 * Main student shell controller.
 * It owns the top navigation bar and swaps the large content area between
 * Home, Courses, Enrollment, Calendar, Advisors, and Reports.
 */
public class DashboardController {

    @Autowired private ConfigurableApplicationContext applicationContext;
    @Autowired private SessionService sessionService;
    @Autowired private EnrollmentCatalogService enrollmentCatalogService;
    @Autowired private SemesterPlannerService semesterPlannerService;

    @FXML private Label studentNameLabel;
    @FXML private StackPane contentArea;

    @FXML private Button btnHome;
    @FXML private Button btnCourses;
    @FXML private Button btnEnrollment;
    @FXML private Button btnPlanner;
    @FXML private Button btnAdvisors;
    @FXML private Button btnReports;

    private String currentView = "";

    public void setStudent(Student student) {
        // Both the session service and planner service need the active student before child screens load.
        sessionService.setCurrentStudent(student);
        semesterPlannerService.setCurrentStudent(student.getStudentId());
        studentNameLabel.setText(student.getName());
        showHome();
    }

    private void setActiveButton(Button active) {
        for (Button btn : new Button[]{btnHome, btnCourses, btnEnrollment, btnPlanner, btnAdvisors, btnReports}) {
            btn.getStyleClass().removeAll("nav-btn-active");
            if (!btn.getStyleClass().contains("nav-btn")) {
                btn.getStyleClass().add("nav-btn");
            }
        }
        active.getStyleClass().remove("nav-btn");
        if (!active.getStyleClass().contains("nav-btn-active")) {
            active.getStyleClass().add("nav-btn-active");
        }
    }

    private void loadScreen(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getClassLoader().getResource(fxml)
            );

            loader.setControllerFactory(applicationContext::getBean);
            Parent screen = loader.load();

            // Give child controllers shortcuts back into shared Enrollment/Calendar navigation when needed.
            configureScreenNavigation(loader.getController());

            showContent(screen);

        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to load screen: " + fxml);
        }
    }

    private void configureScreenNavigation(Object controller) {
        if (controller instanceof HomeController homeController) {
            homeController.setNavigationActions(this::showEnrollment, this::showPlanner);
        }
        if (controller instanceof FacultyController facultyController) {
            facultyController.setNavigationActions(this::showEnrollment, this::showPlanner);
        }
        if (controller instanceof ReportsController reportsController) {
            reportsController.setNavigationActions(this::showEnrollment, this::showPlanner);
        }
    }

    // ---------------- NAVIGATION ----------------

    @FXML
    public void showHome() {
        currentView = "home";
        setActiveButton(btnHome);
        loadScreen("home.fxml");
    }

    @FXML
    public void showCourses() {
        currentView = "courses";
        setActiveButton(btnCourses);
        loadScreen("courses.fxml");
    }

    @FXML
    public void showEnrollment() {
        currentView = "enrollment";
        setActiveButton(btnEnrollment);
        showEnrollmentScreen();
    }

    @FXML
    public void showPlanner() {
        currentView = "planner";
        setActiveButton(btnPlanner);
        showPlannerScreen();
    }

    @FXML
    public void showAdvisors() {
        currentView = "advisors";
        setActiveButton(btnAdvisors);
        loadScreen("faculty.fxml");
    }

    @FXML
    public void showReports() {
        currentView = "reports";
        setActiveButton(btnReports);
        loadScreen("reports.fxml");
    }

    // ---------------- ENROLLMENT ----------------

    private void showEnrollmentScreen() {
        showSpinner();

        Task<Parent> task = new Task<>() {
            @Override
            protected Parent call() {
                // Build heavy catalog-based screens off the FX thread so navigation feels responsive.
                EnrollmentCatalogData catalog = enrollmentCatalogService.loadCatalog();

                EnrollmentViewBuilder builder = new EnrollmentViewBuilder(
                        enrollmentCatalogService,
                        semesterPlannerService,
                        DashboardController.this::showPlanner,
                        () -> {}
                );

                return builder.build(catalog);
            }
        };

        task.setOnSucceeded(event -> {
            if (!"enrollment".equals(currentView)) return;
            showContent(task.getValue());
        });

        task.setOnFailed(event -> {
            task.getException().printStackTrace();
            showError("Failed to load enrollment catalog.");
        });

        new Thread(task).start();
    }

    // ---------------- PLANNER ----------------

    private void showPlannerScreen() {
        showSpinner();

        Task<Parent> task = new Task<>() {
            @Override
            protected Parent call() {
                // Calendar shares the same catalog data, so we load and build it asynchronously too.
                EnrollmentCatalogData catalog = enrollmentCatalogService.loadCatalog();

                PlannerViewBuilder builder = new PlannerViewBuilder(
                        enrollmentCatalogService,
                        semesterPlannerService,
                        DashboardController.this::showEnrollment,
                        () -> {}
                );

                return builder.build(catalog);
            }
        };

        task.setOnSucceeded(event -> {
            if (!"planner".equals(currentView)) return;
            showContent(task.getValue());
        });

        task.setOnFailed(event -> {
            task.getException().printStackTrace();
            showError("Failed to load semester calendar.");
        });

        new Thread(task).start();
    }

    // ---------------- UI HELPERS ----------------

    private void showSpinner() {
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(52, 52);

        StackPane spinnerPane = new StackPane(spinner);
        spinnerPane.setMinSize(0, 0);
        spinnerPane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        contentArea.getChildren().setAll(spinnerPane);
    }

    private void showContent(Parent content) {
        // Anchor dynamic pages to the top-left so fullscreen layouts expand naturally instead of recentering.
        StackPane.setAlignment(content, Pos.TOP_LEFT);
        if (content instanceof Region region) {
            region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        }
        contentArea.getChildren().setAll(content);
    }

    private void showError(String message) {
        Label error = new Label(message);
        error.setStyle("-fx-text-fill: #b94141; -fx-font-size: 13px; -fx-font-weight: bold;");
        contentArea.getChildren().setAll(new StackPane(error));
    }

    // ---------------- LOGOUT ----------------

    @FXML
    public void handleLogout() {
        try {
            sessionService.clearCurrentStudent();
            semesterPlannerService.clearCurrentStudent();

            FXMLLoader loader = new FXMLLoader(
                    getClass().getClassLoader().getResource("login.fxml")
            );

            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(new Scene(root, 560, 480));
            stage.setResizable(false);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
