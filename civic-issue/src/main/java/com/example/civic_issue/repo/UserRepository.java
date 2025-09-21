package com.example.civic_issue.repo;



import com.example.civic_issue.Model.User;
import com.example.civic_issue.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.w3c.dom.stylesheets.LinkStyle;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhoneNumber(String phoneNumber);

    List<User> findByRole(Role role);
    List<User> findByRoleAndDepartment_Id(Role role, Long departmentId);

    // Dynamic lookup of department head by role + department name
    Optional<User> findByRoleAndDepartment_Name(Role role, String departmentName);
    List<User> findByDepartmentIdAndRole(Long departmentId, Role role);

}
