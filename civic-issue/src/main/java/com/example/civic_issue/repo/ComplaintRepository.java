package com.example.civic_issue.repo;

import com.example.civic_issue.Model.Complaint;
import com.example.civic_issue.Model.Department;
import com.example.civic_issue.Model.User;
import com.example.civic_issue.enums.ComplaintStatus;
import com.twilio.base.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.awt.print.Pageable;
import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    // -------------------------------
    // By status
    // -------------------------------
    List<Complaint> findByStatus(ComplaintStatus status);
    // If Complaint has a direct department field
    List<Complaint> findByAssignedTo_Department_Id(Long departmentId);

    // -------------------------------
    // By assigned user (operator or head)
    // -------------------------------
    List<Complaint> findByAssignedTo(User assignedTo);

    // -------------------------------
    // By department (via assignedTo.department)
    // -------------------------------

    // Complaints for a department
    List<Complaint> findByDepartment_Id(Long departmentId);

    // Count complaints for a department by status
    long countByDepartment_IdAndStatus(Long departmentId, ComplaintStatus status);


    Page<Complaint> findByStatus(ComplaintStatus status, Pageable pageable);


    // -------------------------------
    // By citizen who filed the complaint
    // -------------------------------
    List<Complaint> findByUser_Id(Long userId);

    // -------------------------------
    // By status + department
    // -------------------------------
    List<Complaint> findByStatusAndAssignedTo_Department_Id(ComplaintStatus status, Long departmentId);

    // -------------------------------
    // Flexible query: optional filters for status & department
    // -------------------------------
    @Query("SELECT c FROM Complaint c WHERE " +
            "(:status IS NULL OR c.status = :status) AND " +
            "(:departmentId IS NULL OR c.assignedTo.department.id = :departmentId)")
    List<Complaint> findByStatusAndDepartmentOptional(
            @Param("status") ComplaintStatus status,
            @Param("departmentId") Long departmentId
    );

    // -------------------------------
    // Ordered by creation date (most recent first)
    // -------------------------------
    List<Complaint> findByStatusOrderByCreatedAtDesc(ComplaintStatus status);

    long countByAssignedToDepartmentIdAndStatus(Long departmentId, ComplaintStatus status);

    // Optional: count for all departments
    long countByAssignedToDepartmentAndStatus(Department department, ComplaintStatus status);

    // Complaints submitted by a specific user (citizen)
    List<Complaint> findByUser(User user);
    // ComplaintRepository.java
    long countByAssignedTo_IdAndStatus(Long assignedToId, ComplaintStatus status);
    List<Complaint> findByAssignedTo_Id(Long assignedToId);



}
