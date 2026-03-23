package org.example.comp2800_sas.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.comp2800_sas.model.Student;
import org.example.comp2800_sas.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class LoginController {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @FXML private TextField studentIdField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;

    @FXML
    public void handleLogin() {
        String input = studentIdField.getText().trim();
        errorLabel.setText("");

        if (input.isEmpty()) {
            errorLabel.setText("Please enter your student ID.");
            return;
        }

        try {
            int id = Integer.parseInt(input);
            Optional<Student> student = studentRepository.findById(id);

            if (student.isPresent()) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/dashboard.fxml"));
                loader.setControllerFactory(applicationContext::getBean);
                Parent root = loader.load();

                DashboardController dashboardController = loader.getController();
                dashboardController.setStudent(student.get());

                Stage stage = (Stage) loginButton.getScene().getWindow();
                stage.setScene(new Scene(root, 1320, 820));
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
}
