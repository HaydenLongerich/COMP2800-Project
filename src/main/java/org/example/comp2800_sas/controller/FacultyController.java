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
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
@Scope("prototype")
// Builds the Advisors page from catalog teaching data.
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
                // Build the advisor list off the FX thread because the page walks the whole catalog.
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


        Button enrollmentButton = createActionButton("Open Enrollment", "#f5c518", "#1a3a5c");
        enrollmentButton.setOnAction(event -> openEnrollmentAction.run());

        Button calendarButton = createActionButton("Open Calendar", "#1a3a5c", "white");
        calendarButton.setOnAction(event -> openCalendarAction.run());

        HBox actionRow = new HBox(10, enrollmentButton, calendarButton);
        actionRow.setAlignment(Pos.CENTER_RIGHT);

        VBox headerBox = new VBox(4, title);
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
                Label roleLabel = new Label("Current catalog lecture contact");
                roleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #b6cee6;");
                nameBox.getChildren().addAll(nameLabel, roleLabel);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Label sectionBadge = new Label(instructor.assignments().size() + " lecture row" + (instructor.assignments().size() == 1 ? "" : "s"));
                sectionBadge.setStyle("-fx-background-color: #254d73; -fx-text-fill: #a8c8e8; " +
                        "-fx-background-radius: 999; -fx-padding: 4 10; -fx-font-size: 11px;");

                cardHeader.getChildren().addAll(avatar, nameBox, spacer, sectionBadge);

                VBox cardBody = new VBox(14);
                cardBody.setPadding(new Insets(18, 24, 18, 24));

                FlowPane summary = new FlowPane(8, 8);
                instructor.sessions().forEach(session -> summary.getChildren().add(createBadge(session, "#eef4fb", "#31506f")));
                instructor.deliveryModes().forEach(mode -> summary.getChildren().add(createBadge(mode, "#f7fafe", "#31506f")));
                cardBody.getChildren().add(createEmailRow(instructor.name()));
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
        Map<String, Set<InstructorAssignment>> assignmentsByInstructor = new LinkedHashMap<>();
        Map<String, String> displayNamesByInstructor = new LinkedHashMap<>();

        for (EnrollmentCatalogCourse course : courses) {
            for (EnrollmentCatalogOption option : course.options()) {
                for (EnrollmentCatalogSection section : option.sections()) {
                    // Advisors only show lecture contacts, not labs/tutorial placeholders.
                    if (!isAdvisorSection(section)) {
                        continue;
                    }

                    // Collapse duplicate names and remove placeholder values such as TBA or Closed.
                    String instructorName = cleanInstructorName(section.instructor());
                    if (instructorName == null) {
                        continue;
                    }

                    String instructorKey = instructorName.toLowerCase(Locale.ROOT);
                    displayNamesByInstructor.putIfAbsent(instructorKey, instructorName);
                    assignmentsByInstructor
                            .computeIfAbsent(instructorKey, key -> new LinkedHashSet<>())
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
                .map(entry -> {
                    List<InstructorAssignment> assignments = entry.getValue().stream()
                                .sorted(Comparator
                                        .comparing(InstructorAssignment::session)
                                        .thenComparing(InstructorAssignment::courseCode)
                                        .thenComparing(InstructorAssignment::sectionNumber))
                                .toList();

                    return new InstructorCatalogSummary(
                            displayNamesByInstructor.get(entry.getKey()),
                            assignments,
                            assignments.stream()
                                    .map(InstructorAssignment::session)
                                    .distinct()
                                    .toList(),
                            assignments.stream()
                                    .map(InstructorAssignment::deliveryMode)
                                    .distinct()
                                    .toList()
                    );
                })
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

    private HBox createEmailRow(String instructorName) {
        Label emailLabel = new Label("Email");
        emailLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #173b63;");

        // These are intentionally placeholder addresses so users can still copy a predictable contact format.
        TextField emailField = new TextField(buildAdvisorEmail(instructorName));
        emailField.setEditable(false);
        emailField.setStyle(
                "-fx-background-color: #f7fafe; -fx-background-radius: 12; -fx-border-radius: 12; " +
                        "-fx-border-color: #d9e3ef; -fx-text-fill: #31506f; -fx-font-size: 12px; -fx-font-weight: bold;"
        );
        emailField.setOnMouseClicked(event -> emailField.selectAll());
        HBox.setHgrow(emailField, Priority.ALWAYS);

        Button copyEmailButton = createActionButton("Copy Email", "#eef4fb", "#1a3a5c");
        copyEmailButton.setOnAction(event -> {
            ClipboardContent clipboardContent = new ClipboardContent();
            clipboardContent.putString(emailField.getText());
            Clipboard.getSystemClipboard().setContent(clipboardContent);
        });

        VBox emailBlock = new VBox(6, emailLabel, emailField);
        HBox.setHgrow(emailBlock, Priority.ALWAYS);

        HBox row = new HBox(10, emailBlock, copyEmailButton);
        row.setAlignment(Pos.BOTTOM_LEFT);
        return row;
    }

    private boolean isAdvisorSection(EnrollmentCatalogSection section) {
        String normalizedSectionType = fallback(section.sectionType(), "")
                .replaceAll("[^A-Za-z]", "")
                .toUpperCase(Locale.ROOT);
        return normalizedSectionType.startsWith("LEC") || "LECTURE".equals(normalizedSectionType);
    }

    private String cleanInstructorName(String instructor) {
        String cleaned = collapseRepeatedName(fallback(instructor, ""));
        if (cleaned.isBlank() || isPlaceholderInstructorName(cleaned)) {
            return null;
        }
        return cleaned;
    }

    private String collapseRepeatedName(String instructor) {
        String cleaned = fallback(instructor, "").replaceAll("\\s+", " ");
        if (cleaned.isBlank()) {
            return "";
        }

        // Some imported names arrive duplicated as repeated halves; collapse them back to one display name.
        String[] parts = cleaned.split("\\s+");
        for (int segmentLength = 1; segmentLength <= parts.length / 2; segmentLength++) {
            if (parts.length % segmentLength != 0) {
                continue;
            }

            boolean repeats = true;
            for (int index = segmentLength; index < parts.length; index++) {
                if (!parts[index].equalsIgnoreCase(parts[index % segmentLength])) {
                    repeats = false;
                    break;
                }
            }

            if (repeats) {
                return String.join(" ", Arrays.copyOfRange(parts, 0, segmentLength));
            }
        }

        return cleaned;
    }

    private boolean isPlaceholderInstructorName(String instructorName) {
        String normalized = instructorName.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z]+", " ")
                .trim();
        return normalized.isBlank()
                || "closed".equals(normalized)
                || "to be announced".equals(normalized)
                || "tba".equals(normalized)
                || "staff".equals(normalized)
                || "staff tba".equals(normalized);
    }

    private String buildAdvisorEmail(String instructorName) {
        StringBuilder localPart = new StringBuilder();
        for (String part : stripHonorifics(instructorName).split("\\s+")) {
            // Strip punctuation so the generated placeholder email stays copyable and predictable.
            String cleanedPart = part.replaceAll("[^A-Za-z]", "");
            if (!cleanedPart.isBlank()) {
                localPart.append(Character.toUpperCase(cleanedPart.charAt(0)));
                if (cleanedPart.length() > 1) {
                    localPart.append(cleanedPart.substring(1).toLowerCase(Locale.ROOT));
                }
            }
        }

        if (localPart.isEmpty()) {
            localPart.append("AdvisorContact");
        }

        return localPart + "@uwindsor.ca";
    }

    private String stripHonorifics(String name) {
        return fallback(name, "")
                .replaceFirst("(?i)^dr\\.?\\s+", "")
                .replaceFirst("(?i)^prof\\.?\\s+", "")
                .replaceFirst("(?i)^professor\\s+", "")
                .trim();
    }

    private String getInitials(String name) {
        String displayName = stripHonorifics(name);
        if (displayName.isBlank()) {
            return "?";
        }
        String[] parts = displayName.split("\\s+");
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
