package com.jklu.campusflow.auth;

import com.jklu.campusflow.CampusFlowApplication;
import com.jklu.campusflow.common.security.GlobalRateLimitingFilter;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CampusFlowApplication.class)
@ActiveProfiles("test") // NO "demo" profile — DemoUserSeeder must NOT run
public class NoDemoProfileEmptyTablesTest {

    @Autowired private Flyway flyway;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired(required = false) private GlobalRateLimitingFilter globalRateLimitingFilter;

    @BeforeEach
    void resetDatabase() {
        if (globalRateLimitingFilter != null) globalRateLimitingFilter.resetAll();
        flyway.clean();
        flyway.migrate();
        // DO NOT call demoUserSeeder.run() — that is the whole point
    }

    @Test
    @DisplayName("Without demo profile: rooms table is empty after clean migration")
    void testRoomsTableIsEmptyWithoutDemoProfile() {
        int count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rooms", Integer.class);
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("Without demo profile: users table is empty after clean migration")
    void testUsersTableIsEmptyWithoutDemoProfile() {
        int count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("Without demo profile: bookings table is empty after clean migration")
    void testBookingsTableIsEmptyWithoutDemoProfile() {
        int count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM bookings", Integer.class);
        assertThat(count).isEqualTo(0);
    }
}
