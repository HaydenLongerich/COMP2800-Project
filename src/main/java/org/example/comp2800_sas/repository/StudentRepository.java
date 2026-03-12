package org.example.comp2800_sas.repository;

import org.example.comp2800_sas.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Integer> {
    List<Student> findByNameContainingIgnoreCase(String name);
}