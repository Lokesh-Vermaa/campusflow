package com.jklu.campusflow.availability.service;

import com.jklu.campusflow.availability.dto.AvailabilityResponse;
import com.jklu.campusflow.availability.dto.RoomScheduleResponse;
import com.jklu.campusflow.booking.model.BookingItem;
import com.jklu.campusflow.booking.repository.BookingItemRepository;
import com.jklu.campusflow.campus.model.Room;
import com.jklu.campusflow.campus.repository.RoomRepository;
import com.jklu.campusflow.common.error.ResourceNotFoundException;
import com.jklu.campusflow.timetable.model.TimetableSlot;
import com.jklu.campusflow.timetable.repository.TimetableSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class RoomAvailabilityService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private final RoomRepository roomRepository;
    private final TimetableSlotRepository timetableSlotRepository;
    private final BookingItemRepository bookingItemRepository;
    private final RoomAvailabilityEngine engine = new RoomAvailabilityEngine();

    public RoomAvailabilityService(
            RoomRepository roomRepository,
            TimetableSlotRepository timetableSlotRepository,
            BookingItemRepository bookingItemRepository
    ) {
        this.roomRepository = roomRepository;
        this.timetableSlotRepository = timetableSlotRepository;
        this.bookingItemRepository = bookingItemRepository;
    }

    public AvailabilityResponse searchAvailability(
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            Integer minCapacity,
            UUID blockId,
            String featuresCsv
    ) {
        List<String> requiredFeatures = null;
        if (featuresCsv != null && !featuresCsv.isBlank()) {
            requiredFeatures = Arrays.stream(featuresCsv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }

        // Bounded database fetch (O(1) queries independent of room count)
        List<Room> rooms = roomRepository.findAllActiveWithBlock();
        int dayOfWeek = date.getDayOfWeek().getValue();
        List<TimetableSlot> slots = timetableSlotRepository.findAllActiveByDayOfWeek(dayOfWeek);

        OffsetDateTime dayStart = date.atStartOfDay(ZONE).toOffsetDateTime();
        OffsetDateTime dayEnd = date.plusDays(1).atStartOfDay(ZONE).toOffsetDateTime();
        List<BookingItem> bookings = bookingItemRepository.findActiveBookingsBetween(dayStart, dayEnd);

        return engine.evaluateAvailability(
                rooms,
                slots,
                bookings,
                date,
                startTime,
                endTime,
                minCapacity,
                blockId,
                requiredFeatures
        );
    }

    public RoomScheduleResponse getRoomSchedule(UUID roomId, LocalDate date) {
        Room room = roomRepository.findActiveByIdWithBlock(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + roomId));

        int dayOfWeek = date.getDayOfWeek().getValue();
        List<TimetableSlot> slots = timetableSlotRepository.findAllActiveByRoomIdAndDayOfWeek(roomId, dayOfWeek);

        OffsetDateTime dayStart = date.atStartOfDay(ZONE).toOffsetDateTime();
        OffsetDateTime dayEnd = date.plusDays(1).atStartOfDay(ZONE).toOffsetDateTime();
        List<BookingItem> bookings = bookingItemRepository.findActiveBookingsForResourceBetween(roomId, dayStart, dayEnd);

        return engine.computeSchedule(room, slots, bookings, date);
    }
}