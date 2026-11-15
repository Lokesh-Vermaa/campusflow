package com.jklu.campusflow.campus.repository;

import com.jklu.campusflow.campus.model.GuestRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GuestRoomRepository extends JpaRepository<GuestRoom, UUID> {
    List<GuestRoom> findAllByActiveTrueOrderByRoomNumberAsc();
    Optional<GuestRoom> findByRoomNumberAndActiveTrue(String roomNumber);
}