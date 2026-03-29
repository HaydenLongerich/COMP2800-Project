package org.example.comp2800_sas.controller;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.example.comp2800_sas.model.Enrollment;
import org.example.comp2800_sas.model.EnrollmentStatus;
import org.example.comp2800_sas.model.Student;
import org.example.comp2800_sas.repository.StudentRepository;
import org.example.comp2800_sas.service.EnrollmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@Scope("prototype")
public class AdminEnrollmentsController {

    @Autowired private StudentRepository studentRepository;
    @Autowired private EnrollmentService enrollmentService;

    @FXML private VBox root;

    @FXML
    public void initialize() {
        StackPane spinnerPane = new StackPane();
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(50, 50);
        spinnerPane.getChildren().add(spinner);
        VBox.setVgrow(spinnerPane, Priority.ALWAYS);
        root.getChildren().add(spinnerPane);

        Task<List<Student>> task = new Task<>() {
            @Override
            protected List<Student> call() {
                return studentRepository.findAll();
            }
        };

        task.setOnSucceeded(e -> {
            root.getChildren().clear();
            buildView(task.getValue());
        });

        task.setOnFailed(e -> {
            root.getChildren().clear();
            Label error = new Label("Failed to load enrollments.");
            error.setStyle("-fx-text-fill: red;");
            root.getChildren().add(error);
        });

        Thread loader = new Thread(task);
        loader.setDaemon(true);
        loader.start();
    }

    private void buildView(List<Student> students) {
        VBox wrapper = new VBox(16);
        wrapper.setPadding(new Insets(24));
        VBox.setVgrow(wrapper, Priority.ALWAYS);

        Label title = new Label("Recorded Enrollment History");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1a3a5c;");

        Label subtitle = new Label(
                "This page manages stored database enrollment outcomes and grades. " +
                        "Live planner selections are now handled through the shared Enrollment and Calendar flow."
        );
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #607286;");

        TextField search = new TextField();
        search.setPromptText("Search recorded student or course history...");
        search.setPrefWidth(300);
        search.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; " +
                "-fx-border-color: #cfd8dc; -fx-padding: 8 12; -fx-font-size: 13px;");

        VBox card = new VBox(0);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 4, 0, 0, 1);");

        HBox colHeader = new HBox(16);
        colHeader.setPadding(new Insets(10, 20, 10, 20));
        colHeader.setStyle("-fx-background-color: #eef2ff; -fx-background-radius: 8 8 0 0;");
        colHeader.getChildren().addAll(
                col("STUDENT", 160), col("COURSE", 100),
                col("TITLE", 220), col("SECTION", 70),
                col("STATUS", 90), col("GRADE", 70),
                col("SET GRADE", 160)
        );
        card.getChildren().add(colHeader);

        VBox rowsBox = new VBox(0);

        Runnable renderRows = () -> {
            rowsBox.getChildren().clear();
            String filter = search.getText().toLowerCase();
            int[] idx = {0};

            for (Student s : students) {
                List<Enrollment> enrollments = enrollmentService.getEnrollmentsForStudent(s.getStudentId());
                for (Enrollment en : enrollments) {
                    if (!filter.isEmpty() &&
                            !s.getName().toLowerCase().contains(filter) &&
                            !en.getSection().getCourse().getCode().toLowerCase().contains(filter) &&
                            !en.getSection().getCourse().getTitle().toLowerCase().contains(filter)) continue;

                    HBox row = new HBox(16);
                    row.setPadding(new Insets(10, 20, 10, 20));
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setStyle("-fx-background-color: " + (idx[0] % 2 == 0 ? "white" : "#fafbff") + "; " +
                            "-fx-border-color: transparent transparent #f0f0f0 transparent;");

                    Label studentName = new Label(s.getName());
                    studentName.setPrefWidth(160);
                    studentName.setStyle("-fx-font-weight: bold; -fx-text-fill: #1a3a5c; -fx-font-size: 12px;");

                    Label courseCode = new Label(en.getSection().getCourse().getCode());
                    courseCode.setPrefWidth(100);
                    courseCode.setStyle("-fx-text-fill: #333; -fx-font-size: 12px;");

                    Label courseTitle = new Label(en.getSection().getCourse().getTitle());
                    courseTitle.setPrefWidth(220);
                    courseTitle.setStyle("-fx-text-fill: #555; -fx-font-size: 12px;");

                    Label sectionNum = new Label("S" + en.getSection().getSectionNumber());
                    sectionNum.setPrefWidth(70);
                    sectionNum.setStyle("-fx-text-fill: #555; -fx-font-size: 12px;");

                    String statusColor = switch (en.getStatus()) {
                        case ENROLLED -> "#27ae60";
                        case DROPPED  -> "#e74c3c";
                        case PASSED   -> "#1a3a5c";
                        case FAILED   -> "#e74c3c";
                        default       -> "#888";
                    };
                    Label status = new Label(en.getStatus().toString());
                    status.setPrefWidth(90);
                    status.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-size: 12px; -fx-font-weight: bold;");

                    String gradeText = en.getGrade() != null ? en.getGrade() + "%" : "-";
                    String gradeColor = en.getGrade() == null ? "#555"
                            : en.getGrade().doubleValue() >= 50 ? "#27ae60" : "#e74c3c";
                    Label gradeLabel = new Label(gradeText);
                    gradeLabel.setPrefWidth(70);
                    gradeLabel.setStyle("-fx-text-fill: " + gradeColor + "; -fx-font-size: 12px; -fx-font-weight: bold;");

                    HBox gradeInput = new HBox(6);
                    gradeInput.setAlignment(Pos.CENTER_LEFT);
                    gradeInput.setPrefWidth(160);

                    if (en.getStatus() == EnrollmentStatus.ENROLLED ||
                            en.getStatus() == EnrollmentStatus.PASSED ||
                            en.getStatus() == EnrollmentStatus.FAILED) {

                        TextField gradeField = new TextField();
                        gradeField.setPromptText("0-100");
                        gradeField.setPrefWidth(70);
                        gradeField.setStyle("-fx-background-radius: 4; -fx-border-radius: 4; " +
                                "-fx-border-color: #cfd8dc; -fx-padding: 4 8; -fx-font-size: 12px;");
                        if (en.getGrade() != null) {
                            gradeField.setText(en.getGrade().toString());
                        }

                        Button saveBtn = new Button("Save");
                        saveBtn.setStyle("-fx-background-color: #1a3a5c; -fx-text-fill: white; " +
                                "-fx-background-radius: 4; -fx-padding: 4 10; " +
                                "-fx-font-size: 11px; -fx-cursor: hand;");

                        saveBtn.setOnAction(e -> {
                            try {
                                double grade = Double.parseDouble(gradeField.getText().trim());
                                if (grade < 0 || grade > 100) {
                                    saveBtn.setText("0-100 only");
                                    saveBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                                            "-fx-background-radius: 4; -fx-padding: 4 10; -fx-font-size: 11px;");
                                    return;
                                }

                                boolean passed = grade >= 50;

                                enrollmentService.updateGrade(
                                        s.getStudentId(),
                                        en.getSection().getSectionId(),
                                        BigDecimal.valueOf(grade)
                                );
                                enrollmentService.updateEnrollmentStatus(
                                        s.getStudentId(),
                                        en.getSection().getSectionId(),
                                        passed ? EnrollmentStatus.PASSED : EnrollmentStatus.FAILED
                                );

                                gradeLabel.setText(grade + "%");
                                gradeLabel.setStyle(passed
                                        ? "-fx-text-fill: #27ae60; -fx-font-size: 12px; -fx-font-weight: bold;"
                                        : "-fx-text-fill: #e74c3c; -fx-font-size: 12px; -fx-font-weight: bold;");

                                status.setText(passed ? "PASSED" : "FAILED");
                                status.setStyle(passed
                                        ? "-fx-text-fill: #1a3a5c; -fx-font-size: 12px; -fx-font-weight: bold;"
                                        : "-fx-text-fill: #e74c3c; -fx-font-size: 12px; -fx-font-weight: bold;");

                                saveBtn.setText("Saved!");
                                saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; " +
                                        "-fx-background-radius: 4; -fx-padding: 4 10; -fx-font-size: 11px;");
                            } catch (NumberFormatException ex) {
                                saveBtn.setText("Invalid!");
                                saveBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                                        "-fx-background-radius: 4; -fx-padding: 4 10; -fx-font-size: 11px;");
                            } catch (Exception ex) {
                                saveBtn.setText("Error");
                                saveBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                                        "-fx-background-radius: 4; -fx-padding: 4 10; -fx-font-size: 11px;");
                            }
                        });

                        gradeInput.getChildren().addAll(gradeField, saveBtn);
                    } else {
                        Label naLabel = new Label("-");
                        naLabel.setStyle("-fx-text-fill: #aaa; -fx-font-size: 12px;");
                        gradeInput.getChildren().add(naLabel);
                    }

                    row.getChildren().addAll(studentName, courseCode, courseTitle,
                            sectionNum, status, gradeLabel, gradeInput);
                    rowsBox.getChildren().add(row);
                    idx[0]++;
                }
            }
        };

        renderRows.run();
        search.textProperty().addListener((obs, o, n) -> renderRows.run());
        card.getChildren().add(rowsBox);

        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        HBox hs = new HBox();
        HBox.setHgrow(hs, Priority.ALWAYS);
        VBox titleBlock = new VBox(4, title, subtitle);
        headerRow.getChildren().addAll(titleBlock, hs, search);

        ScrollPane scroll = new ScrollPane(card);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        wrapper.getChildren().addAll(headerRow, scroll);
        root.getChildren().add(wrapper);
    }

    private Label col(String text, double width) {
        Label l = new Label(text);
        l.setPrefWidth(width);
        l.setStyle("-fx-text-fill: #1a3a5c; -fx-font-size: 11px; -fx-font-weight: bold;");
        return l;
    }
}
