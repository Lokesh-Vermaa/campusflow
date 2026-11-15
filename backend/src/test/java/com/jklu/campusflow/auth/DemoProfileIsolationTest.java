package com.jklu.campusflow.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jklu.campusflow.CampusFlowApplication;
import com.jklu.campusflow.auth.dto.LoginRequest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
@ActiveProfiles("test") // Notice: "demo" profile is NOT active
public class DemoProfileIsolationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired(required = false)
    private DemoUserSeeder demoUserSeeder;

    @Autowired
    private Flyway flyway;

    @Test
    @DisplayName("When 'demo' profile is NOT active: DemoUserSeeder bean does not exist and demo login fails with 401")
    void testLoginFailsWithoutDemoProfile() throws Exception {
        flyway.clean();
        flyway.migrate();

        // 1. Verify DemoUserSeeder bean is not registered
        assertThat(demoUserSeeder).isNull();

        // 2. Attempt login as student@demo.jklu.test
        LoginRequest request = new LoginRequest("student@demo.jklu.test", "any-password");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"));
    }
}