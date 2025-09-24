package com.example.civic_issue.repo;

import com.example.civic_issue.Model.Complaint;
import com.example.civic_issue.Model.Department;
import com.example.civic_issue.Model.User;
import com.example.civic_issue.enums.ComplaintStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    // -------------------------------
    // By status
    // -------------------------------
    List<Complaint> findByStatus(ComplaintStatus status);

    Page<Complaint> findByStatus(ComplaintStatus status, Pageable pageable);
    List<Complaint> findByStatusAndAssignedTo(ComplaintStatus status, User assignedTo);
    // -------------------------------
    // By assigned user (operator or head)
    // -------------------------------
    List<Complaint> findByAssignedTo(User assignedTo);

    List<Complaint> findByAssignedTo_Id(Long assignedToId);

    // -------------------------------
    // By department (via assignedTo.department)
    // -------------------------------
    List<Complaint> findByAssignedTo_Department_Id(Long departmentId);

    List<Complaint> findByDepartment_Id(Long departmentId);

    long countByDepartment_IdAndStatus(Long departmentId, ComplaintStatus status);

    long countByAssignedToDepartmentIdAndStatus(Long departmentId, ComplaintStatus status);

    long countByAssignedToDepartmentAndStatus(Department department, ComplaintStatus status);

    long countByAssignedTo_IdAndStatus(Long assignedToId, ComplaintStatus status);

    // -------------------------------
    // By citizen who filed the complaint
    // -------------------------------
    List<Complaint> findByUser_Id(Long userId);



    // -------------------------------
    // By status + department
    // -------------------------------
    @EntityGraph(attributePaths = {"assignedTo"})
    List<Complaint> findByStatusAndAssignedTo_Department_Id(ComplaintStatus status, Long departmentId);

    // -------------------------------
    // Flexible query: optional filters for status & department
    // -------------------------------
    @Query("SELECT c FROM Complaint c " +
            "JOIN FETCH c.assignedTo a " + // ensures assignedTo is loaded
            "WHERE (:status IS NULL OR c.status = :status) " +
            "AND (:departmentId IS NULL OR a.department.id = :departmentId)")
    List<Complaint> findByStatusAndDepartmentOptional(
            @Param("status") ComplaintStatus status,
            @Param("departmentId") Long departmentId
    );
    List<Complaint> findByUser(User user);

    List<Complaint> findByUserAndStatus(User user, ComplaintStatus status);

    // -------------------------------
    // Ordered by creation date (most recent first)
    // -------------------------------
    List<Complaint> findByStatusOrderByCreatedAtDesc(ComplaintStatus status);
}
