package com.jklu.campusflow.campus.repository;

import com.jklu.campusflow.campus.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {

    @Query("SELECT r FROM Room r JOIN FETCH r.block WHERE r.active = true ORDER BY r.roomNumber ASC")
    List<Room> findAllActiveWithBlock();

    @Query("SELECT r FROM Room r JOIN FETCH r.block WHERE r.id = :id AND r.active = true")
    Optional<Room> findActiveByIdWithBlock(@Param("id") UUID id);

    @Query("SELECT r FROM Room r JOIN FETCH r.block WHERE r.roomNumber = :roomNumber AND r.active = true")
    Optional<Room> findByRoomNumberAndActiveTrue(@Param("roomNumber") String roomNumber);

    @Query("SELECT r FROM Room r JOIN FETCH r.block WHERE r.active = true AND r.type IN ('AMPHITHEATRE', 'LAB') ORDER BY r.roomNumber ASC")
    List<Room> findAllActiveVenuesWithBlock();

    @Query("SELECT r FROM Room r JOIN FETCH r.block WHERE r.active = true AND r.type = 'CLASSROOM' ORDER BY r.roomNumber ASC")
    List<Room> findAllActiveClassroomsWithBlock();

    boolean existsByBlockIdAndRoomNumber(UUID blockId, String roomNumber);
}