package com.jklu.campusflow.common.security;

import com.jklu.campusflow.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class SecurityMatrixTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest(name = "{0} on {1} expects HTTP {2}")
    @CsvSource({
            "ANONYMOUS, /api/auth/me, 401",
            "STUDENT,   /api/auth/me, 200",
            "STAFF,     /api/auth/me, 200",
            "ADMIN,     /api/auth/me, 200",
            "ANONYMOUS, /api/auth/csrf, 200",
            "STUDENT,   /api/auth/csrf, 200",
            "STAFF,     /api/auth/csrf, 200",
            "ADMIN,     /api/auth/csrf, 200"
    })
    @DisplayName("Security Matrix: Verify endpoint access control by role")
    void testSecurityMatrix(String role, String endpoint, int expectedStatus) throws Exception {
        MockHttpServletRequestBuilder request = get(endpoint);

        if (!"ANONYMOUS".equals(role)) {
            request.with(user(role.toLowerCase() + "@demo.jklu.test").roles(role));
        }

        mockMvc.perform(request)
                .andExpect(status().is(expectedStatus));
    }
}
