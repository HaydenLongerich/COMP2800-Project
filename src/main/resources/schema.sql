CREATE TABLE IF NOT EXISTS PLANNER_SELECTION (
    planner_selection_id BIGINT NOT NULL AUTO_INCREMENT,
    student_id INT NOT NULL,
    session_name VARCHAR(120) NOT NULL,
    course_code VARCHAR(32) NOT NULL,
    option_number VARCHAR(32) NOT NULL,
    PRIMARY KEY (planner_selection_id),
    CONSTRAINT FK_PLANNER_SELECTION_STUDENT
        FOREIGN KEY (student_id) REFERENCES STUDENT(student_id),
    CONSTRAINT UK_PLANNER_SELECTION_COURSE
        UNIQUE (student_id, session_name, course_code)
);
