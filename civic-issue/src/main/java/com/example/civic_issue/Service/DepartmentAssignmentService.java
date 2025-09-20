
package com.example.civic_issue.Service;

import com.example.civic_issue.Model.User;
import com.example.civic_issue.enums.Category;
import com.example.civic_issue.enums.Role;
import com.example.civic_issue.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DepartmentAssignmentService {

    private final UserRepository userRepository;

    /**
     * Get the department head assigned for the given category.
     */
    public User getDepartmentHeadForCategory(String departmentName) {
        if (departmentName == null) return null;
        return userRepository
                .findByRoleAndDepartment_Name(Role.DEPARTMENT_HEAD, departmentName)
                .orElse(null);
    }

    public boolean isValidCategory(String categoryStr) {
        return Category.fromString(categoryStr) != null;
    }

}
