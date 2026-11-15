package com.jklu.campusflow.agent.tools;

import com.jklu.campusflow.agent.service.PolicySearchService;
import com.jklu.campusflow.availability.dto.AvailabilityResponse;
import com.jklu.campusflow.availability.dto.RoomScheduleResponse;
import com.jklu.campusflow.availability.service.RoomAvailabilityService;
import com.jklu.campusflow.campus.dto.GuestRoomResponse;
import com.jklu.campusflow.campus.dto.ParkingZoneResponse;
import com.jklu.campusflow.campus.dto.VenueResponse;
import com.jklu.campusflow.campus.service.CampusDataService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Component
public class AgentTools {

    private final RoomAvailabilityService roomAvailabilityService;
    private final CampusDataService campusDataService;
    private final PolicySearchService policySearchService;

    public AgentTools(
            RoomAvailabilityService roomAvailabilityService,
            CampusDataService campusDataService,
            PolicySearchService policySearchService
    ) {
        this.roomAvailabilityService = roomAvailabilityService;
        this.campusDataService = campusDataService;
        this.policySearchService = policySearchService;
    }

    public AvailabilityResponse findFreeRooms(LocalDate date, LocalTime startTime, LocalTime endTime, Integer minCapacity, String userRole) {
        if (date == null || startTime == null || endTime == null) {
            throw new IllegalArgumentException("date, startTime, and endTime must not be null");
        }
        return roomAvailabilityService.searchAvailability(date, startTime, endTime, minCapacity, null, null, userRole);
    }

    public RoomScheduleResponse checkRoomAvailability(UUID roomId, LocalDate date) {
        if (roomId == null || date == null) {
            throw new IllegalArgumentException("roomId and date must not be null");
        }
        return roomAvailabilityService.getRoomSchedule(roomId, date);
    }

    public List<VenueResponse> listVenuesAndLabs() {
        return campusDataService.listVenues(null);
    }

    public List<GuestRoomResponse> checkGuestHouse(LocalDate checkIn, LocalDate checkOut) {
        return campusDataService.listGuestRooms(checkIn, checkOut);
    }

    public List<ParkingZoneResponse> checkParking() {
        return campusDataService.listParkingZones();
    }

    public AvailabilityResponse findAlternatives(LocalDate date, LocalTime startTime, LocalTime endTime, Integer minCapacity) {
        return findFreeRooms(date, startTime, endTime, minCapacity, "STAFF");
    }

    public String searchBookingPolicy(String query) {
        return policySearchService.search(query);
    }
}
