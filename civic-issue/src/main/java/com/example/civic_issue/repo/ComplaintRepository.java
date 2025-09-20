package com.example.civic_issue.repo;

import com.example.civic_issue.Model.Complaint;
import com.example.civic_issue.enums.ComplaintStatus;
import com.example.civic_issue.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    // Find all complaints by status
    List<Complaint> findByStatus(ComplaintStatus status);
    List<Complaint> findAll();

    // Find all complaints assigned to a user (operator or head)
    List<Complaint> findByAssignedTo(User assignedTo);

    // Find all complaints of a department (via assignedTo.department)
    List<Complaint> findByAssignedTo_Department_Id(Long departmentId);

    // Optional: find by user (citizen) who filed it
    List<Complaint> findByUser_Id(Long userId);


    List<Complaint> findByStatusAndAssignedTo_Department_Id(ComplaintStatus status, Long departmentId);



}
