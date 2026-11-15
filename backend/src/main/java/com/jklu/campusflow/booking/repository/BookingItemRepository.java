package com.jklu.campusflow.booking.repository;

import com.jklu.campusflow.booking.model.BookingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingItemRepository extends JpaRepository<BookingItem, UUID> {

    @Query("SELECT bi FROM BookingItem bi WHERE bi.status IN ('PENDING_APPROVAL', 'CONFIRMED') AND bi.endTime > :dayStart AND bi.startTime < :dayEnd")
    List<BookingItem> findActiveBookingsBetween(@Param("dayStart") OffsetDateTime dayStart, @Param("dayEnd") OffsetDateTime dayEnd);

    @Query("SELECT bi FROM BookingItem bi WHERE bi.resourceId = :resourceId AND bi.status IN ('PENDING_APPROVAL', 'CONFIRMED') AND bi.endTime > :dayStart AND bi.startTime < :dayEnd ORDER BY bi.startTime ASC")
    List<BookingItem> findActiveBookingsForResourceBetween(@Param("resourceId") UUID resourceId, @Param("dayStart") OffsetDateTime dayStart, @Param("dayEnd") OffsetDateTime dayEnd);

    @Query("SELECT bi FROM BookingItem bi WHERE bi.resourceType = :resourceType AND bi.status IN ('PENDING_APPROVAL', 'CONFIRMED') AND bi.endTime > :startTime AND bi.startTime < :endTime")
    List<BookingItem> findActiveBookingsByResourceTypeBetween(@Param("resourceType") String resourceType, @Param("startTime") OffsetDateTime startTime, @Param("endTime") OffsetDateTime endTime);
}