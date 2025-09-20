package com.example.civic_issue.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Disable CSRF for simplicity
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/auth/**").permitAll()             // OTP endpoints for citizens
                        .requestMatchers("/auth/admin-login").permitAll()    // Admin login via password
                        // Admin endpoints protected by role
                        .requestMatchers("/admin/**").hasAnyAuthority("SUPER_ADMIN", "DEPARTMENT_HEAD", "OPERATOR")
                        // Citizen endpoints protected for authenticated users
                        .requestMatchers("/api/users/**").authenticated()
                        .requestMatchers("/api/complaints/create").hasAuthority("CITIZEN")
                        .requestMatchers("/api/complaints/update-status/**").hasAnyAuthority("SUPER_ADMIN","DEPARTMENT_HEAD","OPERATOR")

                        .anyRequest().authenticated()
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS) // No sessions, JWT only
                );

        // Add JWT filter before Spring Security's authentication filter
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
