package org.example.comp2800_sas.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.comp2800_sas.model.Admin;
import org.example.comp2800_sas.model.Student;
import org.example.comp2800_sas.repository.AdminRepository;
import org.example.comp2800_sas.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class LoginController {

    @Autowired private StudentRepository studentRepository;
    @Autowired private AdminRepository adminRepository;
    @Autowired private ConfigurableApplicationContext applicationContext;

    @FXML private TextField studentIdField;
    @FXML private TextField adminUsernameField;
    @FXML private PasswordField adminPasswordField;
    @FXML private Button loginButton;
    @FXML private Button adminLoginButton;
    @FXML private Button btnStudentTab;
    @FXML private Button btnAdminTab;
    @FXML private VBox studentPane;
    @FXML private VBox adminPane;
    @FXML private Label errorLabel;
    @FXML private PasswordField studentPasswordField;
    @FXML
    public void switchToStudent() {
        studentPane.setVisible(true);
        studentPane.setManaged(true);
        adminPane.setVisible(false);
        adminPane.setManaged(false);
        btnStudentTab.getStyleClass().setAll("tab-btn-active");
        btnAdminTab.getStyleClass().setAll("tab-btn");
        errorLabel.setText("");
    }

    @FXML
    public void switchToAdmin() {
        adminPane.setVisible(true);
        adminPane.setManaged(true);
        studentPane.setVisible(false);
        studentPane.setManaged(false);
        btnAdminTab.getStyleClass().setAll("tab-btn-active");
        btnStudentTab.getStyleClass().setAll("tab-btn");
        errorLabel.setText("");
    }

    @FXML
    public void handleLogin() {
        String input = studentIdField.getText().trim();
        errorLabel.setText("");

        String password = studentPasswordField.getText().trim();

        if (input.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter student ID and password.");
            return;
        }

        try {
            int id = Integer.parseInt(input);
            Optional<Student> student = studentRepository.findById(id);
//

            if (student.isPresent()) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/dashboard.fxml"));
                loader.setControllerFactory(applicationContext::getBean);
                Parent root = loader.load();

                DashboardController dashboardController = loader.getController();
                dashboardController.setStudent(student.get());

                Stage stage = (Stage) loginButton.getScene().getWindow();
                stage.setScene(new Scene(root, 1100, 700));
                stage.setResizable(true);
            } else {
                errorLabel.setText("No student found with ID " + id + ".");
            }
        } catch (NumberFormatException e) {
            errorLabel.setText("Student ID must be a number.");
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Failed to load dashboard.");
        }
    }

    @FXML
    public void handleAdminLogin() {
        String username = adminUsernameField.getText().trim();
        String password = adminPasswordField.getText().trim();
        errorLabel.setText("");

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter username and password.");
            return;
        }

        try {
            Optional<Admin> admin = adminRepository.findByUsernameAndPassword(username, password);

            if (admin.isPresent()) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/admin_dashboard.fxml"));
                loader.setControllerFactory(applicationContext::getBean);
                Parent root = loader.load();

                AdminDashboardController adminController = loader.getController();
                adminController.setAdmin(admin.get());

                Stage stage = (Stage) adminLoginButton.getScene().getWindow();
                stage.setScene(new Scene(root, 1100, 700));
                stage.setResizable(true);
            } else {
                errorLabel.setText("Invalid username or password.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Failed to load admin dashboard.");
        }
    }
}