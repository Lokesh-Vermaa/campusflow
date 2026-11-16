package com.jklu.campusflow.timetable.repository;

import com.jklu.campusflow.timetable.model.TimetableSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TimetableSlotRepository extends JpaRepository<TimetableSlot, UUID> {

    @Query("SELECT ts FROM TimetableSlot ts JOIN FETCH ts.room r JOIN FETCH r.block WHERE ts.active = true AND ts.dayOfWeek = :dayOfWeek")
    List<TimetableSlot> findAllActiveByDayOfWeek(@Param("dayOfWeek") int dayOfWeek);

    @Query("SELECT ts FROM TimetableSlot ts JOIN FETCH ts.room r JOIN FETCH r.block WHERE ts.active = true AND ts.room.id = :roomId AND ts.dayOfWeek = :dayOfWeek ORDER BY ts.startMinute ASC")
    List<TimetableSlot> findAllActiveByRoomIdAndDayOfWeek(@Param("roomId") UUID roomId, @Param("dayOfWeek") int dayOfWeek);

    @Query("""
           SELECT ts FROM TimetableSlot ts
           JOIN FETCH ts.room r
           JOIN FETCH r.block
           WHERE ts.active = true
             AND (
               LOWER(ts.courseCode) LIKE LOWER(CONCAT('%', :q, '%'))
               OR (ts.faculty IS NOT NULL AND LOWER(ts.faculty) LIKE LOWER(CONCAT('%', :q, '%')))
               OR (ts.section IS NOT NULL AND LOWER(ts.section) LIKE LOWER(CONCAT('%', :q, '%')))
               OR LOWER(r.roomNumber) LIKE LOWER(CONCAT('%', :q, '%'))
             )
           ORDER BY ts.dayOfWeek ASC, ts.startMinute ASC
           """)
    List<TimetableSlot> searchActiveSlots(@Param("q") String q);
}