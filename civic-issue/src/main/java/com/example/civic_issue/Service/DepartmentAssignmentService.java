package com.example.civic_issue.Service;

import com.example.civic_issue.Model.Department;
import com.example.civic_issue.Model.User;
import com.example.civic_issue.enums.Role;
import com.example.civic_issue.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DepartmentAssignmentService {

    private final UserRepository userRepository;

    /**
     * Returns the department head assigned for the given department name.
     *
     * @param department Department entity
     * @return User object of department head, or null if none found
     */
    public User getDepartmentHeadForDepartment(Department department) {
        if (department == null) return null;

        return userRepository
                .findByRoleAndDepartment_Name(Role.DEPARTMENT_HEAD, department.getName())
                .orElse(null);
    }
}
