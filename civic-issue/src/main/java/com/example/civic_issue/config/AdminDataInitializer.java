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

    @Override
    public void run(String... args) throws Exception {
        // Check if super admin already exists
        if (userRepository.findByPhoneNumber(superAdminPhone).isEmpty()) {
            User superAdmin = new User();
            superAdmin.setPhoneNumber(superAdminPhone);
            superAdmin.setRole(Role.SUPER_ADMIN);
            superAdmin.setPassword(passwordEncoder.encode(superAdminPassword)); // hashed password
            userRepository.save(superAdmin);
        }

        // You can similarly add department heads or operators
    }
}

