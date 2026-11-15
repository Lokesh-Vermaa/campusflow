package com.jklu.campusflow.campus.repository;

import com.jklu.campusflow.campus.model.ParkingZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParkingZoneRepository extends JpaRepository<ParkingZone, UUID> {
    List<ParkingZone> findAllByActiveTrueOrderByNameAsc();
    Optional<ParkingZone> findByCode(String code);
}