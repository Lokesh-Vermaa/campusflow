package com.jklu.campusflow.booking.repository;

import com.jklu.campusflow.booking.model.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    @Query("SELECT b FROM Booking b JOIN FETCH b.user WHERE b.user.id = :userId ORDER BY b.createdAt DESC")
    Page<Booking> findAllByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT b FROM Booking b JOIN FETCH b.user WHERE b.user.id = :userId AND b.status = :status ORDER BY b.createdAt DESC")
    Page<Booking> findAllByUserIdAndStatusOrderByCreatedAtDesc(@Param("userId") UUID userId, @Param("status") String status, Pageable pageable);

    @Query("SELECT b FROM Booking b JOIN FETCH b.user WHERE b.status = 'PENDING_APPROVAL' ORDER BY b.createdAt ASC")
    Page<Booking> findPendingApprovals(Pageable pageable);

    @Query("SELECT b FROM Booking b JOIN FETCH b.user LEFT JOIN FETCH b.items WHERE b.id = :id")
    Optional<Booking> findByIdWithDetails(@Param("id") UUID id);
}
