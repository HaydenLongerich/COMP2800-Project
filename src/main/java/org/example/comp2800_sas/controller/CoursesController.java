package org.example.comp2800_sas.controller;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.example.comp2800_sas.model.CourseBrowserEntry;
import org.example.comp2800_sas.service.CourseBrowserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * JavaFX controller for the course catalog browser screen.
 */
@Component
@Scope("prototype")
public class CoursesController {

    private static final String CARD_STYLE =
            "-fx-background-color: white; -fx-background-radius: 16; -fx-border-radius: 16; " +
                    "-fx-border-color: #d9e3ef; -fx-effect: dropshadow(gaussian, rgba(19,49,77,0.06), 14, 0.16, 0, 4);";

    @Autowired private CourseBrowserService courseBrowserService;

    @FXML private VBox root;

    @FXML
    public void initialize() {
        root.getChildren().clear();

        StackPane spinnerPane = new StackPane();
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(50, 50);
        spinnerPane.getChildren().add(spinner);
        VBox.setVgrow(spinnerPane, Priority.ALWAYS);
        root.getChildren().add(spinnerPane);

        Task<List<CourseBrowserEntry>> task = new Task<>() {
            @Override
            protected List<CourseBrowserEntry> call() {
                return courseBrowserService.loadCourses();
            }
        };

        task.setOnSucceeded(event -> {
            root.getChildren().clear();
            buildCourseBrowser(task.getValue());
        });

        task.setOnFailed(event -> {
            root.getChildren().clear();
            Label error = new Label("Failed to load academic course data.");
            error.setStyle("-fx-text-fill: #b94141; -fx-font-size: 13px; -fx-font-weight: bold;");
            root.getChildren().add(error);
        });

        Thread loader = new Thread(task);
        loader.setDaemon(true);
        loader.start();
    }

    private void buildCourseBrowser(List<CourseBrowserEntry> courses) {
        root.setPadding(new Insets(24));
        root.setSpacing(16);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBlock = new VBox(4);
        Label title = new Label("Courses");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #173b63;");

        Label subtitle = new Label(
                ""
        );
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #607286;");
        titleBlock.getChildren().addAll(title, subtitle);

        TextField search = new TextField();
        search.setPromptText("Search by course code or title...");
        search.setPrefWidth(320);
        search.setStyle(
                "-fx-background-radius: 8; -fx-border-radius: 8; " +
                        "-fx-border-color: #cfd8dc; -fx-padding: 8 12; -fx-font-size: 13px;"
        );

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(titleBlock, spacer, search);

        Label resultsLabel = new Label();
        resultsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #607286;");

        VBox courseList = new VBox(12);

        Runnable renderList = () -> {
            courseList.getChildren().clear();
            String filter = normalize(search.getText());
            int visibleCount = 0;

            for (CourseBrowserEntry course : courses) {
                String haystack = normalize(
                        course.courseCode() + " " +
                                course.courseTitle() + " " +
                                course.description() + " " +
                                String.join(" ", course.prerequisites())
                );
                if (!filter.isBlank() && !haystack.contains(filter)) {
                    continue;
                }

                visibleCount++;

                VBox card = new VBox(12);
                card.setPadding(new Insets(18, 20, 18, 20));
                card.setStyle(CARD_STYLE);

                HBox topRow = new HBox(12);
                topRow.setAlignment(Pos.TOP_LEFT);

                VBox nameBlock = new VBox(4);
                Label code = new Label(course.courseCode());
                code.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #1c4a86;");
                Label name = new Label(course.courseTitle());
                name.setWrapText(true);
                name.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #173b63;");
                nameBlock.getChildren().addAll(code, name);

                Region nameSpacer = new Region();
                HBox.setHgrow(nameSpacer, Priority.ALWAYS);

                Label requirementBadge = new Label(course.hasPrerequisites() ? "Prerequisites Listed" : "No Prerequisites Listed");
                requirementBadge.setStyle(course.hasPrerequisites()
                        ? "-fx-background-color: #fff8e6; -fx-text-fill: #7a5a00; -fx-background-radius: 999; -fx-padding: 5 10; -fx-font-size: 11px; -fx-font-weight: bold;"
                        : "-fx-background-color: #f3fbf5; -fx-text-fill: #246344; -fx-background-radius: 999; -fx-padding: 5 10; -fx-font-size: 11px; -fx-font-weight: bold;");
                topRow.getChildren().addAll(nameBlock, nameSpacer, requirementBadge);

                VBox descriptionBlock = new VBox(6);
                Label descriptionTitle = sectionTitle("Description");
                Label description = new Label(course.description().isBlank()
                        ? "No course description is available for this course yet."
                        : course.description());
                description.setWrapText(true);
                description.setStyle("-fx-font-size: 12px; -fx-text-fill: #607286;");
                descriptionBlock.getChildren().addAll(descriptionTitle, description);

                VBox requirementsBlock = new VBox(6);
                Label requirementsTitle = sectionTitle("Requirements / Prerequisites");
                requirementsBlock.getChildren().add(requirementsTitle);
                if (course.prerequisites().isEmpty()) {
                    Label none = new Label("No prerequisites are listed for this course.");
                    none.setWrapText(true);
                    none.setStyle("-fx-font-size: 12px; -fx-text-fill: #607286;");
                    requirementsBlock.getChildren().add(none);
                } else {
                    for (String prerequisite : course.prerequisites()) {
                        requirementsBlock.getChildren().add(createRequirementRow(prerequisite));
                    }
                }

                card.getChildren().addAll(topRow, descriptionBlock, requirementsBlock);
                courseList.getChildren().add(card);
            }

            resultsLabel.setText("Showing " + visibleCount + " of " + courses.size() + " course" + (courses.size() == 1 ? "" : "s") + ".");

            if (courseList.getChildren().isEmpty()) {
                Label empty = new Label("No courses match the current search.");
                empty.setStyle("-fx-text-fill: #607286; -fx-font-size: 12px;");
                courseList.getChildren().add(empty);
            }
        };

        renderList.run();
        search.textProperty().addListener((obs, oldValue, newValue) -> renderList.run());

        ScrollPane scroll = new ScrollPane(courseList);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        root.getChildren().addAll(header, resultsLabel, scroll);
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #173b63;");
        return label;
    }

    private VBox createRequirementRow(String text) {
        VBox row = new VBox();
        row.setPadding(new Insets(8, 10, 8, 10));
        row.setStyle("-fx-background-color: #f7fafe; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #d9e3ef;");

        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: #31506f;");
        row.getChildren().add(label);
        return row;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
