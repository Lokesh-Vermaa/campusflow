package com.jklu.campusflow.performance;

import com.jklu.campusflow.BaseIntegrationTest;
import com.jklu.campusflow.audit.service.AuditService;
import com.jklu.campusflow.availability.service.RoomAvailabilityService;
import com.jklu.campusflow.booking.service.BookingService;
import com.jklu.campusflow.campus.service.CampusDataService;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BoundedQueryTest extends BaseIntegrationTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private RoomAvailabilityService availabilityService;

    @Autowired
    private CampusDataService campusDataService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private AuditService auditService;

    @Test
    @DisplayName("B3: Room availability search runs a bounded number of queries independent of room count (no N+1)")
    void testAvailabilitySearchIsBounded() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics stats = sessionFactory.getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        long before = stats.getQueryExecutionCount();
        availabilityService.searchAvailability(
                LocalDate.of(2026, 11, 12),
                LocalTime.of(14, 0),
                LocalTime.of(16, 0),
                null,
                null,
                null
        );
        long after = stats.getQueryExecutionCount();
        long queries = after - before;

        // Exactly 3 queries: (1) findAllActiveWithBlock, (2) findAllActiveByDayOfWeek, (3) findActiveBookingsBetween
        assertThat(queries).isLessThanOrEqualTo(3);
    }

    @Test
    @DisplayName("B3: Venue listing runs in a single fetch-joined query without N+1 on blocks")
    void testVenueListingIsSingleQuery() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics stats = sessionFactory.getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        long before = stats.getQueryExecutionCount();
        campusDataService.listVenues(null);
        long after = stats.getQueryExecutionCount();
        long queries = after - before;

        // Single query with JOIN FETCH on blocks
        assertThat(queries).isLessThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Phase 3: List my bookings runs bounded queries (count + page) without N+1 on user or items")
    void testListMyBookingsIsBounded() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics stats = sessionFactory.getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        UUID studentId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        long before = stats.getQueryExecutionCount();
        bookingService.listMyBookings(studentId, null, 1, 10);
        long after = stats.getQueryExecutionCount();
        long queries = after - before;

        // Page query + total count query = 2 queries
        assertThat(queries).isLessThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Phase 3: Pending approvals queue runs bounded queries without N+1")
    void testPendingApprovalsIsBounded() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics stats = sessionFactory.getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        long before = stats.getQueryExecutionCount();
        bookingService.getPendingApprovals(1, 10);
        long after = stats.getQueryExecutionCount();
        long queries = after - before;

        assertThat(queries).isLessThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Phase 3: Audit log search runs bounded queries without N+1")
    void testAuditLogSearchIsBounded() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics stats = sessionFactory.getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        long before = stats.getQueryExecutionCount();
        auditService.searchAuditLogs(null, null, null, null, 1, 10);
        long after = stats.getQueryExecutionCount();
        long queries = after - before;

        assertThat(queries).isLessThanOrEqualTo(2);
    }
}