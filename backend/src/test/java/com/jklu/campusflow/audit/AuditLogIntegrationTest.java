package com.jklu.campusflow.audit;

import com.jklu.campusflow.BaseIntegrationTest;
import com.jklu.campusflow.audit.service.AuditService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
public class AuditLogIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AuditService auditService;

    @Test
    @WithMockUser(username = "admin@demo.jklu.test", roles = {"ADMIN"})
    @DisplayName("GET /api/admin/audit searches and paginates audit log records")
    void testSearchAuditLogs() throws Exception {
        mockMvc.perform(get("/api/admin/audit")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.total").value(greaterThanOrEqualTo(50)));
    }

    @Test
    @WithMockUser(username = "admin@demo.jklu.test", roles = {"ADMIN"})
    @DisplayName("GET /api/admin/audit/export returns CSV with formula injection neutralization (' prefix for =, +, -, @)")
    void testAuditLogCsvExportFormulaSanitization() throws Exception {
        // Record an audit log containing formula characters
        auditService.record(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "SECURITY_PROBE",
                "TEST",
                "=SUM(A1:A10)",
                null,
                "{\"probe\":\"+cmd\"}",
                "127.0.0.1",
                "-Mozilla/5.0"
        );

        MvcResult result = mockMvc.perform(get("/api/admin/audit/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", containsString("audit_logs.csv")))
                .andReturn();

        String csv = result.getResponse().getContentAsString();
        assertThat(csv).contains("id,user_id,action,entity_type,entity_id,ip_address,created_at");
        // Verify formula prefixing
        assertThat(csv).contains("'=SUM(A1:A10)");
    }

    @Test
    @DisplayName("Append-only audit trigger prohibits direct UPDATE and DELETE on audit_logs table")
    void testAppendOnlyAuditTrigger() {
        // 1. Assert direct UPDATE on audit_logs is blocked by trigger
        assertThatThrownBy(() ->
                jdbcTemplate.execute("UPDATE audit_logs SET action = 'TAMPERED' WHERE action = 'LOGIN_SUCCESS'")
        ).isInstanceOf(Exception.class)
                .hasMessageContaining("append-only");

        // 2. Assert direct DELETE on audit_logs is blocked by trigger
        assertThatThrownBy(() ->
                jdbcTemplate.execute("DELETE FROM audit_logs WHERE action = 'LOGIN_SUCCESS'")
        ).isInstanceOf(Exception.class)
                .hasMessageContaining("append-only");
    }
}
