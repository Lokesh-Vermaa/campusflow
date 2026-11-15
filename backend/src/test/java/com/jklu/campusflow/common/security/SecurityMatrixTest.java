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
            // --- Auth endpoints ---
            "ANONYMOUS, /api/auth/me,                   401",
            "STUDENT,   /api/auth/me,                   200",
            "STAFF,     /api/auth/me,                   200",
            "ADMIN,     /api/auth/me,                   200",
            "ANONYMOUS, /api/auth/csrf,                 200",
            // --- Phase 2: Room availability (all roles allowed) ---
            "ANONYMOUS, /api/rooms/availability?date=2026-11-12&startTime=09:00&endTime=10:00, 401",
            "STUDENT,   /api/rooms/availability?date=2026-11-12&startTime=09:00&endTime=10:00, 200",
            "STAFF,     /api/rooms/availability?date=2026-11-12&startTime=09:00&endTime=10:00, 200",
            "ADMIN,     /api/rooms/availability?date=2026-11-12&startTime=09:00&endTime=10:00, 200",
            // --- Phase 2: Venues (all roles allowed) ---
            "ANONYMOUS, /api/venues,                    401",
            "STUDENT,   /api/venues,                    200",
            "STAFF,     /api/venues,                    200",
            "ADMIN,     /api/venues,                    200",
            // --- Phase 2: Guest House (STAFF and ADMIN only) ---
            "ANONYMOUS, /api/guesthouse/rooms,          401",
            "STUDENT,   /api/guesthouse/rooms,          403",
            "STAFF,     /api/guesthouse/rooms,          200",
            "ADMIN,     /api/guesthouse/rooms,          200",
            // --- Phase 2: Parking Zones (all roles) ---
            "ANONYMOUS, /api/parking/zones,             401",
            "STUDENT,   /api/parking/zones,             200",
            "STAFF,     /api/parking/zones,             200",
            "ADMIN,     /api/parking/zones,             200",
            // --- Phase 2: Admin master data (ADMIN only) ---
            "ANONYMOUS, /api/admin/master/rooms,        401",
            "STUDENT,   /api/admin/master/rooms,        403",
            "STAFF,     /api/admin/master/rooms,        403",
            "ADMIN,     /api/admin/master/rooms,        200",
            "STUDENT,   /api/admin/master/blocks,       403",
            "STAFF,     /api/admin/master/blocks,       403",
            "ADMIN,     /api/admin/master/blocks,       200",
            "STUDENT,   /api/admin/master/guesthouse,   403",
            "STAFF,     /api/admin/master/guesthouse,   403",
            "ADMIN,     /api/admin/master/guesthouse,   200",
            "STUDENT,   /api/admin/master/parking,      403",
            "STAFF,     /api/admin/master/parking,      403",
            "ADMIN,     /api/admin/master/parking,      200",
            // --- Phase 3: Bookings /my (authenticated only) ---
            "ANONYMOUS, /api/bookings/my,               401",
            "STUDENT,   /api/bookings/my,               200",
            "STAFF,     /api/bookings/my,               200",
            "ADMIN,     /api/bookings/my,               200",
            // --- Phase 3: Admin Approvals Queue (ADMIN only) ---
            "ANONYMOUS, /api/admin/approvals,           401",
            "STUDENT,   /api/admin/approvals,           403",
            "STAFF,     /api/admin/approvals,           403",
            "ADMIN,     /api/admin/approvals,           200",
            // --- Phase 3: Admin Audit Logs (ADMIN only) ---
            "ANONYMOUS, /api/admin/audit,               401",
            "STUDENT,   /api/admin/audit,               403",
            "STAFF,     /api/admin/audit,               403",
            "ADMIN,     /api/admin/audit,               200",
            "STUDENT,   /api/admin/audit/export,        403",
            "STAFF,     /api/admin/audit/export,        403",
            "ADMIN,     /api/admin/audit/export,        200"
    })
    @DisplayName("Security Matrix: Verify endpoint access control by role (Phase 1 + Phase 2)")
    void testSecurityMatrix(String role, String endpoint, int expectedStatus) throws Exception {
        MockHttpServletRequestBuilder request = get(endpoint.trim());

        if (!"ANONYMOUS".equals(role)) {
            request.with(user(role.trim().toLowerCase() + "@demo.jklu.test").roles(role.trim()));
        }

        mockMvc.perform(request)
                .andExpect(status().is(expectedStatus));
    }
}