package org.example.comp2800_sas.service;

import org.example.comp2800_sas.model.Student;
import org.springframework.stereotype.Service;

/**
 * Stores the currently signed-in user so controllers can share session context.
 */
@Service
public class SessionService {
    private Student currentStudent;

    public Student getCurrentStudent() {
        return currentStudent;
    }

    public void setCurrentStudent(Student student) {
        this.currentStudent = student;
    }

    public void clearCurrentStudent() {
        this.currentStudent = null;
    }
}
