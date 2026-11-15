package com.jklu.campusflow.campus.controller;

import com.jklu.campusflow.campus.model.Block;
import com.jklu.campusflow.campus.model.GuestRoom;
import com.jklu.campusflow.campus.model.ParkingZone;
import com.jklu.campusflow.campus.model.Room;
import com.jklu.campusflow.campus.repository.BlockRepository;
import com.jklu.campusflow.campus.repository.GuestRoomRepository;
import com.jklu.campusflow.campus.repository.ParkingZoneRepository;
import com.jklu.campusflow.campus.repository.RoomRepository;
import com.jklu.campusflow.common.error.ResourceNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/master")
@PreAuthorize("hasRole('ADMIN')")
public class MasterDataController {

    private final BlockRepository blockRepository;
    private final RoomRepository roomRepository;
    private final GuestRoomRepository guestRoomRepository;
    private final ParkingZoneRepository parkingZoneRepository;

    public MasterDataController(
            BlockRepository blockRepository,
            RoomRepository roomRepository,
            GuestRoomRepository guestRoomRepository,
            ParkingZoneRepository parkingZoneRepository
    ) {
        this.blockRepository = blockRepository;
        this.roomRepository = roomRepository;
        this.guestRoomRepository = guestRoomRepository;
        this.parkingZoneRepository = parkingZoneRepository;
    }

    @GetMapping("/{resource}")
    public ResponseEntity<?> listMasterData(@PathVariable String resource) {
        return switch (resource.toLowerCase()) {
            case "blocks" -> ResponseEntity.ok(blockRepository.findAll());
            case "rooms" -> ResponseEntity.ok(roomRepository.findAllActiveWithBlock().stream().map(r -> Map.of(
                    "id", r.getId(),
                    "roomNumber", r.getRoomNumber(),
                    "name", r.getName() != null ? r.getName() : "",
                    "block", r.getBlock() != null ? r.getBlock().getName() : "",
                    "floor", r.getFloor(),
                    "capacity", r.getCapacity(),
                    "type", r.getType(),
                    "active", r.isActive(),
                    "features", r.getFeatures()
            )).toList());
            case "guesthouse" -> ResponseEntity.ok(guestRoomRepository.findAll());
            case "parking" -> ResponseEntity.ok(parkingZoneRepository.findAll());
            default -> throw new ResourceNotFoundException("Unknown master resource: " + resource);
        };
    }
}