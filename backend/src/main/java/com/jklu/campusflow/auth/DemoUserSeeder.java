package com.jklu.campusflow.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile("demo")
public class DemoUserSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoUserSeeder.class);

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final String demoPassword;

    public DemoUserSeeder(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            @Value("${app.demo-password:${DEMO_USER_PASSWORD:change-me-demo-password}}") String demoPassword
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.demoPassword = demoPassword;
    }

    @Override
    public void run(String... args) {
        seedUser(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "student@demo.jklu.test",
                "Aarav Sharma",
                "STUDENT",
                "Computer Science & Engineering",
                "+91 98765 43210"
        );

        seedUser(
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                "staff@demo.jklu.test",
                "Dr. Neha Verma",
                "STAFF",
                "Computer Science & Engineering",
                "+91 98765 43211"
        );

        seedUser(
                UUID.fromString("00000000-0000-0000-0000-000000000003"),
                "admin@demo.jklu.test",
                "Rajesh Patel",
                "ADMIN",
                "Campus Administration",
                "+91 98765 43212"
        );

        log.info("Runtime demo users seeded successfully in 'demo' profile.");
    }

    private void seedUser(UUID id, String email, String name, String role, String department, String phone) {
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, full_name, role, department, phone) VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT (email) DO NOTHING",
                id, email, passwordEncoder.encode(demoPassword), name, role, department, phone
        );
    }
}