package com.jklu.campusflow.reports;

import com.jklu.campusflow.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
public class ReportsAndContentIntegrationTest extends BaseIntegrationTest {

    @Autowired MockMvc mockMvc;

    @Test
    @DisplayName("PUBLIC CONTENT: Announcement endpoint is publicly accessible without authentication")
    void testPublicAnnouncementEndpoint() throws Exception {
        mockMvc.perform(get("/api/content/announcement")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.message", containsString("Admissions peak week")))
                .andExpect(jsonPath("$.linkText", notNullValue()))
                .andExpect(jsonPath("$.linkHref", notNullValue()))
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    @DisplayName("REPORTS SECURITY: Unauthenticated request to /api/admin/reports/utilisation is rejected with 401")
    void testReportsUnauthenticatedForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/reports/utilisation")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "student@demo.jklu.test", roles = {"STUDENT"})
    @DisplayName("REPORTS SECURITY: Student request to /api/admin/reports/utilisation is rejected with 403")
    void testReportsStudentForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/reports/utilisation")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@demo.jklu.test", roles = {"ADMIN"})
    @DisplayName("REPORTS ADMIN: Admin request to /api/admin/reports/utilisation returns full aggregated report")
    void testAdminUtilisationReport() throws Exception {
        mockMvc.perform(get("/api/admin/reports/utilisation")
                        .param("startDate", "2026-11-09")
                        .param("endDate", "2026-11-15")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startDate", is("2026-11-09")))
                .andExpect(jsonPath("$.endDate", is("2026-11-15")))
                .andExpect(jsonPath("$.byHour", hasSize(greaterThanOrEqualTo(10))))
                .andExpect(jsonPath("$.byHour[0].hour", notNullValue()))
                .andExpect(jsonPath("$.byHour[0].occupancyRate", notNullValue()))
                .andExpect(jsonPath("$.topRooms", notNullValue()))
                .andExpect(jsonPath("$.leastRooms", notNullValue()))
                .andExpect(jsonPath("$.guestHouseOccupancyPct", notNullValue()))
                .andExpect(jsonPath("$.parkingPeakOccupancyPct", notNullValue()))
                .andExpect(jsonPath("$.totalBookingsPeriod", notNullValue()))
                .andExpect(jsonPath("$.pendingApprovalsCount", notNullValue()))
                .andExpect(jsonPath("$.freeRoomsNow", notNullValue()))
                .andExpect(jsonPath("$.peakHours", hasSize(greaterThanOrEqualTo(1))));
    }
}
