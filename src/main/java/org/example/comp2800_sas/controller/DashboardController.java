package org.example.comp2800_sas.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.comp2800_sas.model.Course;
import org.example.comp2800_sas.model.EnrollmentCatalogData;
import org.example.comp2800_sas.model.Section;
import org.example.comp2800_sas.model.Student;
import org.example.comp2800_sas.model.Timeslot;
import org.example.comp2800_sas.repository.TimeslotRepository;
import org.example.comp2800_sas.service.CourseService;
import org.example.comp2800_sas.service.EnrollmentCatalogService;
import org.example.comp2800_sas.service.EnrollmentService;
import org.example.comp2800_sas.service.SemesterPlannerService;
import org.example.comp2800_sas.service.SectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class DashboardController {

    @Autowired private CourseService courseService;
    @Autowired private EnrollmentCatalogService enrollmentCatalogService;
    @Autowired private SemesterPlannerService plannerService;
    @Autowired private SectionService sectionService;
    @Autowired private EnrollmentService enrollmentService;
    @Autowired private TimeslotRepository timeslotRepository;
    @Autowired private ConfigurableApplicationContext applicationContext;

    @FXML private Label studentNameLabel;
    @FXML private Label welcomeLabel;
    @FXML private Label enrolledLabel;
    @FXML private StackPane contentArea;

    @FXML private Button btnHome;
    @FXML private Button btnCourses;
    @FXML private Button btnSchedule;
    @FXML private Button btnPlanner;
    @FXML private Button btnAdvisors;
    @FXML private Button btnReports;

    private Student currentStudent;
    private EnrollmentCatalogData cachedCatalog;

    public void setStudent(Student student) {
        this.currentStudent = student;
        studentNameLabel.setText(student.getName());
        welcomeLabel.setText("Welcome back, " + student.getName().split(" ")[0] + "!");
        plannerService.clearAllPlans();
        refreshPlannerBadge();
    }

    private void setActiveButton(Button active) {
        for (Button btn : new Button[]{btnHome, btnCourses, btnSchedule, btnPlanner, btnAdvisors, btnReports}) {
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

    // ─── HOME ────────────────────────────────────────────────────────────────

    @FXML
    public void showHome() {
        setActiveButton(btnHome);
        contentArea.getChildren().clear();
        Label label = new Label("Home dashboard coming soon...");
        label.setStyle("-fx-font-size: 16px; -fx-text-fill: #444;");
        contentArea.getChildren().add(label);
    }

    // ─── COURSES ─────────────────────────────────────────────────────────────

    @FXML
    public void showCourses() {
        setActiveButton(btnCourses);
        contentArea.getChildren().clear();

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(50, 50);
        contentArea.getChildren().add(spinner);

        Task<List<Course>> task = new Task<>() {
            @Override
            protected List<Course> call() {
                return courseService.getAllCourses();
            }
        };

        task.setOnSucceeded(e -> {
            contentArea.getChildren().clear();
            buildCourseList(task.getValue());
        });

        task.setOnFailed(e -> {
            contentArea.getChildren().clear();
            Label error = new Label("Failed to load courses.");
            error.setStyle("-fx-text-fill: red;");
            contentArea.getChildren().add(error);
        });

        new Thread(task).start();
    }

    private void buildCourseList(List<Course> courses) {
        VBox wrapper = new VBox(16);
        wrapper.setPadding(new Insets(24));

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("All Courses (" + courses.size() + ")");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1a3a5c;");

        TextField search = new TextField();
        search.setPromptText("Search courses...");
        search.setPrefWidth(280);
        search.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; " +
                "-fx-border-color: #cfd8dc; -fx-padding: 8 12; -fx-font-size: 13px;");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(title, spacer, search);

        VBox courseList = new VBox(8);
        ObservableList<Course> observableCourses = FXCollections.observableArrayList(courses);

        final HBox[] currentOpenCard = {null};
        final HBox[] currentOpenDescBox = {null};
        final Label[] currentOpenArrow = {null};

        Runnable renderList = () -> {
            courseList.getChildren().clear();
            currentOpenCard[0] = null;
            currentOpenDescBox[0] = null;
            currentOpenArrow[0] = null;

            String filter = search.getText().toLowerCase();

            for (Course c : observableCourses) {
                if (!filter.isEmpty() &&
                        !c.getCode().toLowerCase().contains(filter) &&
                        !c.getTitle().toLowerCase().contains(filter)) continue;

                VBox cardWrapper = new VBox();
                cardWrapper.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 4, 0, 0, 1);");

                HBox card = new HBox(16);
                card.setAlignment(Pos.CENTER_LEFT);
                card.setPadding(new Insets(14, 20, 14, 20));
                card.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-cursor: hand;");

                Label code = new Label(c.getCode());
                code.setPrefWidth(110);
                code.setStyle("-fx-font-weight: bold; -fx-text-fill: #1a3a5c; -fx-font-size: 13px;");

                Label titleLabel = new Label(c.getTitle());
                titleLabel.setStyle("-fx-text-fill: #333; -fx-font-size: 13px;");

                HBox spacer2 = new HBox();
                HBox.setHgrow(spacer2, Priority.ALWAYS);

                Label arrow = new Label("v");
                arrow.setStyle("-fx-text-fill: #aaa; -fx-font-size: 11px;");

                card.getChildren().addAll(code, titleLabel, spacer2, arrow);

                HBox descBox = new HBox();
                descBox.setPadding(new Insets(0, 20, 14, 130));
                descBox.setVisible(false);
                descBox.setManaged(false);

                String descText = c.getDescription() != null ? c.getDescription() : "No description available.";
                Label descLabel = new Label(descText);
                descLabel.setWrapText(true);
                descLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
                descBox.getChildren().add(descLabel);

                cardWrapper.getChildren().addAll(card, descBox);

                card.setOnMouseClicked(e -> {
                    boolean isThisOpen = descBox.isVisible();
                    if (currentOpenDescBox[0] != null && currentOpenDescBox[0] != descBox) {
                        currentOpenDescBox[0].setVisible(false);
                        currentOpenDescBox[0].setManaged(false);
                        currentOpenArrow[0].setText("v");
                        currentOpenCard[0].setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-cursor: hand;");
                    }
                    if (isThisOpen) {
                        descBox.setVisible(false);
                        descBox.setManaged(false);
                        arrow.setText("v");
                        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-cursor: hand;");
                        currentOpenCard[0] = null;
                        currentOpenDescBox[0] = null;
                        currentOpenArrow[0] = null;
                    } else {
                        descBox.setVisible(true);
                        descBox.setManaged(true);
                        arrow.setText("^");
                        card.setStyle("-fx-background-color: #f0f5ff; -fx-background-radius: 8; -fx-cursor: hand;");
                        currentOpenCard[0] = card;
                        currentOpenDescBox[0] = descBox;
                        currentOpenArrow[0] = arrow;
                    }
                });

                card.setOnMouseEntered(e -> {
                    if (!descBox.isVisible())
                        card.setStyle("-fx-background-color: #f5f8ff; -fx-background-radius: 8; -fx-cursor: hand;");
                });
                card.setOnMouseExited(e -> {
                    if (!descBox.isVisible())
                        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-cursor: hand;");
                });

                courseList.getChildren().add(cardWrapper);
            }
        };

        renderList.run();
        search.textProperty().addListener((obs, oldVal, newVal) -> renderList.run());

        ScrollPane scroll = new ScrollPane(courseList);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        wrapper.getChildren().addAll(header, scroll);
        VBox.setVgrow(wrapper, Priority.ALWAYS);
        StackPane.setAlignment(wrapper, Pos.TOP_CENTER);
        contentArea.getChildren().add(wrapper);
    }

    // ─── ENROLL ──────────────────────────────────────────────────────────────

    private static class SectionData {
        Section section;
        List<Timeslot> timeslots;
        int seatsLeft;
        boolean enrolled;

        SectionData(Section section, List<Timeslot> timeslots, int seatsLeft, boolean enrolled) {
            this.section = section;
            this.timeslots = timeslots;
            this.seatsLeft = seatsLeft;
            this.enrolled = enrolled;
        }
    }

    private List<String[]> groupTimeslots(List<Timeslot> timeslots) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("h:mm a");
        Map<String, List<String>> grouped = new LinkedHashMap<>();

        for (Timeslot t : timeslots) {
            String key = t.getStartTime().format(fmt) + "|"
                    + t.getEndTime().format(fmt) + "|"
                    + t.getRoom().getBuilding() + " " + t.getRoom().getRoomNumber();
            grouped.computeIfAbsent(key, k -> new ArrayList<>())
                    .add(abbrevDay(t.getDay().toString()));
        }

        List<String[]> rows = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
            String[] parts = entry.getKey().split("\\|");
            String days = String.join(", ", entry.getValue());
            String time = parts[0] + " – " + parts[1];
            String room = parts[2];
            rows.add(new String[]{days, time, room});
        }
        return rows;
    }

    @FXML
    public void showSchedule() {
        setActiveButton(btnSchedule);
        loadCatalogView(catalog -> {
            VBox enrollmentView = new EnrollmentViewBuilder(
                    enrollmentCatalogService,
                    plannerService,
                    this::showPlanner,
                    this::refreshPlannerBadge
            ).build(catalog);
            StackPane.setAlignment(enrollmentView, Pos.TOP_CENTER);
            contentArea.getChildren().add(enrollmentView);
        }, "Failed to load the Enrollment catalog.");
    }

    @FXML
    public void showPlanner() {
        setActiveButton(btnPlanner);
        loadCatalogView(catalog -> {
            VBox plannerView = new PlannerViewBuilder(
                    enrollmentCatalogService,
                    plannerService,
                    this::showSchedule,
                    this::refreshPlannerBadge
            ).build(catalog);
            StackPane.setAlignment(plannerView, Pos.TOP_CENTER);
            contentArea.getChildren().add(plannerView);
        }, "Failed to load planner data.");
    }

    private void loadCatalogView(Consumer<EnrollmentCatalogData> onSuccess, String failureText) {
        contentArea.getChildren().clear();

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(50, 50);
        contentArea.getChildren().add(spinner);

        Task<EnrollmentCatalogData> task = new Task<>() {
            @Override
            protected EnrollmentCatalogData call() {
                return cachedCatalog != null ? cachedCatalog : enrollmentCatalogService.loadCatalog();
            }
        };

        task.setOnSucceeded(e -> {
            contentArea.getChildren().clear();
            cachedCatalog = task.getValue();
            onSuccess.accept(cachedCatalog);
        });

        task.setOnFailed(e -> {
            contentArea.getChildren().clear();
            Label error = new Label(failureText);
            error.setStyle("-fx-text-fill: #b33a3a; -fx-font-size: 14px; -fx-font-weight: bold;");
            contentArea.getChildren().add(error);
        });

        Thread loader = new Thread(task);
        loader.setDaemon(true);
        loader.start();
    }

    private void refreshPlannerBadge() {
        enrolledLabel.setText(String.valueOf(plannerService.getTotalPlannedCount()));
    }

    private void buildEnrollScreen(Map<Course, List<SectionData>> grouped) {
        VBox wrapper = new VBox(16);
        wrapper.setPadding(new Insets(24));

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Winter 2026 — Course Enrollment");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1a3a5c;");

        TextField search = new TextField();
        search.setPromptText("Search by course code or title...");
        search.setPrefWidth(300);
        search.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; " +
                "-fx-border-color: #cfd8dc; -fx-padding: 8 12; -fx-font-size: 13px;");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(title, spacer, search);

        VBox courseList = new VBox(8);

        final VBox[] openSectionsBox = {null};
        final HBox[] openCourseHeader = {null};
        final Label[] openArrow = {null};

        Runnable renderList = () -> {
            courseList.getChildren().clear();
            openSectionsBox[0] = null;
            openCourseHeader[0] = null;
            openArrow[0] = null;

            String filter = search.getText().toLowerCase();

            for (Map.Entry<Course, List<SectionData>> entry : grouped.entrySet()) {
                Course course = entry.getKey();
                List<SectionData> sections = entry.getValue();

                if (!filter.isEmpty() &&
                        !course.getCode().toLowerCase().contains(filter) &&
                        !course.getTitle().toLowerCase().contains(filter)) continue;

                VBox courseWrapper = new VBox();
                courseWrapper.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 4, 0, 0, 1);");

                HBox courseHeader = new HBox(16);
                courseHeader.setAlignment(Pos.CENTER_LEFT);
                courseHeader.setPadding(new Insets(14, 20, 14, 20));
                courseHeader.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-cursor: hand;");

                Label codeLabel = new Label(course.getCode());
                codeLabel.setPrefWidth(110);
                codeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1a3a5c; -fx-font-size: 13px;");

                Label titleLabel = new Label(course.getTitle());
                titleLabel.setStyle("-fx-text-fill: #333; -fx-font-size: 13px;");

                HBox rowSpacer = new HBox();
                HBox.setHgrow(rowSpacer, Priority.ALWAYS);

                Label sectionCount = new Label(sections.size() + " section" + (sections.size() > 1 ? "s" : "") + " available");
                sectionCount.setStyle("-fx-text-fill: #888; -fx-font-size: 11px;");

                Label arrow = new Label("v");
                arrow.setStyle("-fx-text-fill: #aaa; -fx-font-size: 11px;");

                courseHeader.getChildren().addAll(codeLabel, titleLabel, rowSpacer, sectionCount, arrow);

                // Sections panel
                VBox sectionsBox = new VBox(0);
                sectionsBox.setVisible(false);
                sectionsBox.setManaged(false);
                sectionsBox.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 0 0 8 8;");

                // Column headers
                HBox colHeader = new HBox();
                colHeader.setAlignment(Pos.CENTER_LEFT);
                colHeader.setPadding(new Insets(8, 20, 8, 20));
                colHeader.setStyle("-fx-background-color: #eef2ff; -fx-border-color: transparent transparent #dde3f0 transparent;");

                Label colOption = new Label("OPTION");
                colOption.setPrefWidth(70);
                colOption.setStyle("-fx-text-fill: #1a3a5c; -fx-font-size: 11px; -fx-font-weight: bold;");

                Label colStatus = new Label("STATUS");
                colStatus.setPrefWidth(80);
                colStatus.setStyle("-fx-text-fill: #1a3a5c; -fx-font-size: 11px; -fx-font-weight: bold;");

                Label colDays = new Label("DAYS & TIMES");
                colDays.setPrefWidth(220);
                colDays.setStyle("-fx-text-fill: #1a3a5c; -fx-font-size: 11px; -fx-font-weight: bold;");

                Label colRoom = new Label("ROOM");
                colRoom.setPrefWidth(160);
                colRoom.setStyle("-fx-text-fill: #1a3a5c; -fx-font-size: 11px; -fx-font-weight: bold;");

                Label colInstr = new Label("INSTRUCTOR");
                colInstr.setPrefWidth(160);
                colInstr.setStyle("-fx-text-fill: #1a3a5c; -fx-font-size: 11px; -fx-font-weight: bold;");

                Label colSeats = new Label("SEATS");
                colSeats.setPrefWidth(80);
                colSeats.setStyle("-fx-text-fill: #1a3a5c; -fx-font-size: 11px; -fx-font-weight: bold;");

                colHeader.getChildren().addAll(colOption, colStatus, colDays, colRoom, colInstr, colSeats);
                sectionsBox.getChildren().add(colHeader);

                // Section rows
                for (int i = 0; i < sections.size(); i++) {
                    SectionData data = sections.get(i);
                    List<String[]> timeslotRows = groupTimeslots(data.timeslots);

                    HBox sectionRow = new HBox();
                    sectionRow.setAlignment(Pos.CENTER_LEFT);
                    sectionRow.setPadding(new Insets(12, 20, 12, 20));
                    String rowBg = i % 2 == 0 ? "white" : "#fafbff";
                    sectionRow.setStyle("-fx-background-color: " + rowBg + "; " +
                            "-fx-border-color: transparent transparent #eef0f5 transparent;");

                    // Option number
                    Label optNum = new Label(String.valueOf(i + 1));
                    optNum.setPrefWidth(70);
                    optNum.setStyle("-fx-text-fill: #333; -fx-font-size: 13px; -fx-font-weight: bold;");

                    // Status badge — fixed width, centered text
                    boolean open = data.seatsLeft > 0;
                    Label statusBadge = new Label(open ? "Open" : "Full");
                    statusBadge.setPrefWidth(55);
                    statusBadge.setMaxWidth(55);
                    statusBadge.setAlignment(Pos.CENTER);
                    statusBadge.setStyle(open
                            ? "-fx-text-fill: #155724; -fx-background-color: #d4edda; -fx-background-radius: 4; " +
                            "-fx-padding: 3 0; -fx-font-size: 11px; -fx-font-weight: bold;"
                            : "-fx-text-fill: #721c24; -fx-background-color: #f8d7da; -fx-background-radius: 4; " +
                            "-fx-padding: 3 0; -fx-font-size: 11px; -fx-font-weight: bold;");

                    // Spacer after badge
                    HBox badgeSpacer = new HBox();
                    badgeSpacer.setPrefWidth(25);

                    // Days & Times — grouped
                    VBox daysTimeBox = new VBox(3);
                    daysTimeBox.setPrefWidth(220);
                    if (timeslotRows.isEmpty()) {
                        Label noTime = new Label("TBA");
                        noTime.setStyle("-fx-text-fill: #aaa; -fx-font-size: 12px;");
                        daysTimeBox.getChildren().add(noTime);
                    } else {
                        for (String[] row : timeslotRows) {
                            Label daysLabel = new Label(row[0]);
                            daysLabel.setStyle("-fx-text-fill: #222; -fx-font-size: 12px; -fx-font-weight: bold;");
                            Label timeLabel = new Label(row[1]);
                            timeLabel.setStyle("-fx-text-fill: #555; -fx-font-size: 12px;");
                            daysTimeBox.getChildren().addAll(daysLabel, timeLabel);
                        }
                    }

                    // Room
                    VBox roomBox = new VBox(3);
                    roomBox.setPrefWidth(160);
                    if (timeslotRows.isEmpty()) {
                        Label noRoom = new Label("TBA");
                        noRoom.setStyle("-fx-text-fill: #aaa; -fx-font-size: 12px;");
                        roomBox.getChildren().add(noRoom);
                    } else {
                        for (String[] row : timeslotRows) {
                            Label roomLabel = new Label(row[2]);
                            roomLabel.setStyle("-fx-text-fill: #555; -fx-font-size: 12px;");
                            roomBox.getChildren().add(roomLabel);
                        }
                    }

                    // Instructor
                    Label instrLabel = new Label(data.section.getInstructor().getName());
                    instrLabel.setPrefWidth(160);
                    instrLabel.setStyle("-fx-text-fill: #333; -fx-font-size: 12px;");

                    // Seats
                    Label seatsLabel = new Label(data.seatsLeft + " / " + data.section.getMaxCapacity());
                    seatsLabel.setPrefWidth(80);
                    seatsLabel.setStyle(open
                            ? "-fx-text-fill: #27ae60; -fx-font-size: 12px; -fx-font-weight: bold;"
                            : "-fx-text-fill: #e74c3c; -fx-font-size: 12px; -fx-font-weight: bold;");

                    // Buttons
                    HBox btnBox = new HBox(6);
                    btnBox.setAlignment(Pos.CENTER_RIGHT);
                    HBox.setHgrow(btnBox, Priority.ALWAYS);

                    Button enrollBtn = new Button(data.enrolled ? "Enrolled" : "Enroll");
                    enrollBtn.setDisable(data.enrolled || data.seatsLeft == 0);
                    enrollBtn.setStyle(data.enrolled
                            ? "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 12px; -fx-padding: 6 14;"
                            : "-fx-background-color: #f5c518; -fx-text-fill: #1a3a5c; -fx-font-weight: bold; -fx-background-radius: 6; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");

                    Button dropBtn = new Button("Drop");
                    dropBtn.setVisible(data.enrolled);
                    dropBtn.setManaged(data.enrolled);
                    dropBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                            "-fx-background-radius: 6; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");

                    enrollBtn.setOnAction(e -> {
                        try {
                            enrollmentService.enrollStudent(currentStudent.getStudentId(), data.section.getSectionId());
                            data.enrolled = true;
                            data.seatsLeft--;
                            enrollBtn.setText("Enrolled");
                            enrollBtn.setDisable(true);
                            enrollBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 12px; -fx-padding: 6 14;");
                            dropBtn.setVisible(true);
                            dropBtn.setManaged(true);
                            seatsLabel.setText(data.seatsLeft + " / " + data.section.getMaxCapacity());
                            seatsLabel.setStyle(data.seatsLeft == 0
                                    ? "-fx-text-fill: #e74c3c; -fx-font-size: 12px; -fx-font-weight: bold;"
                                    : "-fx-text-fill: #27ae60; -fx-font-size: 12px; -fx-font-weight: bold;");
                            if (data.seatsLeft == 0) {
                                statusBadge.setText("Full");
                                statusBadge.setStyle("-fx-text-fill: #721c24; -fx-background-color: #f8d7da; " +
                                        "-fx-background-radius: 4; -fx-padding: 3 0; -fx-font-size: 11px; -fx-font-weight: bold;");
                            }
                        } catch (Exception ex) {
                            enrollBtn.setText("Failed");
                            enrollBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 12px; -fx-padding: 6 14;");
                        }
                    });

                    dropBtn.setOnAction(e -> {
                        try {
                            enrollmentService.dropStudent(currentStudent.getStudentId(), data.section.getSectionId());
                            data.enrolled = false;
                            data.seatsLeft++;
                            enrollBtn.setText("Enroll");
                            enrollBtn.setDisable(false);
                            enrollBtn.setStyle("-fx-background-color: #f5c518; -fx-text-fill: #1a3a5c; -fx-font-weight: bold; " +
                                    "-fx-background-radius: 6; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
                            dropBtn.setVisible(false);
                            dropBtn.setManaged(false);
                            seatsLabel.setText(data.seatsLeft + " / " + data.section.getMaxCapacity());
                            seatsLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 12px; -fx-font-weight: bold;");
                            statusBadge.setText("Open");
                            statusBadge.setStyle("-fx-text-fill: #155724; -fx-background-color: #d4edda; " +
                                    "-fx-background-radius: 4; -fx-padding: 3 0; -fx-font-size: 11px; -fx-font-weight: bold;");
                        } catch (Exception ex) {
                            dropBtn.setText("Failed");
                            dropBtn.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 12px; -fx-padding: 6 14;");
                        }
                    });

                    btnBox.getChildren().addAll(enrollBtn, dropBtn);
                    sectionRow.getChildren().addAll(optNum, statusBadge, badgeSpacer, daysTimeBox, roomBox, instrLabel, seatsLabel, btnBox);
                    sectionsBox.getChildren().add(sectionRow);
                }

                courseWrapper.getChildren().addAll(courseHeader, sectionsBox);

                courseHeader.setOnMouseClicked(e -> {
                    boolean isOpen = sectionsBox.isVisible();

                    if (openSectionsBox[0] != null && openSectionsBox[0] != sectionsBox) {
                        openSectionsBox[0].setVisible(false);
                        openSectionsBox[0].setManaged(false);
                        openArrow[0].setText("v");
                        openCourseHeader[0].setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-cursor: hand;");
                    }

                    if (isOpen) {
                        sectionsBox.setVisible(false);
                        sectionsBox.setManaged(false);
                        arrow.setText("v");
                        courseHeader.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-cursor: hand;");
                        openSectionsBox[0] = null;
                        openCourseHeader[0] = null;
                        openArrow[0] = null;
                    } else {
                        sectionsBox.setVisible(true);
                        sectionsBox.setManaged(true);
                        arrow.setText("^");
                        courseHeader.setStyle("-fx-background-color: #eef2ff; -fx-background-radius: 8 8 0 0; -fx-cursor: hand;");
                        openSectionsBox[0] = sectionsBox;
                        openCourseHeader[0] = courseHeader;
                        openArrow[0] = arrow;
                    }
                });

                courseHeader.setOnMouseEntered(e -> {
                    if (!sectionsBox.isVisible())
                        courseHeader.setStyle("-fx-background-color: #f5f8ff; -fx-background-radius: 8; -fx-cursor: hand;");
                });
                courseHeader.setOnMouseExited(e -> {
                    if (!sectionsBox.isVisible())
                        courseHeader.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-cursor: hand;");
                });

                courseList.getChildren().add(courseWrapper);
            }
        };

        renderList.run();
        search.textProperty().addListener((obs, oldVal, newVal) -> renderList.run());

        ScrollPane scroll = new ScrollPane(courseList);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        wrapper.getChildren().addAll(header, scroll);
        VBox.setVgrow(wrapper, Priority.ALWAYS);
        contentArea.getChildren().add(wrapper);
    }

    // ─── ADVISORS ────────────────────────────────────────────────────────────

    @FXML
    public void showAdvisors() {
        setActiveButton(btnAdvisors);
        contentArea.getChildren().clear();
        Label label = new Label("Advisors coming soon...");
        label.setStyle("-fx-font-size: 16px; -fx-text-fill: #444;");
        contentArea.getChildren().add(label);
    }

    // ─── REPORTS ─────────────────────────────────────────────────────────────

    @FXML
    public void showReports() {
        setActiveButton(btnReports);
        contentArea.getChildren().clear();
        Label label = new Label("Reports coming soon...");
        label.setStyle("-fx-font-size: 16px; -fx-text-fill: #444;");
        contentArea.getChildren().add(label);
    }

    // ─── LOGOUT ──────────────────────────────────────────────────────────────

    @FXML
    public void handleLogout() {
        try {
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

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    private String abbrevDay(String day) {
        if (day == null) return "";
        return switch (day.toUpperCase()) {
            case "MONDAY"    -> "Mon";
            case "TUESDAY"   -> "Tue";
            case "WEDNESDAY" -> "Wed";
            case "THURSDAY"  -> "Thu";
            case "FRIDAY"    -> "Fri";
            case "SATURDAY"  -> "Sat";
            case "SUNDAY"    -> "Sun";
            default -> day;
        };
    }
}
