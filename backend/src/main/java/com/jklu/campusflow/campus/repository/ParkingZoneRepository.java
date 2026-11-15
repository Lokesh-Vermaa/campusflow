package com.jklu.campusflow.campus.repository;

import com.jklu.campusflow.campus.model.ParkingZone;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParkingZoneRepository extends JpaRepository<ParkingZone, UUID> {
    List<ParkingZone> findAllByActiveTrueOrderByNameAsc();
    Optional<ParkingZone> findByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pz FROM ParkingZone pz WHERE pz.id = :id AND pz.active = true")
    Optional<ParkingZone> findByIdForUpdate(@Param("id") UUID id);
}