package com.jklu.campusflow;

import com.jklu.campusflow.auth.DemoUserSeeder;
import com.jklu.campusflow.common.security.GlobalRateLimitingFilter;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = CampusFlowApplication.class)
@ActiveProfiles({"test", "demo"})
public abstract class BaseIntegrationTest {

    @Autowired
    protected Flyway flyway;

    @Autowired(required = false)
    protected DemoUserSeeder demoUserSeeder;

    @Autowired(required = false)
    protected GlobalRateLimitingFilter globalRateLimitingFilter;

    @BeforeEach
    void setupDatabase() {
        if (globalRateLimitingFilter != null) {
            globalRateLimitingFilter.resetAll();
        }
        flyway.clean();
        flyway.migrate();
        if (demoUserSeeder != null) {
            demoUserSeeder.run();
        }
    }
}