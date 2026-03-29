package org.example.comp2800_sas.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.example.comp2800_sas.controller.EnrollmentViewBuilder;
import org.example.comp2800_sas.controller.PlannerViewBuilder;
import org.example.comp2800_sas.model.Student;
import org.example.comp2800_sas.service.EnrollmentCatalogService;
import org.example.comp2800_sas.service.SemesterPlannerService;
import org.example.comp2800_sas.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
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

    public void setStudent(Student student) {
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            loader.setControllerFactory(applicationContext::getBean);
            Parent screen = loader.load();
            configureScreenNavigation(loader.getController());
            contentArea.getChildren().setAll(screen);
        } catch (Exception e) {
            e.printStackTrace();
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

    @FXML public void showHome()       { setActiveButton(btnHome);       loadScreen("/home.fxml");      }
    @FXML public void showCourses()    { setActiveButton(btnCourses);    loadScreen("/courses.fxml");   }
    @FXML public void showEnrollment() { setActiveButton(btnEnrollment); showEnrollmentScreen();          }
    @FXML public void showPlanner()    { setActiveButton(btnPlanner);    showPlannerScreen();             }
    @FXML public void showAdvisors()   { setActiveButton(btnAdvisors);   loadScreen("/faculty.fxml");   }
    @FXML public void showReports()    { setActiveButton(btnReports);    loadScreen("/reports.fxml");   }

    private void showEnrollmentScreen() {
        EnrollmentViewBuilder builder = new EnrollmentViewBuilder(
                enrollmentCatalogService,
                semesterPlannerService,
                this::showPlanner,
                () -> {}
        );
        contentArea.getChildren().setAll(builder.build(enrollmentCatalogService.loadCatalog()));
    }

    private void showPlannerScreen() {
        PlannerViewBuilder builder = new PlannerViewBuilder(
                enrollmentCatalogService,
                semesterPlannerService,
                this::showEnrollment,
                () -> {}
        );
        contentArea.getChildren().setAll(builder.build(enrollmentCatalogService.loadCatalog()));
    }

    @FXML
    public void handleLogout() {
        try {
            sessionService.clearCurrentStudent();
            semesterPlannerService.clearCurrentStudent();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
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
