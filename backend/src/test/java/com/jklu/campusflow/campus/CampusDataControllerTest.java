package com.jklu.campusflow.campus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jklu.campusflow.BaseIntegrationTest;
import com.jklu.campusflow.campus.dto.ParkingPermitRequest;
import com.jklu.campusflow.campus.model.ParkingZone;
import com.jklu.campusflow.campus.model.Room;
import com.jklu.campusflow.campus.repository.ParkingZoneRepository;
import com.jklu.campusflow.campus.repository.RoomRepository;
import com.jklu.campusflow.timetable.dto.TimetablePublishRequest;
import com.jklu.campusflow.timetable.dto.TimetableRowParsed;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class CampusDataControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ParkingZoneRepository parkingZoneRepository;

    @Test
    @WithMockUser(username = "student@demo.jklu.test", roles = {"STUDENT"})
    @DisplayName("GET /api/rooms/availability returns 200 OK and valid available/unavailable rooms shape")
    void testSearchAvailability() throws Exception {
        mockMvc.perform(get("/api/rooms/availability")
                        .param("date", "2026-11-12")
                        .param("startTime", "14:00")
                        .param("endTime", "16:00")
                        .param("minCapacity", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-11-12"))
                .andExpect(jsonPath("$.requestedInterval.startTime").value("14:00"))
                .andExpect(jsonPath("$.requestedInterval.endTime").value("16:00"))
                .andExpect(jsonPath("$.availableRooms").isArray())
                .andExpect(jsonPath("$.unavailableRooms").isArray());
    }

    @Test
    @WithMockUser(username = "student@demo.jklu.test", roles = {"STUDENT"})
    @DisplayName("GET /api/rooms/{id}/schedule returns 200 and heatmap slots")
    void testGetRoomSchedule() throws Exception {
        Room room = roomRepository.findAllActiveWithBlock().get(0);

        mockMvc.perform(get("/api/rooms/" + room.getId() + "/schedule")
                        .param("date", "2026-11-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(room.getId().toString()))
                .andExpect(jsonPath("$.roomNumber").value(room.getRoomNumber()))
                .andExpect(jsonPath("$.slots").isArray());
    }

    @Test
    @WithMockUser(username = "student@demo.jklu.test", roles = {"STUDENT"})
    @DisplayName("GET /api/venues returns 200 with amphitheatres and labs")
    void testListVenues() throws Exception {
        mockMvc.perform(get("/api/venues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].type").exists())
                .andExpect(jsonPath("$[0].requiresApproval").value(true));
    }

    @Test
    @WithMockUser(username = "staff@demo.jklu.test", roles = {"STAFF"})
    @DisplayName("GET /api/guesthouse/rooms returns 200 for STAFF")
    void testListGuestHouseRoomsForStaff() throws Exception {
        mockMvc.perform(get("/api/guesthouse/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].roomNumber").exists())
                .andExpect(jsonPath("$[0].isAvailable").value(true));
    }

    @Test
    @WithMockUser(username = "student@demo.jklu.test", roles = {"STUDENT"})
    @DisplayName("GET /api/guesthouse/rooms returns 403 Forbidden for STUDENT")
    void testListGuestHouseRoomsForbiddenForStudent() throws Exception {
        mockMvc.perform(get("/api/guesthouse/rooms"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "student@demo.jklu.test", roles = {"STUDENT"})
    @DisplayName("GET /api/parking/zones returns 200 and available slot count")
    void testListParkingZones() throws Exception {
        mockMvc.perform(get("/api/parking/zones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].totalSlots").exists())
                .andExpect(jsonPath("$[0].availableSlots").exists());
    }

    @Test
    @WithMockUser(username = "student@demo.jklu.test", roles = {"STUDENT"})
    @DisplayName("POST /api/parking/permits returns 201 Created and permit code")
    void testRequestParkingPermit() throws Exception {
        ParkingZone zone = parkingZoneRepository.findAll().get(0);
        ParkingPermitRequest req = new ParkingPermitRequest(
                "Prof. Amit Sengupta",
                "RJ-14-CB-9021",
                "FOUR_WHEELER",
                zone.getId(),
                LocalDate.of(2026, 11, 15),
                "10:00",
                "17:00",
                "Guest Lecture"
        );

        mockMvc.perform(post("/api/parking/permits")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.permitId").exists())
                .andExpect(jsonPath("$.permitCode").value(org.hamcrest.Matchers.startsWith("JKLU-PARK-")))
                .andExpect(jsonPath("$.status").value("ALLOCATED"))
                .andExpect(jsonPath("$.allocatedSlot").exists());
    }

    @Test
    @WithMockUser(username = "admin@demo.jklu.test", roles = {"ADMIN"})
    @DisplayName("POST /api/admin/timetable/upload parses CSV and stages rows")
    void testAdminUploadTimetable() throws Exception {
        String csv = """
                course,room,day,start,end,section,faculty
                CS301,A-101,Monday,09:00,10:00,A,Dr. Neha
                CS302,A-102,Tuesday,10:00,11:00,B,Dr. Verma
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "timetable.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/admin/timetable/upload")
                        .file(file)
                        .with(csrf())
                        .param("academicTerm", "Spring 2027"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadId").exists())
                .andExpect(jsonPath("$.totalRows").value(2))
                .andExpect(jsonPath("$.validRows").value(2))
                .andExpect(jsonPath("$.preview", hasSize(2)));
    }

    @Test
    @WithMockUser(username = "admin@demo.jklu.test", roles = {"ADMIN"})
    @DisplayName("POST /api/admin/timetable/publish commits slots to timetable_slots")
    void testAdminPublishTimetable() throws Exception {
        TimetablePublishRequest req = new TimetablePublishRequest(
                "Spring 2027",
                null,
                List.of(new TimetableRowParsed(
                        1,
                        "CS401",
                        "A-101",
                        1,
                        "11:00",
                        "12:00",
                        660,
                        720,
                        "A",
                        "Dr. Neha",
                        "Spring 2027",
                        true,
                        null
                ))
        );

        mockMvc.perform(post("/api/admin/timetable/publish")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.publishedSlots").value(1));
    }

    @Test
    @WithMockUser(username = "admin@demo.jklu.test", roles = {"ADMIN"})
    @DisplayName("GET /api/admin/master/rooms returns active rooms")
    void testMasterDataRooms() throws Exception {
        mockMvc.perform(get("/api/admin/master/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }
}