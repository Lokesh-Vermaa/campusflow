package com.jklu.campusflow.performance;

import com.jklu.campusflow.BaseIntegrationTest;
import com.jklu.campusflow.availability.service.RoomAvailabilityService;
import com.jklu.campusflow.campus.service.CampusDataService;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class BoundedQueryTest extends BaseIntegrationTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private RoomAvailabilityService availabilityService;

    @Autowired
    private CampusDataService campusDataService;

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
}