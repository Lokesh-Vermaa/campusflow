package com.jklu.campusflow.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jklu.campusflow.CampusFlowApplication;
import com.jklu.campusflow.auth.dto.LoginRequest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CampusFlowApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles({"test", "demo"}) // Notice: "demo" profile IS active
public class DemoProfileActiveTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired(required = false)
    private DemoUserSeeder demoUserSeeder;

    @Autowired
    private Flyway flyway;

    @Value("${app.demo-password:${DEMO_USER_PASSWORD:change-me-demo-password}}")
    private String demoPassword;

    @Test
    @DisplayName("When 'demo' profile IS active: DemoUserSeeder bean exists and demo login succeeds with 200")
    void testLoginSucceedsWithDemoProfile() throws Exception {
        flyway.clean();
        flyway.migrate();

        // 1. Verify DemoUserSeeder bean is registered
        assertThat(demoUserSeeder).isNotNull();
        demoUserSeeder.run();

        // 2. Attempt login as student@demo.jklu.test
        LoginRequest request = new LoginRequest("student@demo.jklu.test", demoPassword);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("student@demo.jklu.test"))
                .andExpect(jsonPath("$.user.role").value("STUDENT"));
    }
}