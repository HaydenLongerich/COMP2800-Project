package org.example.comp2800_sas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "PLANNER_SELECTION",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_PLANNER_SELECTION_COURSE",
                columnNames = {"student_id", "session_name", "course_code"}
        )
)
// Persistent saved planner selection for a student's draft calendar.
public class PlannerSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "planner_selection_id")
    private Long plannerSelectionId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "session_name", nullable = false, length = 120)
    private String sessionName;

    @Column(name = "course_code", nullable = false, length = 32)
    private String courseCode;

    @Column(name = "option_number", nullable = false, length = 32)
    private String optionNumber;

    public PlannerSelection() {
    }

    public PlannerSelection(Student student, String sessionName, String courseCode, String optionNumber) {
        this.student = student;
        this.sessionName = sessionName;
        this.courseCode = courseCode;
        this.optionNumber = optionNumber;
    }
}
