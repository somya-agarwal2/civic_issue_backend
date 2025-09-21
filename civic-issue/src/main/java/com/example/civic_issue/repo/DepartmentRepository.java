package com.example.civic_issue.repo;

import com.example.civic_issue.Model.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByName(String name);


    // Optional: You could add case-insensitive search if needed
    // Optional<Department> findByNameIgnoreCase(String name);
}
