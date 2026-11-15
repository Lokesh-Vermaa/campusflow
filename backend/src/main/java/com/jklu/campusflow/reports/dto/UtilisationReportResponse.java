package com.jklu.campusflow.reports.dto;

import java.time.LocalDate;
import java.util.List;

public record UtilisationReportResponse(
        String startDate,
        String endDate,
        long totalBookingsPeriod,
        long totalBookings,
        double overallUtilisationPct,
        List<HourlyUtilisationDto> byHour,
        List<RoomUtilisationDto> topRooms,
        List<RoomUtilisationDto> leastRooms,
        List<String> peakHours,
        double guestHouseOccupancyPct,
        double parkingPeakOccupancyPct,
        long pendingApprovalsCount,
        long freeRoomsNow
) {}
