package com.jklu.campusflow.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile("demo")
public class DemoUserSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoUserSeeder.class);

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final String demoPassword;

    public DemoUserSeeder(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            @Value("${app.demo-password:${DEMO_USER_PASSWORD:change-me-demo-password}}") String demoPassword
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.demoPassword = demoPassword;
    }

    @Override
    public void run(String... args) {
        // 1. Seed Demo Users
        seedUser(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "student@demo.jklu.test",
                "Aarav Sharma",
                "STUDENT",
                "Computer Science",
                "+91 98765 43210"
        );

        seedUser(
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                "staff@demo.jklu.test",
                "Dr. Neha Verma",
                "STAFF",
                "Electronics & Communication",
                "+91 98765 43211"
        );

        seedUser(
                UUID.fromString("00000000-0000-0000-0000-000000000003"),
                "admin@demo.jklu.test",
                "Rajesh Patel",
                "ADMIN",
                "Estate & Campus Operations",
                "+91 98765 43212"
        );

        log.info("Demo users synced (demo profile)");

        // 2. Seed Extended Demo Campus Data
        seedParkingZones();
        seedRooms();
        seedGuestRooms();
        seedTimetableSlots();
        seedBookings();
        seedAuditLogs();

        // 3. Log verified row counts
        logRowCounts();
    }

    private void seedUser(UUID id, String email, String name, String role, String department, String phone) {
        jdbcTemplate.update(
                """
                INSERT INTO users (id, email, password_hash, full_name, role, department, phone)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (email) DO UPDATE SET
                    password_hash = EXCLUDED.password_hash,
                    full_name = EXCLUDED.full_name,
                    role = EXCLUDED.role,
                    department = EXCLUDED.department,
                    phone = EXCLUDED.phone
                """,
                id, email, passwordEncoder.encode(demoPassword), name, role, department, phone
        );
    }

    private void seedParkingZones() {
        jdbcTemplate.execute("""
            INSERT INTO parking_zones (id, name, code, total_slots, active) VALUES
            ('00000000-0000-0000-0000-000000000043', 'Student & General Parking', 'P-STUDENT', 50, TRUE)
            ON CONFLICT (code) DO NOTHING
        """);
    }

    private void seedRooms() {
        jdbcTemplate.execute("""
            INSERT INTO rooms (id, block_id, room_number, name, type, capacity, features, active, bookable_outside) VALUES
            ('00000000-0000-0000-0000-000000000051', '00000000-0000-0000-0000-000000000011', 'A-103', 'Classroom 103', 'CLASSROOM', 50, ARRAY['projector', 'whiteboard'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000052', '00000000-0000-0000-0000-000000000011', 'A-104', 'Classroom 104', 'CLASSROOM', 50, ARRAY['projector', 'ac'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000053', '00000000-0000-0000-0000-000000000011', 'A-203', 'Tutorial Room 203', 'CLASSROOM', 30, ARRAY['whiteboard'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000054', '00000000-0000-0000-0000-000000000011', 'A-204', 'Seminar Room 204', 'CLASSROOM', 40, ARRAY['projector', 'ac', 'mic'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000055', '00000000-0000-0000-0000-000000000011', 'A-301', 'Faculty Board Room', 'CLASSROOM', 25, ARRAY['projector', 'ac', 'audio_system'], TRUE, FALSE),
            ('00000000-0000-0000-0000-000000000056', '00000000-0000-0000-0000-000000000012', 'T-103', 'Robotics & IoT Lab', 'LAB', 30, ARRAY['computers', 'lan', 'sensors', 'ac'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000057', '00000000-0000-0000-0000-000000000012', 'T-202', 'Tech Classroom 202', 'CLASSROOM', 60, ARRAY['projector', 'smartboard', 'ac'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000058', '00000000-0000-0000-0000-000000000012', 'T-203', 'Tech Classroom 203', 'CLASSROOM', 60, ARRAY['projector', 'ac'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000059', '00000000-0000-0000-0000-000000000012', 'T-301', 'Innovation Studio', 'CLASSROOM', 45, ARRAY['projector', 'smartboard', 'ac'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000060', '00000000-0000-0000-0000-000000000012', 'T-302', 'Cloud Computing Lab', 'LAB', 40, ARRAY['computers', 'lan', 'ac', 'projector'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000061', '00000000-0000-0000-0000-000000000013', 'M-103', 'Analytics & Finance Lab', 'LAB', 35, ARRAY['computers', 'lan', 'ac'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000062', '00000000-0000-0000-0000-000000000013', 'M-104', 'Executive Lecture Hall', 'CLASSROOM', 70, ARRAY['projector', 'ac', 'audio_system'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000063', '00000000-0000-0000-0000-000000000013', 'M-202', 'Case Study Room 202', 'CLASSROOM', 45, ARRAY['projector', 'ac'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000064', '00000000-0000-0000-0000-000000000013', 'M-203', 'Group Discussion Room', 'CLASSROOM', 20, ARRAY['whiteboard', 'ac'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000065', '00000000-0000-0000-0000-000000000013', 'M-301', 'Dean Conference Room', 'CLASSROOM', 30, ARRAY['projector', 'ac', 'smartboard'], TRUE, FALSE)
            ON CONFLICT (block_id, room_number) DO NOTHING
        """);
    }

    private void seedGuestRooms() {
        jdbcTemplate.execute("""
            INSERT INTO guest_rooms (id, room_number, type, capacity, price_per_night, active, features) VALUES
            ('00000000-0000-0000-0000-000000000071', 'G-103', 'STANDARD', 2, 1200.00, TRUE, ARRAY['wifi', 'ac', 'tv']),
            ('00000000-0000-0000-0000-000000000072', 'G-104', 'STANDARD', 2, 1200.00, TRUE, ARRAY['wifi', 'ac', 'tv']),
            ('00000000-0000-0000-0000-000000000073', 'G-203', 'DELUXE',   2, 2000.00, TRUE, ARRAY['wifi', 'ac', 'tv', 'mini_fridge']),
            ('00000000-0000-0000-0000-000000000074', 'G-204', 'DELUXE',   2, 2000.00, TRUE, ARRAY['wifi', 'ac', 'tv', 'mini_fridge']),
            ('00000000-0000-0000-0000-000000000075', 'G-301', 'SUITE',    4, 3500.00, TRUE, ARRAY['wifi', 'ac', 'tv', 'mini_fridge', 'balcony']),
            ('00000000-0000-0000-0000-000000000076', 'G-302', 'SUITE',    4, 3500.00, TRUE, ARRAY['wifi', 'ac', 'tv', 'mini_fridge', 'living_area']),
            ('00000000-0000-0000-0000-000000000077', 'G-303', 'STANDARD', 1, 1000.00, TRUE, ARRAY['wifi', 'ac', 'tv']),
            ('00000000-0000-0000-0000-000000000078', 'G-304', 'DELUXE',   3, 2200.00, TRUE, ARRAY['wifi', 'ac', 'tv', 'mini_fridge'])
            ON CONFLICT (room_number) DO NOTHING
        """);
    }

    private void seedTimetableSlots() {
        jdbcTemplate.execute("""
            INSERT INTO timetable_slots (id, room_id, day_of_week, start_minute, end_minute, course_code, section, faculty, academic_term, active) VALUES
            ('00000000-0000-0000-0000-000000000081', '00000000-0000-0000-0000-000000000021', 1, 540, 600, 'CS201', 'Sec A', 'Dr. Neha Verma', 'Spring 2027', TRUE),
            ('00000000-0000-0000-0000-000000000082', '00000000-0000-0000-0000-000000000021', 1, 660, 720, 'CS203', 'Sec A', 'Prof. Sharma', 'Spring 2027', TRUE),
            ('00000000-0000-0000-0000-000000000083', '00000000-0000-0000-0000-000000000022', 1, 600, 660, 'MA201', 'Sec B', 'Dr. K. S. Rao', 'Spring 2027', TRUE),
            ('00000000-0000-0000-0000-000000000084', '00000000-0000-0000-0000-000000000025', 1, 600, 720, 'CS205L', 'Lab 1', 'Dr. Amit Sengupta', 'Spring 2027', TRUE),
            ('00000000-0000-0000-0000-000000000085', '00000000-0000-0000-0000-000000000021', 2, 540, 600, 'CS301', 'Sec A', 'Dr. Neha Verma', 'Spring 2027', TRUE),
            ('00000000-0000-0000-0000-000000000086', '00000000-0000-0000-0000-000000000022', 2, 660, 720, 'EC202', 'Sec B', 'Prof. Guha', 'Spring 2027', TRUE),
            ('00000000-0000-0000-0000-000000000087', '00000000-0000-0000-0000-000000000026', 2, 840, 960, 'CS302L', 'Lab 2', 'Prof. Verma', 'Spring 2027', TRUE),
            ('00000000-0000-0000-0000-000000000088', '00000000-0000-0000-0000-000000000028', 3, 600, 720, 'MB101', 'MBA A', 'Dr. Priya Nair', 'Spring 2027', TRUE),
            ('00000000-0000-0000-0000-000000000089', '00000000-0000-0000-0000-000000000029', 3, 720, 840, 'MB102', 'MBA B', 'Dr. Sengupta', 'Spring 2027', TRUE),
            ('00000000-0000-0000-0000-000000000090', '00000000-0000-0000-0000-000000000021', 4, 600, 660, 'CS305', 'Sec A', 'Prof. Sharma', 'Spring 2027', TRUE),
            ('00000000-0000-0000-0000-000000000091', '00000000-0000-0000-0000-000000000027', 4, 840, 960, 'EE201', 'Tech A', 'Dr. Raman', 'Spring 2027', TRUE),
            ('00000000-0000-0000-0000-000000000092', '00000000-0000-0000-0000-000000000023', 5, 900, 1020, 'GEN100', 'All UG', 'Dean Academics', 'Spring 2027', TRUE),
            ('00000000-0000-0000-0000-000000000093', '00000000-0000-0000-0000-000000000024', 5, 600, 720, 'CS401', 'Sec Final', 'Dr. Neha Verma', 'Spring 2027', TRUE)
            ON CONFLICT (id) DO NOTHING
        """);
    }

    private void seedBookings() {
        jdbcTemplate.execute("""
            INSERT INTO bookings (id, booking_number, user_id, title, description, status, purpose, expected_attendees, created_at) VALUES
            ('00000000-0000-0000-0000-000000000101', 'BK-2026-001', '00000000-0000-0000-0000-000000000001', 'Robotics Club Meeting', 'Weekly club sync', 'CONFIRMED', 'Club Activity', 25, NOW() - INTERVAL '10 days'),
            ('00000000-0000-0000-0000-000000000102', 'BK-2026-002', '00000000-0000-0000-0000-000000000001', 'Hackathon Prep Session', 'Coding night prep', 'PENDING_APPROVAL', 'Workshop', 40, NOW() - INTERVAL '8 days'),
            ('00000000-0000-0000-0000-000000000103', 'BK-2026-003', '00000000-0000-0000-0000-000000000002', 'Guest Lecture on AI', 'International guest speaker', 'CONFIRMED', 'Guest Lecture', 100, NOW() - INTERVAL '7 days'),
            ('00000000-0000-0000-0000-000000000104', 'BK-2026-004', '00000000-0000-0000-0000-000000000002', 'Faculty Development Program', 'Two-day faculty training', 'CONFIRMED', 'Faculty Training', 30, NOW() - INTERVAL '6 days'),
            ('00000000-0000-0000-0000-000000000105', 'BK-2026-005', '00000000-0000-0000-0000-000000000001', 'Cultural Fest Rehearsal', 'Dance group rehearsal', 'REJECTED', 'Cultural Event', 20, NOW() - INTERVAL '5 days'),
            ('00000000-0000-0000-0000-000000000106', 'BK-2026-006', '00000000-0000-0000-0000-000000000001', 'Study Group Session', 'Math study group', 'CANCELLED', 'Academic', 10, NOW() - INTERVAL '4 days'),
            ('00000000-0000-0000-0000-000000000107', 'BK-2026-007', '00000000-0000-0000-0000-000000000002', 'Alumni Talk Session', 'Alumni guidance lecture', 'CONFIRMED', 'Alumni Event', 80, NOW() - INTERVAL '3 days'),
            ('00000000-0000-0000-0000-000000000108', 'BK-2026-008', '00000000-0000-0000-0000-000000000001', 'Design Thinking Workshop', 'Student council workshop', 'PENDING_APPROVAL', 'Workshop', 35, NOW() - INTERVAL '2 days'),
            ('00000000-0000-0000-0000-000000000109', 'BK-2026-009', '00000000-0000-0000-0000-000000000002', 'Board of Studies Meeting', 'Curriculum review', 'CONFIRMED', 'Meeting', 15, NOW() - INTERVAL '1 day'),
            ('00000000-0000-0000-0000-000000000110', 'BK-2026-010', '00000000-0000-0000-0000-000000000001', 'Music Society Jamming', 'Acoustic practice', 'PENDING_APPROVAL', 'Club Activity', 12, NOW() - INTERVAL '12 hours'),
            ('00000000-0000-0000-0000-000000000111', 'BK-2026-011', '00000000-0000-0000-0000-000000000002', 'External Examiner Visit', 'PhD Viva Defense', 'CONFIRMED', 'Examination', 20, NOW() - INTERVAL '10 hours'),
            ('00000000-0000-0000-0000-000000000112', 'BK-2026-012', '00000000-0000-0000-0000-000000000001', 'Placement Orientation', 'Mock interview batch', 'PENDING_APPROVAL', 'Placement', 50, NOW() - INTERVAL '8 hours'),
            ('00000000-0000-0000-0000-000000000113', 'BK-2026-013', '00000000-0000-0000-0000-000000000002', 'Industry Partnership Sync', 'Corporate MoU signing', 'CONFIRMED', 'Meeting', 10, NOW() - INTERVAL '6 hours'),
            ('00000000-0000-0000-0000-000000000114', 'BK-2026-014', '00000000-0000-0000-0000-000000000001', 'Debate Society Semi-Finals', 'Inter-batch debate', 'REJECTED', 'Competition', 30, NOW() - INTERVAL '5 hours'),
            ('00000000-0000-0000-0000-000000000115', 'BK-2026-015', '00000000-0000-0000-0000-000000000001', 'Photography Exhibition', 'Campus life gallery', 'PENDING_APPROVAL', 'Exhibition', 60, NOW() - INTERVAL '4 hours'),
            ('00000000-0000-0000-0000-000000000116', 'BK-2026-016', '00000000-0000-0000-0000-000000000002', 'Visiting Faculty Stay', 'Accommodation for guest prof', 'CONFIRMED', 'Guest House', 2, NOW() - INTERVAL '3 hours'),
            ('00000000-0000-0000-0000-000000000117', 'BK-2026-017', '00000000-0000-0000-0000-000000000001', 'IEEE Student Chapter AGM', 'Annual general meeting', 'PENDING_APPROVAL', 'Meeting', 45, NOW() - INTERVAL '2 hours'),
            ('00000000-0000-0000-0000-000000000118', 'BK-2026-018', '00000000-0000-0000-0000-000000000002', 'Conference Speaker Stay', 'Stay for keynote speaker', 'CONFIRMED', 'Guest House', 1, NOW() - INTERVAL '1 hour'),
            ('00000000-0000-0000-0000-000000000119', 'BK-2026-019', '00000000-0000-0000-0000-000000000001', 'Campus Flow Demo Session', 'Testing booking capabilities', 'CONFIRMED', 'Demonstration', 15, NOW() - INTERVAL '30 minutes'),
            ('00000000-0000-0000-0000-000000000120', 'BK-2026-020', '00000000-0000-0000-0000-000000000001', 'Late Night Coding Sprint', 'Hackathon testing sprint', 'PENDING_APPROVAL', 'Academic', 20, NOW() - INTERVAL '10 minutes')
            ON CONFLICT (id) DO NOTHING
        """);

        jdbcTemplate.execute("""
            INSERT INTO booking_items (id, booking_id, resource_type, resource_id, start_time, end_time, status) VALUES
            ('00000000-0000-0000-0000-000000000201', '00000000-0000-0000-0000-000000000101', 'ROOM', '00000000-0000-0000-0000-000000000021', NOW() + INTERVAL '1 day', NOW() + INTERVAL '1 day 2 hours', 'CONFIRMED'),
            ('00000000-0000-0000-0000-000000000202', '00000000-0000-0000-0000-000000000102', 'ROOM', '00000000-0000-0000-0000-000000000025', NOW() + INTERVAL '2 days', NOW() + INTERVAL '2 days 3 hours', 'PENDING_APPROVAL'),
            ('00000000-0000-0000-0000-000000000203', '00000000-0000-0000-0000-000000000103', 'ROOM', '00000000-0000-0000-0000-000000000023', NOW() + INTERVAL '3 days', NOW() + INTERVAL '3 days 4 hours', 'CONFIRMED'),
            ('00000000-0000-0000-0000-000000000204', '00000000-0000-0000-0000-000000000116', 'GUEST_ROOM', '00000000-0000-0000-0000-000000000031', NOW() + INTERVAL '4 days', NOW() + INTERVAL '6 days', 'CONFIRMED'),
            ('00000000-0000-0000-0000-000000000205', '00000000-0000-0000-0000-000000000118', 'GUEST_ROOM', '00000000-0000-0000-0000-000000000033', NOW() + INTERVAL '5 days', NOW() + INTERVAL '7 days', 'CONFIRMED')
            ON CONFLICT (id) DO NOTHING
        """);
    }

    private void seedAuditLogs() {
        Integer existingCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_logs", Integer.class);
        if (existingCount != null && existingCount >= 50) {
            return;
        }

        jdbcTemplate.execute("""
            INSERT INTO audit_logs (id, user_id, action, entity_type, entity_id, ip_address, user_agent, created_at)
            SELECT
                gen_random_uuid(),
                CASE (i % 3)
                    WHEN 0 THEN '00000000-0000-0000-0000-000000000001'::uuid
                    WHEN 1 THEN '00000000-0000-0000-0000-000000000002'::uuid
                    ELSE '00000000-0000-0000-0000-000000000003'::uuid
                END,
                CASE (i % 6)
                    WHEN 0 THEN 'LOGIN_SUCCESS'
                    WHEN 1 THEN 'ROOM_AVAILABILITY_SEARCH'
                    WHEN 2 THEN 'BOOKING_CREATED'
                    WHEN 3 THEN 'BOOKING_APPROVED'
                    WHEN 4 THEN 'PARKING_PERMIT_REQUESTED'
                    ELSE 'TIMETABLE_UPLOAD'
                END,
                CASE (i % 4)
                    WHEN 0 THEN 'AUTH'
                    WHEN 1 THEN 'ROOM'
                    WHEN 2 THEN 'BOOKING'
                    ELSE 'TIMETABLE'
                END,
                'SEED-ENT-' || i,
                '127.0.0.1',
                'Mozilla/5.0 (Windows NT 10.0; Win64; x64) DemoSeeder/1.0',
                NOW() - (i || ' hours')::interval
            FROM generate_series(1, 62) AS i
        """);
    }

    private void logRowCounts() {
        Integer roomCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rooms", Integer.class);
        Integer slotCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM timetable_slots", Integer.class);
        Integer guestRoomCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM guest_rooms", Integer.class);
        Integer zoneCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM parking_zones", Integer.class);
        Integer bookingCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM bookings", Integer.class);
        Integer auditCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_logs", Integer.class);

        log.info("Demo seed verified row counts: rooms={}, timetable_slots={}, guest_rooms={}, parking_zones={}, bookings={}, audit_log={}",
                roomCount, slotCount, guestRoomCount, zoneCount, bookingCount, auditCount);
    }
}
