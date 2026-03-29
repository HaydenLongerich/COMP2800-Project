package org.example.comp2800_sas.repository;

import org.example.comp2800_sas.model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Integer> {
    Optional<Admin> findByUsernameAndPassword(String username, String password);
}