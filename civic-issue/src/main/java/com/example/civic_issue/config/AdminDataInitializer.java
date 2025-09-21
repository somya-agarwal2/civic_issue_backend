package com.example.civic_issue.config;

import com.example.civic_issue.Model.User;
import com.example.civic_issue.enums.Role;
import com.example.civic_issue.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class AdminDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Inject values from application.properties
    @Value("${superadmin.phone}")
    private String superAdminPhone;

    @Value("${superadmin.password}")
    private String superAdminPassword;

    @Value("${superadmin.name:Super Admin}") // fallback default
    private String superAdminName;

    @Value("${superadmin.email:admin@civicflow.gov}") // fallback default
    private String superAdminEmail;

    @Override
    public void run(String... args) {
        // Check if super admin already exists
        if (userRepository.findByPhoneNumber(superAdminPhone).isEmpty()) {
            User superAdmin = new User();
            superAdmin.setPhoneNumber(superAdminPhone);
            superAdmin.setRole(Role.SUPER_ADMIN);
            superAdmin.setPassword(passwordEncoder.encode(superAdminPassword)); // hashed password

            // ✅ Added missing profile fields
            superAdmin.setFullName(superAdminName);
            superAdmin.setEmail(superAdminEmail);
            superAdmin.setDepartment(null); // SUPER_ADMIN has no department

            userRepository.save(superAdmin);
        }

        // TODO: Optionally seed Department Heads and Operators if needed
        // This would make role-based testing easier
    }
}
