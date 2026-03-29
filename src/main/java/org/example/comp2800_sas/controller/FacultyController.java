package org.example.comp2800_sas.controller;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.example.comp2800_sas.model.EnrollmentCatalogCourse;
import org.example.comp2800_sas.model.EnrollmentCatalogOption;
import org.example.comp2800_sas.model.EnrollmentCatalogSection;
import org.example.comp2800_sas.service.EnrollmentCatalogService;
import org.example.comp2800_sas.util.PlannerScheduleUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@Scope("prototype")
public class FacultyController {

    @Autowired private EnrollmentCatalogService enrollmentCatalogService;

    @FXML private VBox root;

    private Runnable openEnrollmentAction = () -> {};
    private Runnable openCalendarAction = () -> {};

    public void setNavigationActions(Runnable openEnrollmentAction, Runnable openCalendarAction) {
        this.openEnrollmentAction = openEnrollmentAction == null ? () -> {} : openEnrollmentAction;
        this.openCalendarAction = openCalendarAction == null ? () -> {} : openCalendarAction;
    }

    @FXML
    public void initialize() {
        root.getChildren().clear();

        StackPane spinnerPane = new StackPane();
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(50, 50);
        spinnerPane.getChildren().add(spinner);
        VBox.setVgrow(spinnerPane, Priority.ALWAYS);
        root.getChildren().add(spinnerPane);

        Task<List<InstructorCatalogSummary>> task = new Task<>() {
            @Override
            protected List<InstructorCatalogSummary> call() {
                return buildInstructorSummaries(enrollmentCatalogService.loadCatalog().courses());
            }
        };

        task.setOnSucceeded(event -> {
            root.getChildren().clear();
            buildFacultyScreen(task.getValue());
        });

        task.setOnFailed(event -> {
            root.getChildren().clear();
            Label error = new Label("Failed to load catalog instructors.");
            error.setStyle("-fx-text-fill: #b94141; -fx-font-size: 13px; -fx-font-weight: bold;");
            root.getChildren().add(error);
        });

        Thread loader = new Thread(task);
        loader.setDaemon(true);
        loader.start();
    }

    private void buildFacultyScreen(List<InstructorCatalogSummary> instructors) {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox content = new VBox(24);
        content.setPadding(new Insets(24));

        Label title = new Label("Advisors and Teaching Contacts");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1a3a5c;");

        Label subtitle = new Label("Advisor-facing teaching contacts and section details now come directly from the shared Enrollment and Calendar catalog.");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #607286;");
        subtitle.setWrapText(true);

        Button enrollmentButton = createActionButton("Open Enrollment", "#f5c518", "#1a3a5c");
        enrollmentButton.setOnAction(event -> openEnrollmentAction.run());

        Button calendarButton = createActionButton("Open Calendar", "#1a3a5c", "white");
        calendarButton.setOnAction(event -> openCalendarAction.run());

        HBox actionRow = new HBox(10, enrollmentButton, calendarButton);
        actionRow.setAlignment(Pos.CENTER_RIGHT);

        VBox headerBox = new VBox(4, title, subtitle);
        HBox headerRow = new HBox(16);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        HBox headerSpacer = new HBox();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        headerRow.getChildren().addAll(headerBox, headerSpacer, actionRow);
        content.getChildren().add(headerRow);

        if (instructors.isEmpty()) {
            Label empty = new Label("No instructor names are available in the catalog yet.");
            empty.setStyle("-fx-text-fill: #607286; -fx-font-size: 12px;");
            content.getChildren().add(empty);
        } else {
            for (InstructorCatalogSummary instructor : instructors) {
                VBox card = new VBox(0);
                card.setStyle("-fx-background-color: white; -fx-background-radius: 16; " +
                        "-fx-border-radius: 16; -fx-border-color: #d9e3ef; " +
                        "-fx-effect: dropshadow(gaussian, rgba(19,49,77,0.08), 14, 0.16, 0, 4);");

                HBox cardHeader = new HBox(16);
                cardHeader.setPadding(new Insets(20, 24, 20, 24));
                cardHeader.setAlignment(Pos.CENTER_LEFT);
                cardHeader.setStyle("-fx-background-color: #1a3a5c; -fx-background-radius: 16 16 0 0;");

                StackPane avatar = new StackPane();
                avatar.setPrefSize(48, 48);
                avatar.setMinSize(48, 48);
                avatar.setStyle("-fx-background-color: #f5c518; -fx-background-radius: 24;");
                Label initialsLabel = new Label(getInitials(instructor.name()));
                initialsLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1a3a5c; -fx-font-size: 16px;");
                avatar.getChildren().add(initialsLabel);

                VBox nameBox = new VBox(4);
                Label nameLabel = new Label(instructor.name());
                nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
                Label roleLabel = new Label("Current catalog teaching contact");
                roleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #b6cee6;");
                nameBox.getChildren().addAll(nameLabel, roleLabel);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Label sectionBadge = new Label(instructor.assignments().size() + " section row" + (instructor.assignments().size() == 1 ? "" : "s"));
                sectionBadge.setStyle("-fx-background-color: #254d73; -fx-text-fill: #a8c8e8; " +
                        "-fx-background-radius: 999; -fx-padding: 4 10; -fx-font-size: 11px;");

                cardHeader.getChildren().addAll(avatar, nameBox, spacer, sectionBadge);

                VBox cardBody = new VBox(14);
                cardBody.setPadding(new Insets(18, 24, 18, 24));

                FlowPane summary = new FlowPane(8, 8);
                instructor.sessions().forEach(session -> summary.getChildren().add(createBadge(session, "#eef4fb", "#31506f")));
                instructor.deliveryModes().forEach(mode -> summary.getChildren().add(createBadge(mode, "#f7fafe", "#31506f")));
                cardBody.getChildren().add(summary);

                VBox teachingList = new VBox(8);
                for (InstructorAssignment assignment : instructor.assignments()) {
                    VBox row = new VBox(6);
                    row.setPadding(new Insets(10, 12, 10, 12));
                    row.setStyle("-fx-background-color: #f7fafe; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #d9e3ef;");

                    HBox rowHeader = new HBox(10);
                    Label code = new Label(assignment.courseCode());
                    code.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #1c4a86;");

                    Region rowSpacer = new Region();
                    HBox.setHgrow(rowSpacer, Priority.ALWAYS);

                    Label session = new Label(assignment.session());
                    session.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #31506f;");
                    rowHeader.getChildren().addAll(code, rowSpacer, session);

                    Label courseTitle = new Label(assignment.courseTitle());
                    courseTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #173b63;");
                    courseTitle.setWrapText(true);

                    Label detail = new Label(
                            assignment.sectionType() + " " + assignment.sectionNumber() + "  |  " +
                                    assignment.daysAndTime() + "  |  " +
                                    assignment.roomLabel()
                    );
                    detail.setWrapText(true);
                    detail.setStyle("-fx-font-size: 12px; -fx-text-fill: #607286;");

                    row.getChildren().addAll(rowHeader, courseTitle, detail);
                    teachingList.getChildren().add(row);
                }

                cardBody.getChildren().add(teachingList);
                card.getChildren().addAll(cardHeader, cardBody);
                content.getChildren().add(card);
            }
        }

        scroll.setContent(content);
        root.getChildren().add(scroll);
    }

    private List<InstructorCatalogSummary> buildInstructorSummaries(List<EnrollmentCatalogCourse> courses) {
        Map<String, List<InstructorAssignment>> assignmentsByInstructor = new LinkedHashMap<>();

        for (EnrollmentCatalogCourse course : courses) {
            for (EnrollmentCatalogOption option : course.options()) {
                for (EnrollmentCatalogSection section : option.sections()) {
                    String instructorName = cleanInstructorName(section.instructor());
                    assignmentsByInstructor
                            .computeIfAbsent(instructorName, key -> new ArrayList<>())
                            .add(new InstructorAssignment(
                                    course.courseCode(),
                                    course.courseName(),
                                    fallback(option.session(), "Session unavailable"),
                                    fallback(section.sectionType(), "Section"),
                                    fallback(section.section(), "N/A"),
                                    buildDaysAndTime(section),
                                    fallback(section.room(), PlannerScheduleUtils.inferDeliveryMode(section.room())),
                                    PlannerScheduleUtils.inferDeliveryMode(section.room())
                            ));
                }
            }
        }

        return assignmentsByInstructor.entrySet().stream()
                .map(entry -> new InstructorCatalogSummary(
                        entry.getKey(),
                        entry.getValue().stream()
                                .sorted(Comparator
                                        .comparing(InstructorAssignment::session)
                                        .thenComparing(InstructorAssignment::courseCode)
                                        .thenComparing(InstructorAssignment::sectionNumber))
                                .toList(),
                        entry.getValue().stream()
                                .map(InstructorAssignment::session)
                                .distinct()
                                .toList(),
                        entry.getValue().stream()
                                .map(InstructorAssignment::deliveryMode)
                                .distinct()
                                .toList()
                ))
                .sorted(Comparator.comparing(summary -> summary.name().toLowerCase(Locale.ROOT)))
                .toList();
    }

    private String buildDaysAndTime(EnrollmentCatalogSection section) {
        String days = fallback(section.days(), "Days unavailable");
        String time = fallback(section.time(), "Time unavailable");
        if ("Days unavailable".equals(days) && "Time unavailable".equals(time)) {
            return PlannerScheduleUtils.inferDeliveryMode(section.room());
        }
        return days + " " + time;
    }

    private String cleanInstructorName(String instructor) {
        String cleaned = fallback(instructor, "Staff / TBA");
        return cleaned.isBlank() ? "Staff / TBA" : cleaned;
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }
        String[] parts = name.replace("Dr.", "").trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (String part : parts) {
            if (!part.isBlank()) {
                initials.append(part.charAt(0));
            }
        }
        return initials.isEmpty() ? "?" : initials.toString().toUpperCase(Locale.ROOT);
    }

    private Label createBadge(String text, String backgroundColor, String textColor) {
        Label badge = new Label(text);
        badge.setStyle(
                "-fx-background-color: " + backgroundColor + "; -fx-text-fill: " + textColor + "; " +
                        "-fx-background-radius: 999; -fx-padding: 5 10; " +
                        "-fx-font-size: 11px; -fx-font-weight: bold;"
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

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private record InstructorAssignment(
            String courseCode,
            String courseTitle,
            String session,
            String sectionType,
            String sectionNumber,
            String daysAndTime,
            String roomLabel,
            String deliveryMode
    ) {
    }

    private record InstructorCatalogSummary(
            String name,
            List<InstructorAssignment> assignments,
            List<String> sessions,
            List<String> deliveryModes
    ) {
    }
}
