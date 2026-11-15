package com.jklu.campusflow.campus.service;

import com.jklu.campusflow.campus.dto.*;
import com.jklu.campusflow.campus.model.GuestRoom;
import com.jklu.campusflow.campus.model.ParkingZone;
import com.jklu.campusflow.campus.model.Room;
import com.jklu.campusflow.campus.repository.BlockRepository;
import com.jklu.campusflow.campus.repository.GuestRoomRepository;
import com.jklu.campusflow.campus.repository.ParkingZoneRepository;
import com.jklu.campusflow.campus.repository.RoomRepository;
import com.jklu.campusflow.common.error.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Transactional(readOnly = true)
public class CampusDataService {

    private final RoomRepository roomRepository;
    private final BlockRepository blockRepository;
    private final GuestRoomRepository guestRoomRepository;
    private final ParkingZoneRepository parkingZoneRepository;
    private final Map<UUID, AtomicInteger> allocatedPermitsByZone = new ConcurrentHashMap<>();

    public void resetAllocatedPermits() {
        allocatedPermitsByZone.clear();
    }

    public CampusDataService(
            RoomRepository roomRepository,
            BlockRepository blockRepository,
            GuestRoomRepository guestRoomRepository,
            ParkingZoneRepository parkingZoneRepository
    ) {
        this.roomRepository = roomRepository;
        this.blockRepository = blockRepository;
        this.guestRoomRepository = guestRoomRepository;
        this.parkingZoneRepository = parkingZoneRepository;
    }

    public List<VenueResponse> listVenues(Authentication auth) {
        boolean isStudent = auth != null && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_STUDENT"));

        List<Room> venues = roomRepository.findAllActiveVenuesWithBlock();
        return venues.stream().map(v -> {
            boolean eligible = !isStudent;
            String reason = isStudent ? "Restricted to STAFF/ADMIN only" : null;
            String location = v.getBlock() != null ? v.getBlock().getName() + ", Floor " + v.getFloor() : "Campus Ground";
            return new VenueResponse(
                    v.getId(),
                    v.getName() != null ? v.getName() : v.getRoomNumber(),
                    v.getType(),
                    v.getCapacity(),
                    location,
                    eligible,
                    reason,
                    v.getFeatures(),
                    true
            );
        }).toList();
    }

    public List<GuestRoomResponse> listGuestRooms(LocalDate checkIn, LocalDate checkOut) {
        List<GuestRoom> rooms = guestRoomRepository.findAllByActiveTrueOrderByRoomNumberAsc();
        return rooms.stream().map(gr -> new GuestRoomResponse(
                gr.getId(),
                gr.getRoomNumber(),
                gr.getType(),
                "DOUBLE",
                gr.getCapacity(),
                true,
                gr.getFeatures(),
                gr.getPricePerNight()
        )).toList();
    }

    public List<ParkingZoneResponse> listParkingZones() {
        List<ParkingZone> zones = parkingZoneRepository.findAllByActiveTrueOrderByNameAsc();
        return zones.stream().map(pz -> {
            int total = pz.getTotalSlots();
            int occupied = Math.min(total, total / 3); // Simulated active occupancy
            int available = total - occupied;
            return new ParkingZoneResponse(
                    pz.getId(),
                    pz.getName(),
                    pz.getCode(),
                    total,
                    occupied,
                    available,
                    available > 0 ? "AVAILABLE" : "FULL",
                    BigDecimal.ZERO
            );
        }).toList();
    }

    @Transactional
    public ParkingPermitResponse requestParkingPermit(ParkingPermitRequest request, Authentication auth) {
        ParkingZone zone = parkingZoneRepository.findByIdForUpdate(request.zoneId())
                .orElseThrow(() -> new ResourceNotFoundException("Parking zone not found with id: " + request.zoneId()));

        AtomicInteger allocated = allocatedPermitsByZone.computeIfAbsent(zone.getId(), k -> new AtomicInteger(0));
        if (allocated.incrementAndGet() > zone.getTotalSlots()) {
            allocated.decrementAndGet();
            throw new com.jklu.campusflow.common.error.ConflictException("Parking zone " + zone.getName() + " has reached maximum capacity (" + zone.getTotalSlots() + " slots).");
        }

        UUID permitId = UUID.randomUUID();
        int randomSeq = ThreadLocalRandom.current().nextInt(100, 999);
        int slotNumber = ThreadLocalRandom.current().nextInt(1, zone.getTotalSlots() + 1);
        String year = String.valueOf(LocalDate.now().getYear());
        String permitCode = "JKLU-PARK-" + year + "-" + randomSeq;
        String validUntil = request.visitDate().format(DateTimeFormatter.ISO_LOCAL_DATE) + "T" + request.endTime() + ":00";
        String allocatedSlot = zone.getCode() + "-" + slotNumber;

        return new ParkingPermitResponse(
                permitId,
                permitCode,
                "PENDING",
                allocatedSlot,
                validUntil
        );
    }
}