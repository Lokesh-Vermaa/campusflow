package com.jklu.campusflow.availability.service;

import com.jklu.campusflow.availability.dto.*;
import com.jklu.campusflow.booking.model.BookingItem;
import com.jklu.campusflow.campus.model.Room;
import com.jklu.campusflow.timetable.model.TimetableSlot;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

public class RoomAvailabilityEngine {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    private static final int DAY_START_MIN = 8 * 60;  // 08:00
    private static final int DAY_END_MIN = 20 * 60;   // 20:00

    public record BusySlot(int startMinute, int endMinute, String title, String type, String category) {}

    public AvailabilityResponse evaluateAvailability(
            List<Room> rooms,
            List<TimetableSlot> timetableSlots,
            List<BookingItem> activeBookings,
            LocalDate date,
            LocalTime requestedStartTime,
            LocalTime requestedEndTime,
            Integer minCapacity,
            UUID blockId,
            List<String> requiredFeatures
    ) {
        int reqStart = requestedStartTime.getHour() * 60 + requestedStartTime.getMinute();
        int reqEnd = requestedEndTime.getHour() * 60 + requestedEndTime.getMinute();
        TimeIntervalDto requestedInterval = new TimeIntervalDto(formatTime(reqStart), formatTime(reqEnd));

        // Group slots by roomId
        Map<UUID, List<BusySlot>> busySlotsByRoom = new HashMap<>();

        for (TimetableSlot ts : timetableSlots) {
            UUID rId = ts.getRoom().getId();
            busySlotsByRoom.computeIfAbsent(rId, k -> new ArrayList<>())
                    .add(new BusySlot(ts.getStartMinute(), ts.getEndMinute(), ts.getCourseCode(), "CLASS", "TIMETABLE"));
        }

        for (BookingItem bi : activeBookings) {
            UUID rId = bi.getResourceId();
            int bStart = bi.getStartTime().atZoneSameInstant(ZONE).toLocalTime().toSecondOfDay() / 60;
            int bEnd = bi.getEndTime().atZoneSameInstant(ZONE).toLocalTime().toSecondOfDay() / 60;
            // Bound to day
            int clampedStart = Math.max(0, bStart);
            int clampedEnd = Math.min(24 * 60, bEnd);
            if (clampedStart < clampedEnd) {
                busySlotsByRoom.computeIfAbsent(rId, k -> new ArrayList<>())
                        .add(new BusySlot(clampedStart, clampedEnd, "Reserved Booking", "BOOKING", "BOOKING"));
            }
        }

        List<AvailableRoomDto> availableRooms = new ArrayList<>();
        List<UnavailableRoomDto> unavailableRooms = new ArrayList<>();

        for (Room room : rooms) {
            // Apply filtering criteria
            if (minCapacity != null && room.getCapacity() < minCapacity) {
                continue;
            }
            if (blockId != null && (room.getBlock() == null || !blockId.equals(room.getBlock().getId()))) {
                continue;
            }
            if (requiredFeatures != null && !requiredFeatures.isEmpty()) {
                if (room.getFeatures() == null || !new HashSet<>(room.getFeatures()).containsAll(requiredFeatures)) {
                    continue;
                }
            }

            List<BusySlot> roomBusy = busySlotsByRoom.getOrDefault(room.getId(), Collections.emptyList());
            roomBusy.sort(Comparator.comparingInt(BusySlot::startMinute));

            // Check if any slot conflicts with requested interval
            List<BusySlot> conflicts = new ArrayList<>();
            for (BusySlot slot : roomBusy) {
                if (intervalsOverlap(slot.startMinute(), slot.endMinute(), reqStart, reqEnd)) {
                    conflicts.add(slot);
                }
            }

            if (conflicts.isEmpty()) {
                // Room is available
                List<TimeIntervalDto> freeWindows = calculateFreeWindows(roomBusy);
                availableRooms.add(new AvailableRoomDto(
                        room.getId(),
                        room.getRoomNumber(),
                        room.getBlock() != null ? room.getBlock().getName() : "Main Block",
                        room.getFloor(),
                        room.getCapacity(),
                        room.getType(),
                        room.getFeatures(),
                        room.isBookableOutside(),
                        freeWindows
                ));
            } else {
                // Room is unavailable
                BusySlot primaryConflict = conflicts.get(0);
                String conflictReason = "Occupied by " + primaryConflict.title() + " from "
                        + formatTime(primaryConflict.startMinute()) + " to " + formatTime(primaryConflict.endMinute());

                List<TimelineSlotDto> timeline = new ArrayList<>();
                for (BusySlot b : roomBusy) {
                    timeline.add(new TimelineSlotDto(
                            formatTime(b.startMinute()) + "-" + formatTime(b.endMinute()),
                            b.type(),
                            b.title()
                    ));
                }

                unavailableRooms.add(new UnavailableRoomDto(
                        room.getId(),
                        room.getRoomNumber(),
                        room.getBlock() != null ? room.getBlock().getName() : "Main Block",
                        room.getFloor(),
                        room.getCapacity(),
                        room.getType(),
                        room.getFeatures(),
                        conflictReason,
                        timeline
                ));
            }
        }

        return new AvailabilityResponse(date, requestedInterval, availableRooms, unavailableRooms);
    }

    public RoomScheduleResponse computeSchedule(
            Room room,
            List<TimetableSlot> timetableSlots,
            List<BookingItem> activeBookings,
            LocalDate date
    ) {
        List<BusySlot> busyList = new ArrayList<>();

        for (TimetableSlot ts : timetableSlots) {
            busyList.add(new BusySlot(ts.getStartMinute(), ts.getEndMinute(), ts.getCourseCode(), "CLASS", "TIMETABLE"));
        }

        for (BookingItem bi : activeBookings) {
            int bStart = bi.getStartTime().atZoneSameInstant(ZONE).toLocalTime().toSecondOfDay() / 60;
            int bEnd = bi.getEndTime().atZoneSameInstant(ZONE).toLocalTime().toSecondOfDay() / 60;
            int clampedStart = Math.max(DAY_START_MIN, bStart);
            int clampedEnd = Math.min(DAY_END_MIN, bEnd);
            if (clampedStart < clampedEnd) {
                busyList.add(new BusySlot(clampedStart, clampedEnd, "Confirmed Booking", "BOOKING", "BOOKING"));
            }
        }

        busyList.sort(Comparator.comparingInt(BusySlot::startMinute));

        List<ScheduleSlotDto> slots = new ArrayList<>();
        int current = DAY_START_MIN;

        for (BusySlot b : busyList) {
            if (b.startMinute() > current) {
                slots.add(new ScheduleSlotDto(formatTime(current), formatTime(b.startMinute()), "FREE", null, null));
            }
            if (b.endMinute() > current) {
                slots.add(new ScheduleSlotDto(
                        formatTime(Math.max(current, b.startMinute())),
                        formatTime(Math.min(DAY_END_MIN, b.endMinute())),
                        "BUSY",
                        b.title(),
                        b.category()
                ));
                current = Math.max(current, b.endMinute());
            }
        }

        if (current < DAY_END_MIN) {
            slots.add(new ScheduleSlotDto(formatTime(current), formatTime(DAY_END_MIN), "FREE", null, null));
        }

        return new RoomScheduleResponse(room.getId(), room.getRoomNumber(), date, slots);
    }

    public static boolean intervalsOverlap(int startA, int endA, int startB, int endB) {
        return startA < endB && startB < endA;
    }

    private List<TimeIntervalDto> calculateFreeWindows(List<BusySlot> busySlots) {
        List<int[]> mergedBusy = new ArrayList<>();
        for (BusySlot b : busySlots) {
            int s = Math.max(DAY_START_MIN, b.startMinute());
            int e = Math.min(DAY_END_MIN, b.endMinute());
            if (s >= e) continue;

            if (mergedBusy.isEmpty()) {
                mergedBusy.add(new int[]{s, e});
            } else {
                int[] last = mergedBusy.get(mergedBusy.size() - 1);
                if (s <= last[1]) {
                    last[1] = Math.max(last[1], e);
                } else {
                    mergedBusy.add(new int[]{s, e});
                }
            }
        }

        List<TimeIntervalDto> freeWindows = new ArrayList<>();
        int cur = DAY_START_MIN;
        for (int[] busy : mergedBusy) {
            if (busy[0] > cur) {
                freeWindows.add(new TimeIntervalDto(formatTime(cur), formatTime(busy[0])));
            }
            cur = Math.max(cur, busy[1]);
        }
        if (cur < DAY_END_MIN) {
            freeWindows.add(new TimeIntervalDto(formatTime(cur), formatTime(DAY_END_MIN)));
        }

        return freeWindows;
    }

    public static String formatTime(int totalMinutes) {
        int h = totalMinutes / 60;
        int m = totalMinutes % 60;
        return String.format("%02d:%02d", h, m);
    }
}