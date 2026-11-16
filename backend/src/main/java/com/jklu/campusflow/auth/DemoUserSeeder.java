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
    private final org.springframework.core.env.Environment environment;
    private final String demoPassword;

    public DemoUserSeeder(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            org.springframework.core.env.Environment environment,
            @Value("${app.demo-password:${DEMO_USER_PASSWORD:change-me-demo-password}}") String demoPassword
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
        this.demoPassword = (demoPassword == null || demoPassword.isBlank()) ? "Demo@2024" : demoPassword;
    }

    @Override
    public void run(String... args) {
        // 1. Clean out fake data (bookings, and old mock timetable slots)
        cleanupFakeData();

        // 2. Seed Campus Blocks (JKLU EB1, EB2, TB, LRC, IET, AB, MB)
        seedBlocks();

        // 3. Seed Campus Parking Zones & Guest Suites
        seedParkingZones();
        seedGuestRooms();

        // 4. Seed Real JKLU Rooms and Laboratories
        seedRooms();

        // 5. Seed Real User Logins (Student, Faculty Staff, Admin)
        seedUsers();

        // 6. Seed Real Timetable Slots from official JKLU 1st & 3rd Semester schedules
        seedTimetableSlots();

        // 7. Seed System Compliance Audit Logs
        seedAuditLogs();

        // 8. Seed Test Bookings (only in test profile for integration test suite)
        boolean isTest = environment != null && environment.matchesProfiles("test");
        if (isTest) {
            seedTestBookings();
        }

        // 9. Log Row Counts
        logRowCounts();
    }

    private void seedTestBookings() {
        jdbcTemplate.execute("""
            INSERT INTO bookings (id, booking_number, user_id, title, description, status, purpose, expected_attendees, created_at) VALUES
            ('00000000-0000-0000-0000-000000000101', 'BK-2026-001', '00000000-0000-0000-0000-000000000001', 'Robotics Club Meeting', 'Weekly club sync', 'CONFIRMED', 'Club Activity', 25, NOW() - INTERVAL '10 days'),
            ('00000000-0000-0000-0000-000000000102', 'BK-2026-002', '00000000-0000-0000-0000-000000000001', 'Hackathon Prep Session', 'Coding night prep', 'PENDING_APPROVAL', 'Workshop', 40, NOW() - INTERVAL '8 days'),
            ('00000000-0000-0000-0000-000000000108', 'BK-2026-008', '00000000-0000-0000-0000-000000000001', 'Design Thinking Workshop', 'Student council workshop', 'PENDING_APPROVAL', 'Workshop', 35, NOW() - INTERVAL '2 days')
            ON CONFLICT (id) DO NOTHING
        """);

        jdbcTemplate.execute("""
            INSERT INTO booking_items (id, booking_id, resource_type, resource_id, start_time, end_time, status) VALUES
            ('00000000-0000-0000-0000-000000000201', '00000000-0000-0000-0000-000000000101', 'ROOM', '00000000-0000-0000-0000-000000000021', NOW() + INTERVAL '1 day', NOW() + INTERVAL '1 day 2 hours', 'CONFIRMED'),
            ('00000000-0000-0000-0000-000000000202', '00000000-0000-0000-0000-000000000102', 'ROOM', '00000000-0000-0000-0000-000000000025', NOW() + INTERVAL '2 days', NOW() + INTERVAL '2 days 3 hours', 'PENDING_APPROVAL'),
            ('00000000-0000-0000-0000-000000000203', '00000000-0000-0000-0000-000000000108', 'ROOM', '00000000-0000-0000-0000-000000000023', NOW() + INTERVAL '3 days', NOW() + INTERVAL '3 days 4 hours', 'PENDING_APPROVAL')
            ON CONFLICT (id) DO NOTHING
        """);
    }

    private void cleanupFakeData() {
        if (environment != null && environment.matchesProfiles("test")) {
            return;
        }
        try {
            // Clear any old fake bookings
            jdbcTemplate.execute("DELETE FROM booking_items WHERE booking_id IN (SELECT id FROM bookings WHERE booking_number LIKE 'BK-2026-%')");
            jdbcTemplate.execute("DELETE FROM bookings WHERE booking_number LIKE 'BK-2026-%'");
            // Clear old random mock timetable slots
            jdbcTemplate.execute("DELETE FROM timetable_slots WHERE section = 'Sec A' AND academic_term = 'Spring 2027' AND course_code NOT IN ('CS201')");
            jdbcTemplate.execute("DELETE FROM timetable_slots WHERE course_code = 'CS-LAB'");
        } catch (Exception e) {
            log.debug("Cleanup notice: {}", e.getMessage());
        }
    }

    private void seedBlocks() {
        boolean isTest = environment != null && environment.matchesProfiles("test");
        String sql = String.format("""
            INSERT INTO blocks (id, name, code, active) VALUES
            ('00000000-0000-0000-0000-000000000011', 'Academic Block', 'AB', %b),
            ('00000000-0000-0000-0000-000000000012', 'Technology Block', 'TB', TRUE),
            ('00000000-0000-0000-0000-000000000013', 'Management Block', 'MB', %b),
            ('00000000-0000-0000-0000-000000000014', 'Engineering Block 1', 'EB1', TRUE),
            ('00000000-0000-0000-0000-000000000015', 'Engineering Block 2', 'EB2', TRUE),
            ('00000000-0000-0000-0000-000000000016', 'Learning Resource Centre', 'LRC', TRUE),
            ('00000000-0000-0000-0000-000000000017', 'Institute of Engineering & Technology', 'IET', TRUE)
            ON CONFLICT (code) DO UPDATE SET active = EXCLUDED.active
        """, isTest, isTest);
        jdbcTemplate.execute(sql);
    }

    private void seedParkingZones() {
        jdbcTemplate.execute("""
            INSERT INTO parking_zones (id, name, code, total_slots, active) VALUES
            ('00000000-0000-0000-0000-000000000041', 'Faculty & Staff Parking', 'P-STAFF', 60, TRUE),
            ('00000000-0000-0000-0000-000000000042', 'Visitor Parking Area', 'P-VISITOR', 40, TRUE),
            ('00000000-0000-0000-0000-000000000043', 'Student & General Parking', 'P-STUDENT', 50, TRUE)
            ON CONFLICT (code) DO NOTHING
        """);
    }

    private void seedGuestRooms() {
        jdbcTemplate.execute("""
            INSERT INTO guest_rooms (id, room_number, type, capacity, price_per_night, active, features) VALUES
            ('00000000-0000-0000-0000-000000000031', 'G-101', 'STANDARD', 2, 1200.00, TRUE, ARRAY['wifi', 'ac', 'tv']),
            ('00000000-0000-0000-0000-000000000032', 'G-102', 'STANDARD', 2, 1200.00, TRUE, ARRAY['wifi', 'ac', 'tv']),
            ('00000000-0000-0000-0000-000000000033', 'G-201', 'DELUXE',   3, 2000.00, TRUE, ARRAY['wifi', 'ac', 'tv', 'mini_fridge']),
            ('00000000-0000-0000-0000-000000000034', 'G-202', 'SUITE',    4, 3500.00, TRUE, ARRAY['wifi', 'ac', 'tv', 'mini_fridge', 'living_area']),
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

    private void seedRooms() {
        jdbcTemplate.execute("""
            INSERT INTO rooms (id, block_id, room_number, name, type, capacity, features, active, bookable_outside) VALUES
            -- Original 25 Baseline Rooms
            ('00000000-0000-0000-0000-000000000021', '00000000-0000-0000-0000-000000000011', 'A-101', 'Seminar Hall 1', 'CLASSROOM', 60, ARRAY['projector', 'ac', 'smartboard'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000022', '00000000-0000-0000-0000-000000000011', 'A-102', 'Lecture Room 102', 'CLASSROOM', 60, ARRAY['projector', 'ac'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000023', '00000000-0000-0000-0000-000000000011', 'A-201', 'Main Auditorium', 'AMPHITHEATRE', 120, ARRAY['projector', 'ac', 'audio_system', 'stage'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000024', '00000000-0000-0000-0000-000000000011', 'A-202', 'Discussion Room 202', 'CLASSROOM', 40, ARRAY['projector'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000051', '00000000-0000-0000-0000-000000000011', 'A-103', 'Classroom 103', 'CLASSROOM', 50, ARRAY['projector', 'whiteboard'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000052', '00000000-0000-0000-0000-000000000011', 'A-104', 'Classroom 104', 'CLASSROOM', 50, ARRAY['projector', 'ac'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000053', '00000000-0000-0000-0000-000000000011', 'A-203', 'Tutorial Room 203', 'CLASSROOM', 30, ARRAY['whiteboard'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000054', '00000000-0000-0000-0000-000000000011', 'A-204', 'Seminar Room 204', 'CLASSROOM', 40, ARRAY['projector', 'ac', 'mic'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000055', '00000000-0000-0000-0000-000000000011', 'A-301', 'Faculty Board Room', 'CLASSROOM', 25, ARRAY['projector', 'ac', 'audio_system'], TRUE, FALSE),

            ('00000000-0000-0000-0000-000000000025', '00000000-0000-0000-0000-000000000012', 'T-101', 'Computing Lab 1', 'LAB', 35, ARRAY['computers', 'lan', 'ac', 'projector'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000026', '00000000-0000-0000-0000-000000000012', 'T-102', 'Networks Research Lab', 'LAB', 35, ARRAY['computers', 'lan', 'ac'], TRUE, FALSE),
            ('00000000-0000-0000-0000-000000000027', '00000000-0000-0000-0000-000000000012', 'T-201', 'Tech Amphitheatre', 'CLASSROOM', 70, ARRAY['projector', 'ac', 'smartboard'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000056', '00000000-0000-0000-0000-000000000012', 'T-103', 'Robotics & IoT Lab', 'LAB', 30, ARRAY['computers', 'lan', 'sensors', 'ac'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000057', '00000000-0000-0000-0000-000000000012', 'T-202', 'Tech Classroom 202', 'CLASSROOM', 60, ARRAY['projector', 'smartboard', 'ac'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000058', '00000000-0000-0000-0000-000000000012', 'T-203', 'Tech Classroom 203', 'CLASSROOM', 60, ARRAY['projector', 'ac'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000059', '00000000-0000-0000-0000-000000000012', 'T-301', 'Innovation Studio', 'CLASSROOM', 45, ARRAY['projector', 'smartboard', 'ac'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000060', '00000000-0000-0000-0000-000000000012', 'T-302', 'Cloud Computing Lab', 'LAB', 40, ARRAY['computers', 'lan', 'ac', 'projector'], TRUE, TRUE),

            ('00000000-0000-0000-0000-000000000028', '00000000-0000-0000-0000-000000000013', 'M-101', 'Case Study Room 1', 'CLASSROOM', 50, ARRAY['projector', 'ac'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000029', '00000000-0000-0000-0000-000000000013', 'M-102', 'Case Study Room 2', 'CLASSROOM', 50, ARRAY['projector', 'ac'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000030', '00000000-0000-0000-0000-000000000013', 'M-201', 'Management Amphitheatre', 'AMPHITHEATRE', 150, ARRAY['projector', 'ac', 'audio_system', 'stage'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000061', '00000000-0000-0000-0000-000000000013', 'M-103', 'Analytics & Finance Lab', 'LAB', 35, ARRAY['computers', 'lan', 'ac'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000062', '00000000-0000-0000-0000-000000000013', 'M-104', 'Executive Lecture Hall', 'CLASSROOM', 70, ARRAY['projector', 'ac', 'audio_system'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000063', '00000000-0000-0000-0000-000000000013', 'M-202', 'Case Study Room 202', 'CLASSROOM', 45, ARRAY['projector', 'ac'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000064', '00000000-0000-0000-0000-000000000013', 'C-204', 'Lecture Hall C-204', 'CLASSROOM', 80, ARRAY['projector', 'ac', 'smartboard'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000065', '00000000-0000-0000-0000-000000000013', 'M-301', 'Dean Conference Room', 'CLASSROOM', 30, ARRAY['projector', 'ac', 'smartboard'], TRUE, FALSE),

            -- Real JKLU Engineering Block 1 (EB1) Rooms
            ('00000000-0000-0000-0000-000000000801', '00000000-0000-0000-0000-000000000014', 'EB1-002', 'Classroom EB1-002 (Sec F EC)', 'CLASSROOM', 60, ARRAY['projector', 'ac'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000802', '00000000-0000-0000-0000-000000000014', 'EB1-101', 'Classroom EB1-101 (Sec A)', 'CLASSROOM', 60, ARRAY['projector', 'ac', 'smartboard'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000803', '00000000-0000-0000-0000-000000000014', 'EB1-102', 'Classroom EB1-102 (Sec B)', 'CLASSROOM', 60, ARRAY['projector', 'ac'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000804', '00000000-0000-0000-0000-000000000014', 'EB1-103', 'Classroom EB1-103 (Sec C)', 'CLASSROOM', 60, ARRAY['projector', 'ac'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000805', '00000000-0000-0000-0000-000000000014', 'EB1-104', 'Classroom EB1-104 (Sec D)', 'CLASSROOM', 60, ARRAY['projector', 'ac', 'smartboard'], TRUE, TRUE),

            -- Real JKLU Engineering Block 2 (EB2) Rooms
            ('00000000-0000-0000-0000-000000000811', '00000000-0000-0000-0000-000000000015', 'EB2-004', 'Classroom EB2-004 (Sec A CSE)', 'CLASSROOM', 60, ARRAY['projector', 'ac', 'smartboard'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000812', '00000000-0000-0000-0000-000000000015', 'EB2-104', 'Classroom EB2-104 (Sec E)', 'CLASSROOM', 60, ARRAY['projector', 'ac'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000813', '00000000-0000-0000-0000-000000000015', 'EB2-105', 'Classroom EB2-105 (Sec F)', 'CLASSROOM', 60, ARRAY['projector', 'ac'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000814', '00000000-0000-0000-0000-000000000015', 'EB2-106', 'Classroom EB2-106 (Sec G)', 'CLASSROOM', 60, ARRAY['projector', 'ac'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000815', '00000000-0000-0000-0000-000000000015', 'EB2-202', 'Classroom EB2-202 (Sec J)', 'CLASSROOM', 60, ARRAY['projector', 'ac'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000816', '00000000-0000-0000-0000-000000000015', 'EB2-204', 'Classroom EB2-204 (Sec I)', 'CLASSROOM', 60, ARRAY['projector', 'ac'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000817', '00000000-0000-0000-0000-000000000015', 'EB2-205', 'Classroom EB2-205 (Sec H)', 'CLASSROOM', 60, ARRAY['projector', 'ac'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000818', '00000000-0000-0000-0000-000000000015', 'EB2-206', 'Classroom EB2-206 (Sec B CSE)', 'CLASSROOM', 60, ARRAY['projector', 'ac', 'smartboard'], TRUE, TRUE),

            -- Real JKLU Technology Block (TB) Labs & Classrooms
            ('00000000-0000-0000-0000-000000000821', '00000000-0000-0000-0000-000000000012', 'TB-102', 'Electronics & EE Lab 102', 'LAB', 35, ARRAY['equipment', 'ac', 'lan'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000822', '00000000-0000-0000-0000-000000000012', 'TB-104', 'Classroom TB-104 (Sec C Cyber/Data)', 'CLASSROOM', 60, ARRAY['projector', 'ac'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000823', '00000000-0000-0000-0000-000000000012', 'TB-105', 'Computer Architecture Lab 105', 'LAB', 40, ARRAY['computers', 'lan', 'ac', 'projector'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000824', '00000000-0000-0000-0000-000000000012', 'TB-106', 'Programming & Algorithms Lab 106', 'LAB', 40, ARRAY['computers', 'lan', 'ac', 'projector'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000825', '00000000-0000-0000-0000-000000000012', 'TB-107', 'Classroom TB-107 (Sec D CSAI)', 'CLASSROOM', 60, ARRAY['projector', 'ac', 'smartboard'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000826', '00000000-0000-0000-0000-000000000012', 'TB-108', 'Electrical Engineering Lab 108', 'LAB', 35, ARRAY['equipment', 'ac'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000827', '00000000-0000-0000-0000-000000000012', 'TB-109', 'Classroom TB-109 (Sec E CSAI)', 'CLASSROOM', 60, ARRAY['projector', 'ac', 'smartboard'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000828', '00000000-0000-0000-0000-000000000012', 'TB-112', 'Electronic Devices Lab 112', 'LAB', 35, ARRAY['equipment', 'ac'], TRUE, TRUE),

            -- Real JKLU Learning Resource Centre (LRC) Computing Labs
            ('00000000-0000-0000-0000-000000000831', '00000000-0000-0000-0000-000000000016', 'LRC002', 'LRC Database Systems Lab 002', 'LAB', 45, ARRAY['computers', 'lan', 'ac'], TRUE, TRUE),
            ('00000000-0000-0000-0000-000000000832', '00000000-0000-0000-0000-000000000016', 'LRC007', 'LRC Advanced Computing Lab 007', 'LAB', 45, ARRAY['computers', 'lan', 'ac', 'projector'], TRUE, TRUE),

            -- Real JKLU IET Main Amphitheatre
            ('00000000-0000-0000-0000-000000000841', '00000000-0000-0000-0000-000000000017', 'IET Amphitheatre', 'IET Main Amphitheatre', 'AMPHITHEATRE', 180, ARRAY['projector', 'ac', 'audio_system', 'stage'], TRUE, TRUE)
            ON CONFLICT (block_id, room_number) DO UPDATE SET capacity = EXCLUDED.capacity, features = EXCLUDED.features
        """);

        boolean isTest = environment != null && environment.matchesProfiles("test");
        if (!isTest) {
            // Guarantee all legacy mock rooms and blocks are purged/deactivated for live dev/demo
            jdbcTemplate.execute("""
                UPDATE rooms SET active = FALSE, bookable_outside = FALSE WHERE room_number LIKE 'A-%' OR room_number LIKE 'M-%' OR room_number LIKE 'T-%' OR room_number = 'C-204';
                UPDATE blocks SET active = FALSE WHERE code IN ('AB', 'MB');
            """);
        } else {
            jdbcTemplate.execute("""
                UPDATE rooms SET active = TRUE, bookable_outside = TRUE WHERE room_number IN ('A-101', 'A-102', 'C-204');
                UPDATE blocks SET active = TRUE WHERE code IN ('AB', 'MB');
            """);
        }
    }

    private void seedUsers() {
        seedUser(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "student@demo.jklu.test",
                "Aarav Sharma",
                "STUDENT",
                "Computer Science & Engineering",
                "+91 98765 43210"
        );

        seedUser(
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                "staff@demo.jklu.test",
                "Dr. Neha Verma",
                "STAFF",
                "Electronics & Communication Engineering",
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

        log.info("Official demo users synced (demo profile)");
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

    private void seedTimetableSlots() {
        Integer realCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM timetable_slots WHERE academic_term IN ('1st Semester', '3rd Semester')",
                Integer.class
        );
        if (realCount != null && realCount >= 400) {
            return;
        }

        // Guaranteed Hermetic Test Slot: C-204 Thursday 15:00-16:00 (Sem 3 Data Structures)
        jdbcTemplate.update("""
            INSERT INTO timetable_slots (id, room_id, day_of_week, start_minute, end_minute, course_code, section, faculty, academic_term, active)
            VALUES ('00000000-0000-0000-0000-000000000999', '00000000-0000-0000-0000-000000000064', 4, 900, 960, 'CS201', 'Sem 3 Data Structures', 'Dr. Ramanathan', 'Spring 2027', TRUE)
            ON CONFLICT (id) DO NOTHING
        """);

        log.info("Inserting official timetable slots from uploaded 1st and 3rd semester schedules...");

        // Batch 1
        try {
            jdbcTemplate.execute("""
                INSERT INTO timetable_slots (id, room_id, day_of_week, start_minute, end_minute, course_code, section, faculty, academic_term, active) VALUES
                ('df12909d-12ab-5a41-af46-730c41471338', (SELECT id FROM rooms WHERE room_number = 'IET Amphitheatre' LIMIT 1), 1, 660, 710, 'LS1108', 'Sec A', 'Dr. Neeti Kasliwal', '3rd Semester', TRUE),
                ('8544f2fe-e8de-503a-a128-e2e52ec32152', (SELECT id FROM rooms WHERE room_number = 'IET Amphitheatre' LIMIT 1), 1, 720, 770, 'LS1108', 'Sec A', 'Dr. Neeti Kasliwal', '3rd Semester', TRUE),
                ('cecc57c6-702a-57a8-9135-ba971cd406b2', (SELECT id FROM rooms WHERE room_number = 'EB2-004' LIMIT 1), 1, 840, 890, 'CS1142', 'Sec A', 'Dr. Manushi Gupta', '3rd Semester', TRUE),
                ('131bc6f2-1393-5db4-ace2-727cb08ff7c0', (SELECT id FROM rooms WHERE room_number = 'LRC007' LIMIT 1), 1, 900, 950, 'CS1133', 'Sec A (P-A1)', 'Dr. S Taruna', '3rd Semester', TRUE),
                ('34a9e78d-f205-5bf6-8f6e-545548b949ce', (SELECT id FROM rooms WHERE room_number = 'EB2-004' LIMIT 1), 1, 900, 950, 'CS1142', 'Sec A (T-A2)', 'Dr. Manushi Gupta', '3rd Semester', TRUE),
                ('f899ea95-6a1a-522d-824f-3d01ac16feaa', (SELECT id FROM rooms WHERE room_number = 'LRC007' LIMIT 1), 1, 960, 1010, 'CS1133', 'Sec A (P-A1)', 'Dr. S Taruna', '3rd Semester', TRUE),
                ('6c23ad25-bc53-5a29-8fd1-079685159af5', (SELECT id FROM rooms WHERE room_number = 'EB2-004' LIMIT 1), 2, 540, 590, 'CC1103', 'Sec A', 'Mr. Afreen Gani Faridi', '3rd Semester', TRUE),
                ('1e3d126c-2cae-5b0f-b705-520bda27bc05', (SELECT id FROM rooms WHERE room_number = 'EB2-004' LIMIT 1), 2, 600, 650, 'CS1133', 'Sec A', 'Dr. S Taruna', '3rd Semester', TRUE),
                ('b5cdb0be-7917-532b-9aa7-5209083337f5', (SELECT id FROM rooms WHERE room_number = 'TB-106' LIMIT 1), 2, 660, 710, 'CS1131', 'Sec A (P)', 'Dr. Ashish Jain', '3rd Semester', TRUE),
                ('3c4a9947-726a-5d04-a0ee-7a3c55f0a185', (SELECT id FROM rooms WHERE room_number = 'TB-106' LIMIT 1), 2, 720, 770, 'CS1131', 'Sec A (P)', 'Dr. Ashish Jain', '3rd Semester', TRUE),
                ('86e120c4-2c4e-545e-a4e3-112950cef78e', (SELECT id FROM rooms WHERE room_number = 'EB2-004' LIMIT 1), 2, 840, 890, 'CS1134', 'Sec A', 'Dr. Rohit Singh', '3rd Semester', TRUE),
                ('90cc905a-55ec-559d-ac0a-7b605c66724f', (SELECT id FROM rooms WHERE room_number = 'EB2-004' LIMIT 1), 2, 900, 950, 'CS1131', 'Sec A', 'Dr. Ashish Jain', '3rd Semester', TRUE),
                ('3ff8afd5-418d-5baf-a5de-dcc8cb534c3e', (SELECT id FROM rooms WHERE room_number = 'LRC007' LIMIT 1), 3, 540, 590, 'CS1133', 'Sec A (P-A2)', 'Dr. S Taruna', '3rd Semester', TRUE),
                ('aab782d6-ac46-5fe2-b95f-9ee6fd08a5d3', (SELECT id FROM rooms WHERE room_number = 'EB2-004' LIMIT 1), 3, 540, 590, 'CS1142', 'Sec A (T-A1)', 'Dr. Sushant Pokhriyal', '3rd Semester', TRUE),
                ('1d51d767-524d-5eb5-8c91-eadaf5cba86f', (SELECT id FROM rooms WHERE room_number = 'LRC007' LIMIT 1), 3, 600, 650, 'CS1133', 'Sec A (P-A2)', 'Dr. S Taruna', '3rd Semester', TRUE),
                ('909f3131-253e-59a6-9e43-a72dfaf70aba', (SELECT id FROM rooms WHERE room_number = 'EB2-004' LIMIT 1), 3, 660, 710, 'CS1131', 'Sec A', 'Dr. Ashish Jain', '3rd Semester', TRUE),
                ('7151579f-c38a-58c8-96b8-93d7a7b558f2', (SELECT id FROM rooms WHERE room_number = 'EB2-004' LIMIT 1), 3, 720, 770, 'CS1142', 'Sec A', 'Dr. Manushi Gupta', '3rd Semester', TRUE),
                ('81e02ac0-6dc0-5d1b-b41d-af06ba5cfd75', (SELECT id FROM rooms WHERE room_number = 'EB2-004' LIMIT 1), 3, 840, 890, 'CS1134', 'Sec A', 'Dr. Rohit Singh', '3rd Semester', TRUE),
                ('8d2c0938-07cf-5fec-a5a7-f715907672f7', (SELECT id FROM rooms WHERE room_number = 'EB2-004' LIMIT 1), 3, 900, 950, 'CS1133', 'Sec A', 'Dr. S Taruna', '3rd Semester', TRUE),
                ('15b1329d-1391-5beb-97a5-d58b0b307723', (SELECT id FROM rooms WHERE room_number = 'EB2-004' LIMIT 1), 4, 540, 590, 'CS1131', 'Sec A', 'Dr. Ashish Jain', '3rd Semester', TRUE),
                ('4bcf15de-6bfc-50ad-8aba-4319cf80903a', (SELECT id FROM rooms WHERE room_number = 'IET Amphitheatre' LIMIT 1), 4, 600, 650, 'LS1108', 'Sec A', 'Dr. Neeti Kasliwal', '3rd Semester', TRUE),
                ('92456177-0da4-5192-bf79-3d16a6bb59bf', (SELECT id FROM rooms WHERE room_number = 'TB-105' LIMIT 1), 4, 660, 710, 'CS1134', 'Sec A (P)', 'Dr. Rohit Singh / Dr. Pranab Roy', '3rd Semester', TRUE),
                ('1d7a3904-9d3d-5b4d-9eb8-1db753506ff7', (SELECT id FROM rooms WHERE room_number = 'TB-105' LIMIT 1), 4, 720, 770, 'CS1134', 'Sec A (P)', 'Dr. Rohit Singh / Dr. Pranab Roy', '3rd Semester', TRUE),
                ('79c4bb1c-9ca9-55ff-a071-f7738bbdae8a', (SELECT id FROM rooms WHERE room_number = 'EB2-004' LIMIT 1), 4, 840, 890, 'CC1103', 'Sec A', 'Mr. Afreen Gani Faridi', '3rd Semester', TRUE),
                ('42075657-e3d0-5bb5-9c04-856e7e3b3b7e', (SELECT id FROM rooms WHERE room_number = 'EB2-004' LIMIT 1), 4, 900, 950, 'CODING', 'Sec A', 'Coding Hours', '3rd Semester', TRUE),
                ('d871d412-99d4-5550-a60a-88d6a5151cb9', (SELECT id FROM rooms WHERE room_number = 'EB2-004' LIMIT 1), 4, 960, 1010, 'CODING', 'Sec A', 'Coding Hours', '3rd Semester', TRUE),
                ('78b34166-1190-5c00-979c-5e1ec7b9651a', (SELECT id FROM rooms WHERE room_number = 'EB2-004' LIMIT 1), 5, 540, 590, 'CS1134', 'Sec A', 'Dr. Rohit Singh', '3rd Semester', TRUE),
                ('a525b902-87b5-51b1-970d-30188818d04b', (SELECT id FROM rooms WHERE room_number = 'EB2-004' LIMIT 1), 5, 600, 650, 'CS1142', 'Sec A', 'Dr. Manushi Gupta', '3rd Semester', TRUE),
                ('10afe345-14d0-5ec9-b4f6-e0440d2a2287', (SELECT id FROM rooms WHERE room_number = 'EB2-004' LIMIT 1), 5, 660, 710, 'CS1133', 'Sec A', 'Dr. S Taruna', '3rd Semester', TRUE),
                ('80387352-2be2-51ff-8ead-5102006e8a2a', (SELECT id FROM rooms WHERE room_number = 'EB2-004' LIMIT 1), 5, 720, 770, 'CC1103', 'Sec A', 'Mr. Afreen Gani Faridi', '3rd Semester', TRUE)
                ON CONFLICT (id) DO NOTHING;
            """);
        } catch (Exception e) {
            log.debug("Slot batch notice: {}", e.getMessage());
        }

        // Batch 2
        try {
            jdbcTemplate.execute("""
                INSERT INTO timetable_slots (id, room_id, day_of_week, start_minute, end_minute, course_code, section, faculty, academic_term, active) VALUES
                ('fe67f1f6-17a1-56f7-9f85-36ae8ae3fee9', (SELECT id FROM rooms WHERE room_number = 'LRC007' LIMIT 1), 1, 540, 590, 'CS1133', 'Sec B (P-B1)', 'Dr. Rohit Singh', '3rd Semester', TRUE),
                ('0bd5cb80-00ef-52a8-b333-3e060b8900d2', (SELECT id FROM rooms WHERE room_number = 'EB2-206' LIMIT 1), 1, 540, 590, 'CS1142', 'Sec B (T-B2)', 'Dr. Sushant Pokhriyal', '3rd Semester', TRUE),
                ('a0db2c59-58e9-58e4-8fe6-dc5dc30d4de9', (SELECT id FROM rooms WHERE room_number = 'LRC007' LIMIT 1), 1, 600, 650, 'CS1133', 'Sec B (P-B1)', 'Dr. Rohit Singh', '3rd Semester', TRUE),
                ('1dce9983-a3b1-5a7e-876e-9a6baa3d1264', (SELECT id FROM rooms WHERE room_number = 'IET Amphitheatre' LIMIT 1), 1, 660, 710, 'LS1108', 'Sec B', 'Dr. Neeti Kasliwal', '3rd Semester', TRUE),
                ('31896a0c-a279-5d86-bb6a-9489430b5e4f', (SELECT id FROM rooms WHERE room_number = 'IET Amphitheatre' LIMIT 1), 1, 720, 770, 'LS1108', 'Sec B', 'Dr. Neeti Kasliwal', '3rd Semester', TRUE),
                ('d9134083-7172-527d-a1df-a95a80281693', (SELECT id FROM rooms WHERE room_number = 'EB2-206' LIMIT 1), 1, 840, 890, 'CS1131', 'Sec B', 'Dr. Ashish Jain', '3rd Semester', TRUE),
                ('0bfb5595-0bfc-5897-a215-404abc460629', (SELECT id FROM rooms WHERE room_number = 'TB-105' LIMIT 1), 2, 540, 590, 'CS1134', 'Sec B (P)', 'Dr. Rohit Singh / Mr. Ashutosh Kumar', '3rd Semester', TRUE),
                ('06c7f77f-e475-52a9-80f7-b2962d95f33a', (SELECT id FROM rooms WHERE room_number = 'TB-105' LIMIT 1), 2, 600, 650, 'CS1134', 'Sec B (P)', 'Dr. Rohit Singh / Mr. Ashutosh Kumar', '3rd Semester', TRUE),
                ('d907b0ca-10b8-5242-98ab-f6e03e95226a', (SELECT id FROM rooms WHERE room_number = 'EB2-206' LIMIT 1), 2, 660, 710, 'CS1142', 'Sec B', 'Dr. Manushi Gupta', '3rd Semester', TRUE),
                ('1eb5ddac-4a2d-5ca1-bfb6-9544de7372d9', (SELECT id FROM rooms WHERE room_number = 'EB2-206' LIMIT 1), 2, 720, 770, 'CS1134', 'Sec B', 'Dr. Rohit Singh', '3rd Semester', TRUE),
                ('9b26a17b-397f-5adc-a92f-e1a7efa2a433', (SELECT id FROM rooms WHERE room_number = 'EB2-206' LIMIT 1), 2, 840, 890, 'CS1131', 'Sec B', 'Dr. Ashish Jain', '3rd Semester', TRUE),
                ('e708f1c0-814e-5182-a906-39f4cec16b90', (SELECT id FROM rooms WHERE room_number = 'EB2-206' LIMIT 1), 2, 900, 950, 'CS1133', 'Sec B', 'Dr. S Taruna', '3rd Semester', TRUE),
                ('97766003-66d2-5349-a5a8-f70f49c2b5ce', (SELECT id FROM rooms WHERE room_number = 'EB2-206' LIMIT 1), 2, 960, 1010, 'CC1103', 'Sec B', 'Ms. Shiva Singh', '3rd Semester', TRUE),
                ('ba8897ca-4b49-514e-b64f-13a01abd04fd', (SELECT id FROM rooms WHERE room_number = 'EB2-206' LIMIT 1), 3, 540, 590, 'CS1134', 'Sec B', 'Dr. Rohit Singh', '3rd Semester', TRUE),
                ('3de49d8b-2379-556a-a11b-e6e70c67bb81', (SELECT id FROM rooms WHERE room_number = 'EB2-206' LIMIT 1), 3, 600, 650, 'CS1133', 'Sec B', 'Dr. S Taruna', '3rd Semester', TRUE),
                ('e945bab2-2333-5387-9076-79e72cb7c5ed', (SELECT id FROM rooms WHERE room_number = 'EB2-206' LIMIT 1), 3, 660, 710, 'CS1131', 'Sec B', 'Dr. Ashish Jain', '3rd Semester', TRUE),
                ('7df35a6e-0c8e-5bab-af9f-fd246701bbff', (SELECT id FROM rooms WHERE room_number = 'EB2-206' LIMIT 1), 3, 720, 770, 'CS1133', 'Sec B', 'Dr. S Taruna', '3rd Semester', TRUE),
                ('b560f563-149a-5ba7-bdc6-baa443c18d81', (SELECT id FROM rooms WHERE room_number = 'LRC007' LIMIT 1), 3, 840, 890, 'CS1133', 'Sec B (P-B2)', 'Dr. Rohit Singh', '3rd Semester', TRUE),
                ('f879bfd7-44cd-5885-8f59-821a0e072401', (SELECT id FROM rooms WHERE room_number = 'EB2-206' LIMIT 1), 3, 840, 890, 'CS1142', 'Sec B (T-B1)', 'Dr. Manushi Gupta', '3rd Semester', TRUE),
                ('4113bd83-15de-50f5-a0e3-391af9e4af05', (SELECT id FROM rooms WHERE room_number = 'LRC007' LIMIT 1), 3, 900, 950, 'CS1133', 'Sec B (P-B2)', 'Dr. Rohit Singh', '3rd Semester', TRUE),
                ('75e03267-054c-5ce2-91a5-3ca09e0396f2', (SELECT id FROM rooms WHERE room_number = 'EB2-206' LIMIT 1), 4, 540, 590, 'CS1142', 'Sec B', 'Dr. Manushi Gupta', '3rd Semester', TRUE),
                ('e2d53544-9068-5de5-bef1-186b8629bfa3', (SELECT id FROM rooms WHERE room_number = 'IET Amphitheatre' LIMIT 1), 4, 600, 650, 'LS1108', 'Sec B', 'Dr. Neeti Kasliwal', '3rd Semester', TRUE),
                ('93371ff9-b1fb-5d13-bee6-4f2c5aa53412', (SELECT id FROM rooms WHERE room_number = 'TB-106' LIMIT 1), 4, 660, 710, 'CS1131', 'Sec B (P)', 'Dr. Ashish Jain', '3rd Semester', TRUE),
                ('b1880328-c325-5e13-b786-5000ab63c9a9', (SELECT id FROM rooms WHERE room_number = 'TB-106' LIMIT 1), 4, 720, 770, 'CS1131', 'Sec B (P)', 'Dr. Ashish Jain', '3rd Semester', TRUE),
                ('c6f69f40-2f92-5f67-9b0c-f9bb5b65ade4', (SELECT id FROM rooms WHERE room_number = 'EB2-206' LIMIT 1), 4, 840, 890, 'CC1103', 'Sec B', 'Mr. Shiva Singh', '3rd Semester', TRUE),
                ('5f85b127-40f0-5984-a80b-d758822c1a2a', (SELECT id FROM rooms WHERE room_number = 'EB2-206' LIMIT 1), 4, 900, 950, 'CODING', 'Sec B', 'Coding Hours', '3rd Semester', TRUE),
                ('bb3423c5-3e64-5017-aa1f-cdf023fae174', (SELECT id FROM rooms WHERE room_number = 'EB2-206' LIMIT 1), 4, 960, 1010, 'CODING', 'Sec B', 'Coding Hours', '3rd Semester', TRUE),
                ('1545b3a0-b520-5645-b856-0fbb364d7450', (SELECT id FROM rooms WHERE room_number = 'EB2-206' LIMIT 1), 5, 540, 590, 'CS1134', 'Sec B', 'Dr. Rohit Singh', '3rd Semester', TRUE),
                ('e4208b3d-f157-50f9-b2d7-eefcca9d94d2', (SELECT id FROM rooms WHERE room_number = 'EB2-206' LIMIT 1), 5, 600, 650, 'CS1142', 'Sec B', 'Dr. Manushi Gupta', '3rd Semester', TRUE),
                ('f2f124a8-1722-596c-bc33-56eadb55426e', (SELECT id FROM rooms WHERE room_number = 'EB2-206' LIMIT 1), 5, 660, 710, 'CS1133', 'Sec B', 'Dr. S Taruna', '3rd Semester', TRUE)
                ON CONFLICT (id) DO NOTHING;
            """);
        } catch (Exception e) {
            log.debug("Slot batch notice: {}", e.getMessage());
        }

        // Batch 3
        try {
            jdbcTemplate.execute("""
                INSERT INTO timetable_slots (id, room_id, day_of_week, start_minute, end_minute, course_code, section, faculty, academic_term, active) VALUES
                ('14f58a22-9e05-5581-b356-9dba9d9a28bb', (SELECT id FROM rooms WHERE room_number = 'EB2-206' LIMIT 1), 5, 720, 770, 'CC1103', 'Sec B', 'Ms. Shiva Singh', '3rd Semester', TRUE),
                ('72eac136-5f49-53e4-8536-857623413a80', (SELECT id FROM rooms WHERE room_number = 'TB-104' LIMIT 1), 1, 540, 590, 'CS1142', 'Sec C', 'Dr. Umesh Gupta', '3rd Semester', TRUE),
                ('ef489583-827f-5c1d-9d2c-8369eb019513', (SELECT id FROM rooms WHERE room_number = 'TB-104' LIMIT 1), 1, 600, 650, 'CS1131', 'Sec C', 'Dr. Satveer Singh', '3rd Semester', TRUE),
                ('d4eddccf-c13a-5bac-a72e-4d95f4a1bbfb', (SELECT id FROM rooms WHERE room_number = 'TB-104' LIMIT 1), 1, 660, 710, 'CS1133', 'Sec C', 'Dr. Deepika Prakash', '3rd Semester', TRUE),
                ('92ff8a08-c747-588f-9035-185a7d214319', (SELECT id FROM rooms WHERE room_number = 'IET Amphitheatre' LIMIT 1), 1, 840, 890, 'LS1108', 'Sec C', 'Dr. Neeti Kasliwal', '3rd Semester', TRUE),
                ('c6d02ee8-ba97-594d-8661-c8d591ed29dc', (SELECT id FROM rooms WHERE room_number = 'TB-104' LIMIT 1), 1, 900, 950, 'CS1134', 'Sec C', 'Dr. Pranab Roy', '3rd Semester', TRUE),
                ('489dedd1-c7f9-512e-8d9d-187608093508', (SELECT id FROM rooms WHERE room_number = 'TB-104' LIMIT 1), 1, 960, 1010, 'CC1103', 'Sec C', 'Mr. Afreen Gani Faridi', '3rd Semester', TRUE),
                ('4adcbc83-0a43-53cb-a947-ab029e57e8ce', (SELECT id FROM rooms WHERE room_number = 'LRC007' LIMIT 1), 2, 540, 590, 'CS1133', 'Sec C (P-C1)', 'Dr. Deepika Prakash', '3rd Semester', TRUE),
                ('69447ab3-85fd-5e83-a272-0e0dbac6947e', (SELECT id FROM rooms WHERE room_number = 'TB-104' LIMIT 1), 2, 540, 590, 'CS1142', 'Sec C (T-C2)', 'Dr. Anshu Yadav', '3rd Semester', TRUE),
                ('fbdf9529-dc8e-572d-9951-76fa053214cf', (SELECT id FROM rooms WHERE room_number = 'LRC007' LIMIT 1), 2, 600, 650, 'CS1133', 'Sec C (P-C1)', 'Dr. Deepika Prakash', '3rd Semester', TRUE),
                ('7bee51f1-87bb-5875-b3e6-bdc8f62ccb44', (SELECT id FROM rooms WHERE room_number = 'TB-104' LIMIT 1), 2, 660, 710, 'CS1142', 'Sec C', 'Dr. Umesh Gupta', '3rd Semester', TRUE),
                ('f8e756d5-ab10-5e4e-b8fe-5150f4ca6daf', (SELECT id FROM rooms WHERE room_number = 'IET Amphitheatre' LIMIT 1), 2, 720, 770, 'LS1108', 'Sec C', 'Dr. Neeti Kasliwal', '3rd Semester', TRUE),
                ('9a195a28-a6f1-5403-b8f9-b5bfc2c475e3', (SELECT id FROM rooms WHERE room_number = 'TB-104' LIMIT 1), 3, 600, 650, 'CS1134', 'Sec C', 'Dr. Pranab Roy', '3rd Semester', TRUE),
                ('28ff2aec-32d2-5ec3-bce0-fa9ec9825c29', (SELECT id FROM rooms WHERE room_number = 'TB-104' LIMIT 1), 3, 660, 710, 'CS1133', 'Sec C', 'Dr. Deepika Prakash', '3rd Semester', TRUE),
                ('581e8695-297b-5ceb-8cfb-102d240c775b', (SELECT id FROM rooms WHERE room_number = 'TB-104' LIMIT 1), 3, 720, 770, 'CC1103', 'Sec C', 'Mr. Afreen Gani Faridi', '3rd Semester', TRUE),
                ('2a46be86-04eb-57eb-b471-08ca3de2bbd1', (SELECT id FROM rooms WHERE room_number = 'TB-104' LIMIT 1), 3, 840, 890, 'CS1131', 'Sec C', 'Dr. Satveer Singh', '3rd Semester', TRUE),
                ('793600f2-41ea-5832-ac64-80dbe8c2c194', (SELECT id FROM rooms WHERE room_number = 'TB-105' LIMIT 1), 4, 540, 590, 'CS1131', 'Sec C (P)', 'Dr. Satveer Singh', '3rd Semester', TRUE),
                ('1469cd05-bf92-51f1-9bc3-42f21058a432', (SELECT id FROM rooms WHERE room_number = 'TB-105' LIMIT 1), 4, 600, 650, 'CS1131', 'Sec C (P)', 'Dr. Satveer Singh', '3rd Semester', TRUE),
                ('f58a1851-9bcd-527c-ae4a-980f25792a4d', (SELECT id FROM rooms WHERE room_number = 'TB-104' LIMIT 1), 4, 660, 710, 'CS1133', 'Sec C', 'Dr. Deepika Prakash', '3rd Semester', TRUE),
                ('99d5e506-e351-54b7-9eef-527b8cfca910', (SELECT id FROM rooms WHERE room_number = 'IET Amphitheatre' LIMIT 1), 4, 720, 770, 'LS1108', 'Sec C', 'Dr. Neeti Kasliwal', '3rd Semester', TRUE),
                ('7d135fd7-f2d1-5c24-a417-9c6869aee2d8', (SELECT id FROM rooms WHERE room_number = 'TB-104' LIMIT 1), 4, 840, 890, 'CS1142', 'Sec C', 'Dr. Umesh Gupta', '3rd Semester', TRUE),
                ('35fc0d99-88c1-5284-9b2f-665fffbff4f2', (SELECT id FROM rooms WHERE room_number = 'TB-104' LIMIT 1), 4, 900, 950, 'CODING', 'Sec C', 'Coding Hours', '3rd Semester', TRUE),
                ('2c8953c1-9413-5bcd-a3fa-9146f0bb4a33', (SELECT id FROM rooms WHERE room_number = 'TB-104' LIMIT 1), 4, 960, 1010, 'CODING', 'Sec C', 'Coding Hours', '3rd Semester', TRUE),
                ('bad6732e-d3ad-5c8a-acd9-ff4bc14a7e99', (SELECT id FROM rooms WHERE room_number = 'LRC007' LIMIT 1), 5, 540, 590, 'CS1133', 'Sec C (P-C2)', 'Dr. Deepika Prakash', '3rd Semester', TRUE),
                ('5000e5cc-07be-5b9d-9fe6-2d3379b8dd6a', (SELECT id FROM rooms WHERE room_number = 'TB-104' LIMIT 1), 5, 540, 590, 'CS1142', 'Sec C (T-C1)', 'Dr. Suprita Talnikar', '3rd Semester', TRUE),
                ('32d2db16-e3b8-503d-9bb7-8d55f9095040', (SELECT id FROM rooms WHERE room_number = 'LRC007' LIMIT 1), 5, 600, 650, 'CS1133', 'Sec C (P-C2)', 'Dr. Deepika Prakash', '3rd Semester', TRUE),
                ('745a5500-9d8a-54a5-81e7-0362f15ea8dd', (SELECT id FROM rooms WHERE room_number = 'TB-104' LIMIT 1), 5, 660, 710, 'CC1103', 'Sec C', 'Mr. Afreen Gani Faridi', '3rd Semester', TRUE),
                ('9a60d6e2-c7a2-5375-9bca-7dbaf8a10cb4', (SELECT id FROM rooms WHERE room_number = 'TB-104' LIMIT 1), 5, 720, 770, 'CS1134', 'Sec C', 'Dr. Pranab Roy', '3rd Semester', TRUE),
                ('97ac378e-922a-52a6-aff6-3ac2929cf148', (SELECT id FROM rooms WHERE room_number = 'TB-104' LIMIT 1), 5, 840, 890, 'CS1131', 'Sec C', 'Dr. Satveer Singh', '3rd Semester', TRUE),
                ('74ec928d-fd93-565b-bf57-8016e9b80a79', (SELECT id FROM rooms WHERE room_number = 'TB-105' LIMIT 1), 5, 900, 950, 'CS1134', 'Sec C (P)', 'Dr. Pranab Roy / Mr. Ashutosh Kumar', '3rd Semester', TRUE)
                ON CONFLICT (id) DO NOTHING;
            """);
        } catch (Exception e) {
            log.debug("Slot batch notice: {}", e.getMessage());
        }

        // Batch 4
        try {
            jdbcTemplate.execute("""
                INSERT INTO timetable_slots (id, room_id, day_of_week, start_minute, end_minute, course_code, section, faculty, academic_term, active) VALUES
                ('e53f77d2-7797-572f-9328-199808776826', (SELECT id FROM rooms WHERE room_number = 'TB-105' LIMIT 1), 5, 960, 1010, 'CS1134', 'Sec C (P)', 'Dr. Pranab Roy / Mr. Ashutosh Kumar', '3rd Semester', TRUE),
                ('9863533e-8dd5-530b-b601-c2b54b3f4343', (SELECT id FROM rooms WHERE room_number = 'TB-107' LIMIT 1), 1, 540, 590, 'CC1103', 'Sec D', 'Mr. Afreen Gani Faridi', '3rd Semester', TRUE),
                ('032e4ee1-4e36-5bcb-a737-d252648236a1', (SELECT id FROM rooms WHERE room_number = 'TB-107' LIMIT 1), 1, 600, 650, 'CS1133', 'Sec D', 'Ms. Supriya Mishra', '3rd Semester', TRUE),
                ('746c5328-9efb-5e2b-aac1-07a071cfc58c', (SELECT id FROM rooms WHERE room_number = 'LRC007' LIMIT 1), 1, 660, 710, 'CS1133', 'Sec D (P-D1)', 'Ms. Supriya Mishra', '3rd Semester', TRUE),
                ('d7f88abb-7d08-5d14-8b78-82eb02275fae', (SELECT id FROM rooms WHERE room_number = 'LRC007' LIMIT 1), 1, 720, 770, 'CS1133', 'Sec D (P-D1)', 'Ms. Supriya Mishra', '3rd Semester', TRUE),
                ('d64c5b30-62c5-5e90-a077-474573c10d2a', (SELECT id FROM rooms WHERE room_number = 'TB-107' LIMIT 1), 1, 720, 770, 'CS1142', 'Sec D (T-D2)', 'Dr. Sushant Pokhriyal', '3rd Semester', TRUE),
                ('ed94f312-5056-56bc-a590-595500c2ff5d', (SELECT id FROM rooms WHERE room_number = 'IET Amphitheatre' LIMIT 1), 1, 840, 890, 'LS1108', 'Sec D', 'Dr. Neeti Kasliwal', '3rd Semester', TRUE),
                ('14bfefa0-ae67-54d3-8ef0-108c3d8b8b26', (SELECT id FROM rooms WHERE room_number = 'TB-107' LIMIT 1), 1, 900, 950, 'CS1131', 'Sec D', 'Dr. Anamika Satrawala', '3rd Semester', TRUE),
                ('82b30e8f-6169-5084-8e06-8218f09bd6ca', (SELECT id FROM rooms WHERE room_number = 'TB-107' LIMIT 1), 2, 660, 710, 'CS1142', 'Sec D', 'Dr. Sushant Pokhriyal', '3rd Semester', TRUE),
                ('0a3ea3d2-9c2c-5404-b9b4-6656ca17f37c', (SELECT id FROM rooms WHERE room_number = 'IET Amphitheatre' LIMIT 1), 2, 720, 770, 'LS1108', 'Sec D', 'Dr. Neeti Kasliwal', '3rd Semester', TRUE),
                ('e3f49931-7707-5dea-bd09-65ac12336a3c', (SELECT id FROM rooms WHERE room_number = 'TB-107' LIMIT 1), 2, 840, 890, 'CC1103', 'Sec D', 'Mr. Afreen Gani Faridi', '3rd Semester', TRUE),
                ('8fe5080d-6591-5ccf-b472-8e5cdc9e9995', (SELECT id FROM rooms WHERE room_number = 'TB-105' LIMIT 1), 2, 900, 950, 'CS1134', 'Sec D (P)', 'Mr. Divyanshu Jain / Mr. Ashutosh Kumar', '3rd Semester', TRUE),
                ('747da442-7686-5bac-8997-67f90c3f8e78', (SELECT id FROM rooms WHERE room_number = 'TB-105' LIMIT 1), 2, 960, 1010, 'CS1134', 'Sec D (P)', 'Mr. Divyanshu Jain / Mr. Ashutosh Kumar', '3rd Semester', TRUE),
                ('dead67f1-d5ce-535f-a724-77fa0c90d4ee', (SELECT id FROM rooms WHERE room_number = 'TB-106' LIMIT 1), 3, 660, 710, 'CS1131', 'Sec D (P)', 'Dr. Anamika Satrawala', '3rd Semester', TRUE),
                ('feda8d6e-50c9-532c-ac3d-7fbb258dd863', (SELECT id FROM rooms WHERE room_number = 'TB-106' LIMIT 1), 3, 720, 770, 'CS1131', 'Sec D (P)', 'Dr. Anamika Satrawala', '3rd Semester', TRUE),
                ('22802264-a1c0-54c2-9d72-a80a0e21d5a1', (SELECT id FROM rooms WHERE room_number = 'TB-107' LIMIT 1), 3, 840, 890, 'CS1134', 'Sec D', 'Mr. Divyanshu Jain', '3rd Semester', TRUE),
                ('063b5afe-bc02-5ef9-bb75-f00ebccf19d1', (SELECT id FROM rooms WHERE room_number = 'TB-107' LIMIT 1), 3, 900, 950, 'CS1131', 'Sec D', 'Dr. Anamika Satrawala', '3rd Semester', TRUE),
                ('25ab80bd-0136-51cb-998f-1444edd6e8aa', (SELECT id FROM rooms WHERE room_number = 'TB-107' LIMIT 1), 3, 960, 1010, 'CS1133', 'Sec D', 'Ms. Supriya Mishra', '3rd Semester', TRUE),
                ('3c80edb2-d113-5a24-8895-7a61104e0a6d', (SELECT id FROM rooms WHERE room_number = 'TB-107' LIMIT 1), 4, 660, 710, 'CS1134', 'Sec D', 'Mr. Divyanshu Jain', '3rd Semester', TRUE),
                ('70819385-2dc2-56bd-924e-99a4443db534', (SELECT id FROM rooms WHERE room_number = 'IET Amphitheatre' LIMIT 1), 4, 720, 770, 'LS1108', 'Sec D', 'Dr. Neeti Kasliwal', '3rd Semester', TRUE),
                ('af696d23-05d3-508e-ac81-94929b38f3c6', (SELECT id FROM rooms WHERE room_number = 'TB-107' LIMIT 1), 4, 840, 890, 'CS1142', 'Sec D', 'Dr. Sushant Pokhriyal', '3rd Semester', TRUE),
                ('58abb2a4-7bb8-5c2c-93c7-e1ee4774545d', (SELECT id FROM rooms WHERE room_number = 'TB-107' LIMIT 1), 4, 900, 950, 'CODING', 'Sec D', 'Coding Hours', '3rd Semester', TRUE),
                ('b1ac9867-a7c6-5bb8-b478-e0dd9df1f0cc', (SELECT id FROM rooms WHERE room_number = 'TB-107' LIMIT 1), 4, 960, 1010, 'CODING', 'Sec D', 'Coding Hours', '3rd Semester', TRUE),
                ('75e8f03a-8e51-5f3c-825c-099fde317e52', (SELECT id FROM rooms WHERE room_number = 'TB-107' LIMIT 1), 5, 540, 590, 'CS1142', 'Sec D', 'Dr. Sushant Pokhriyal', '3rd Semester', TRUE),
                ('a2c85896-0c92-5aa2-b7c0-dcb224408cc0', (SELECT id FROM rooms WHERE room_number = 'TB-107' LIMIT 1), 5, 600, 650, 'CS1134', 'Sec D', 'Mr. Divyanshu Jain', '3rd Semester', TRUE),
                ('1d453065-9ad2-5a38-a692-aaeb73a370fd', (SELECT id FROM rooms WHERE room_number = 'LRC002' LIMIT 1), 5, 660, 710, 'CS1133', 'Sec D (P-D2)', 'Ms. Supriya Mishra', '3rd Semester', TRUE),
                ('a3a73b51-a1aa-5678-a3ab-4ff7c096bd1d', (SELECT id FROM rooms WHERE room_number = 'TB-107' LIMIT 1), 5, 660, 710, 'CS1142', 'Sec D (T-D1)', 'Dr. Sushant Pokhriyal', '3rd Semester', TRUE),
                ('8faef935-df1c-589d-9750-208f6de0b6aa', (SELECT id FROM rooms WHERE room_number = 'LRC002' LIMIT 1), 5, 720, 770, 'CS1133', 'Sec D (P-D2)', 'Ms. Supriya Mishra', '3rd Semester', TRUE),
                ('ebc1859c-ffdd-5d84-b269-04559f4ca202', (SELECT id FROM rooms WHERE room_number = 'TB-107' LIMIT 1), 5, 840, 890, 'CC1103', 'Sec D', 'Mr. Afreen Gani Faridi', '3rd Semester', TRUE),
                ('b75788c8-2c22-51b1-9ac6-20555683881f', (SELECT id FROM rooms WHERE room_number = 'TB-107' LIMIT 1), 5, 900, 950, 'CS1133', 'Sec D', 'Ms. Supriya Mishra', '3rd Semester', TRUE)
                ON CONFLICT (id) DO NOTHING;
            """);
        } catch (Exception e) {
            log.debug("Slot batch notice: {}", e.getMessage());
        }

        // Batch 5
        try {
            jdbcTemplate.execute("""
                INSERT INTO timetable_slots (id, room_id, day_of_week, start_minute, end_minute, course_code, section, faculty, academic_term, active) VALUES
                ('49e6c6c5-e422-5098-96f8-6f113b51ac51', (SELECT id FROM rooms WHERE room_number = 'TB-107' LIMIT 1), 5, 960, 1010, 'CS1131', 'Sec D', 'Dr. Anamika Satrawala', '3rd Semester', TRUE),
                ('9d4f73cd-f02d-5b54-a61d-789cda3d9bed', (SELECT id FROM rooms WHERE room_number = 'TB-109' LIMIT 1), 1, 600, 650, 'CS1131', 'Sec E', 'Dr. Anamika Satrawala', '3rd Semester', TRUE),
                ('d8dadd5d-56d1-56ef-bda1-3ce748eba941', (SELECT id FROM rooms WHERE room_number = 'TB-109' LIMIT 1), 1, 720, 770, 'CC1103', 'Sec E', 'Mr. Afreen Gani Faridi', '3rd Semester', TRUE),
                ('491bfaac-0472-5334-a5ad-fb5a58b594a1', (SELECT id FROM rooms WHERE room_number = 'TB-109' LIMIT 1), 1, 840, 890, 'CS1134', 'Sec E', 'Dr. Rajnish Kumar', '3rd Semester', TRUE),
                ('19a844df-7b98-54bc-bccf-968982490465', (SELECT id FROM rooms WHERE room_number = 'TB-105' LIMIT 1), 1, 900, 950, 'CS1134', 'Sec E (P)', 'Dr. Rajnish Kumar / Mr. Ashutosh Kumar', '3rd Semester', TRUE),
                ('8a2dcb8b-bca4-5433-bffe-d29e8c34914c', (SELECT id FROM rooms WHERE room_number = 'TB-105' LIMIT 1), 1, 960, 1010, 'CS1134', 'Sec E (P)', 'Dr. Rajnish Kumar / Mr. Ashutosh Kumar', '3rd Semester', TRUE),
                ('a5258968-05d0-5aeb-93b4-ae6202101bfe', (SELECT id FROM rooms WHERE room_number = 'IET Amphitheatre' LIMIT 1), 2, 540, 590, 'LS1108', 'Sec E', 'Dr. Neeti Kasliwal', '3rd Semester', TRUE),
                ('38b977d8-1e2a-5d24-96da-c740d1c05a55', (SELECT id FROM rooms WHERE room_number = 'IET Amphitheatre' LIMIT 1), 2, 600, 650, 'LS1108', 'Sec E', 'Dr. Neeti Kasliwal', '3rd Semester', TRUE),
                ('2511b906-b686-588a-88e8-28d6154e980d', (SELECT id FROM rooms WHERE room_number = 'LRC007' LIMIT 1), 2, 660, 710, 'CS1133', 'Sec E (P-E1)', 'Ms. Supriya Mishra', '3rd Semester', TRUE),
                ('a97ebe4c-cb05-548b-8757-302b9471d454', (SELECT id FROM rooms WHERE room_number = 'TB-109' LIMIT 1), 2, 660, 710, 'CS1142', 'Sec E (T-E2)', 'Dr. Anshu Yadav', '3rd Semester', TRUE),
                ('47876c08-58bd-5c1e-b83f-71802b1aca82', (SELECT id FROM rooms WHERE room_number = 'LRC007' LIMIT 1), 2, 720, 770, 'CS1133', 'Sec E (P-E1)', 'Ms. Supriya Mishra', '3rd Semester', TRUE),
                ('861dd686-aa27-52e2-9f66-41413f8fb1a1', (SELECT id FROM rooms WHERE room_number = 'TB-109' LIMIT 1), 2, 720, 770, 'CS1142', 'Sec E (T-E1)', 'Dr. Anshu Yadav', '3rd Semester', TRUE),
                ('94d5fe25-da1b-5ba0-ad2d-9b6539c2914d', (SELECT id FROM rooms WHERE room_number = 'TB-109' LIMIT 1), 2, 840, 890, 'CS1131', 'Sec E', 'Dr. Anamika Satrawala', '3rd Semester', TRUE),
                ('e5af7d9f-d985-55d3-a964-31fdfb5d2ef9', (SELECT id FROM rooms WHERE room_number = 'LRC002' LIMIT 1), 2, 900, 950, 'CS1133', 'Sec E (P-E2)', 'Ms. Supriya Mishra', '3rd Semester', TRUE),
                ('8e9a6279-b52a-5ee9-bf8b-8db72674c4c2', (SELECT id FROM rooms WHERE room_number = 'LRC002' LIMIT 1), 2, 960, 1010, 'CS1133', 'Sec E (P-E2)', 'Ms. Supriya Mishra', '3rd Semester', TRUE),
                ('99c60d39-ad0f-574d-b942-341727f22189', (SELECT id FROM rooms WHERE room_number = 'TB-109' LIMIT 1), 3, 600, 650, 'CC1103', 'Sec E', 'Mr. Afreen Gani Faridi', '3rd Semester', TRUE),
                ('7a9d002e-ce87-52b1-a951-5c9424143954', (SELECT id FROM rooms WHERE room_number = 'TB-109' LIMIT 1), 3, 660, 710, 'CS1133', 'Sec E', 'Ms. Supriya Mishra', '3rd Semester', TRUE),
                ('ba3e3865-8203-5d7e-855a-9f06269cb02e', (SELECT id FROM rooms WHERE room_number = 'TB-109' LIMIT 1), 3, 840, 890, 'CS1142', 'Sec E', 'Dr. Anshu Yadav', '3rd Semester', TRUE),
                ('79f13876-1c41-5dce-a1aa-a7a3bb540974', (SELECT id FROM rooms WHERE room_number = 'IET Amphitheatre' LIMIT 1), 4, 540, 590, 'LS1108', 'Sec E', 'Dr. Neeti Kasliwal', '3rd Semester', TRUE),
                ('4c22a0e9-412f-52d0-af80-29168d862fdc', (SELECT id FROM rooms WHERE room_number = 'TB-109' LIMIT 1), 4, 600, 650, 'CC1103', 'Sec E', 'Mr. Afreen Gani Faridi', '3rd Semester', TRUE),
                ('e5060ebd-8d71-5196-a07c-0c88cd9bac29', (SELECT id FROM rooms WHERE room_number = 'TB-109' LIMIT 1), 4, 660, 710, 'CS1134', 'Sec E', 'Dr. Rajnish Kumar', '3rd Semester', TRUE),
                ('a95e068c-2243-5cd7-97cf-a74be45721a5', (SELECT id FROM rooms WHERE room_number = 'TB-109' LIMIT 1), 4, 720, 770, 'CS1133', 'Sec E', 'Ms. Supriya Mishra', '3rd Semester', TRUE),
                ('4866b870-03db-55e6-80a3-bad340bbc464', (SELECT id FROM rooms WHERE room_number = 'TB-109' LIMIT 1), 4, 840, 890, 'CS1142', 'Sec E', 'Dr. Anshu Yadav', '3rd Semester', TRUE),
                ('10552e16-4bb8-5dfa-8d71-bd6efe5aebc8', (SELECT id FROM rooms WHERE room_number = 'TB-109' LIMIT 1), 4, 900, 950, 'CODING', 'Sec E', 'Coding Hours', '3rd Semester', TRUE),
                ('4048e7fa-5959-5cce-b1bb-abcaa0ca336a', (SELECT id FROM rooms WHERE room_number = 'TB-109' LIMIT 1), 4, 960, 1010, 'CODING', 'Sec E', 'Coding Hours', '3rd Semester', TRUE),
                ('12c2d4fb-b8ec-518c-bd3b-121a4e1c5e6c', (SELECT id FROM rooms WHERE room_number = 'TB-109' LIMIT 1), 5, 540, 590, 'CS1142', 'Sec E', 'Dr. Anshu Yadav', '3rd Semester', TRUE),
                ('0eab172c-2c1b-577b-b386-5f4df889bf8b', (SELECT id FROM rooms WHERE room_number = 'TB-109' LIMIT 1), 5, 600, 650, 'CS1134', 'Sec E', 'Dr. Rajnish Kumar', '3rd Semester', TRUE),
                ('b0fed3bc-25f0-541d-8cb9-2f40119a70c8', (SELECT id FROM rooms WHERE room_number = 'TB-106' LIMIT 1), 5, 660, 710, 'CS1131', 'Sec E (P)', 'Dr. Anamika Satrawala', '3rd Semester', TRUE),
                ('d5f444fb-0cdd-527e-aadf-95a352a3db18', (SELECT id FROM rooms WHERE room_number = 'TB-106' LIMIT 1), 5, 720, 770, 'CS1131', 'Sec E (P)', 'Dr. Anamika Satrawala', '3rd Semester', TRUE),
                ('bfb12973-65f4-5456-bc56-cbb4cfd3cfa4', (SELECT id FROM rooms WHERE room_number = 'TB-109' LIMIT 1), 5, 840, 890, 'CS1133', 'Sec E', 'Ms. Supriya Mishra', '3rd Semester', TRUE)
                ON CONFLICT (id) DO NOTHING;
            """);
        } catch (Exception e) {
            log.debug("Slot batch notice: {}", e.getMessage());
        }

        // Batch 6
        try {
            jdbcTemplate.execute("""
                INSERT INTO timetable_slots (id, room_id, day_of_week, start_minute, end_minute, course_code, section, faculty, academic_term, active) VALUES
                ('20e95a36-f2ae-5db1-8177-bfd9d9ddaa24', (SELECT id FROM rooms WHERE room_number = 'TB-109' LIMIT 1), 5, 900, 950, 'CS1131', 'Sec E', 'Dr. Anamika Satrawala', '3rd Semester', TRUE),
                ('565683f3-93a8-5e6f-b3a3-1d8fe4c127e3', (SELECT id FROM rooms WHERE room_number = 'EB1-002' LIMIT 1), 1, 600, 650, 'CC1103', 'Sec F', 'Mr. Shiva Singh', '3rd Semester', TRUE),
                ('f36de720-3df6-57e1-8b4b-12006c734dc4', (SELECT id FROM rooms WHERE room_number = 'TB-106' LIMIT 1), 1, 660, 710, 'CS1131', 'Sec F (P)', 'Dr. Anamika Satrawal', '3rd Semester', TRUE),
                ('03147f33-ddb5-532e-8201-d31afb11754b', (SELECT id FROM rooms WHERE room_number = 'TB-106' LIMIT 1), 1, 720, 770, 'CS1131', 'Sec F (P)', 'Dr. Anamika Satrawal', '3rd Semester', TRUE),
                ('a48ce6b7-7aad-5266-ad14-807d101f1b25', (SELECT id FROM rooms WHERE room_number = 'EB1-002' LIMIT 1), 1, 840, 890, 'CS1131', 'Sec F', 'Dr. Sonali Vyas', '3rd Semester', TRUE),
                ('d52cf899-b077-5c5a-99c8-559dc3ed0a4a', (SELECT id FROM rooms WHERE room_number = 'IET Amphitheatre' LIMIT 1), 2, 540, 590, 'LS1108', 'Sec F', 'Dr. Neeti Kasliwal', '3rd Semester', TRUE),
                ('36081e48-aab1-5879-b10b-ee0ecfff89a7', (SELECT id FROM rooms WHERE room_number = 'IET Amphitheatre' LIMIT 1), 2, 600, 650, 'LS1108', 'Sec F', 'Dr. Neeti Kasliwal', '3rd Semester', TRUE),
                ('99e4d35f-0f07-5e52-a3a2-4bd25324f1c0', (SELECT id FROM rooms WHERE room_number = 'TB-112' LIMIT 1), 2, 660, 710, 'EE1126', 'Sec F (P)', 'Dr. Gaurav Mani Khanal', '3rd Semester', TRUE),
                ('d26af9f9-8afb-5d9d-abcc-d8e56cbc6e54', (SELECT id FROM rooms WHERE room_number = 'TB-112' LIMIT 1), 2, 720, 770, 'EE1126', 'Sec F (P)', 'Dr. Gaurav Mani Khanal', '3rd Semester', TRUE),
                ('cb115608-34c1-55c5-8880-56fdebe0168e', (SELECT id FROM rooms WHERE room_number = 'EB1-002' LIMIT 1), 2, 840, 890, 'EE1110', 'Sec F', 'Dr. Gaurav Mani Khanal', '3rd Semester', TRUE),
                ('f632d29d-5418-5e07-8e3d-eb80a44a8dee', (SELECT id FROM rooms WHERE room_number = 'EB1-002' LIMIT 1), 2, 900, 950, 'EE1122', 'Sec F', 'Dr. Vijaysekhar Chellaboina / Dr. H. P. Agarwal', '3rd Semester', TRUE),
                ('b97cce00-070f-578f-8491-144de2aef21b', (SELECT id FROM rooms WHERE room_number = 'EB1-002' LIMIT 1), 3, 540, 590, 'EE1126', 'Sec F', 'Dr. Gaurav Mani Khanal', '3rd Semester', TRUE),
                ('31959173-4aab-5b0d-a1ad-fa9441125317', (SELECT id FROM rooms WHERE room_number = 'EB1-002' LIMIT 1), 3, 600, 650, 'CS1131', 'Sec F', 'Dr. Sonali Vyas', '3rd Semester', TRUE),
                ('a7b57af1-51a6-52c5-b67a-21b08824ecd7', (SELECT id FROM rooms WHERE room_number = 'EB1-002' LIMIT 1), 3, 660, 710, 'EE1110', 'Sec F', 'Dr. Gaurav Mani Khanal', '3rd Semester', TRUE),
                ('2d0bdf98-8f06-5b23-a5f9-ad5c57a84eae', (SELECT id FROM rooms WHERE room_number = 'EB1-002' LIMIT 1), 3, 840, 890, 'EE1122', 'Sec F', 'Dr. Vijaysekhar Chellaboina / Dr. H. P. Agarwal', '3rd Semester', TRUE),
                ('8b714504-0e1d-5c56-a630-9850db7828e1', (SELECT id FROM rooms WHERE room_number = 'EB1-002' LIMIT 1), 3, 900, 950, 'CC1103', 'Sec F', 'Mr. Shiva Singh', '3rd Semester', TRUE),
                ('968e3265-4bef-533b-8417-3add5f37724d', (SELECT id FROM rooms WHERE room_number = 'IET Amphitheatre' LIMIT 1), 4, 540, 590, 'LS1108', 'Sec F', 'Dr. Neeti Kasliwal', '3rd Semester', TRUE),
                ('98c07306-5a0a-5e2e-99a0-8507f7ce8d85', (SELECT id FROM rooms WHERE room_number = 'EB1-002' LIMIT 1), 4, 600, 650, 'EE1122', 'Sec F', 'Dr. Vijaysekhar Chellaboina / Dr. H. P. Agarwal', '3rd Semester', TRUE),
                ('43583888-d6e7-5965-9ab7-da6070118b3a', (SELECT id FROM rooms WHERE room_number = 'EB1-002' LIMIT 1), 4, 660, 710, 'EE1110', 'Sec F', 'Dr. Gaurav Mani Khanal', '3rd Semester', TRUE),
                ('4d35704a-6b51-546f-8628-720899360efe', (SELECT id FROM rooms WHERE room_number = 'EB1-002' LIMIT 1), 4, 720, 770, 'CS1131', 'Sec F', 'Dr. Sonali Vyas', '3rd Semester', TRUE),
                ('809cc36f-54e4-5fea-af61-dea76caece7c', (SELECT id FROM rooms WHERE room_number = 'EB1-002' LIMIT 1), 4, 840, 890, 'EE1126', 'Sec F', 'Dr. Gaurav Mani Khanal', '3rd Semester', TRUE),
                ('af4fc387-c8c7-584d-9431-349218b40b55', (SELECT id FROM rooms WHERE room_number = 'EB1-002' LIMIT 1), 4, 900, 950, 'CODING', 'Sec F', 'Coding Hours', '3rd Semester', TRUE),
                ('fbd2d5c9-4c32-5eb7-8d85-2f1ce18f99db', (SELECT id FROM rooms WHERE room_number = 'EB1-002' LIMIT 1), 4, 960, 1010, 'CODING', 'Sec F', 'Coding Hours', '3rd Semester', TRUE),
                ('68594e62-74c1-5c71-ae88-f978b9f00851', (SELECT id FROM rooms WHERE room_number = 'TB-102' LIMIT 1), 5, 540, 590, 'EE1110', 'Sec F (P)', 'Dr. Gaurav Mani Khanal', '3rd Semester', TRUE),
                ('e9b2932c-27cc-5421-bdf7-bc1a9394295d', (SELECT id FROM rooms WHERE room_number = 'TB-102' LIMIT 1), 5, 600, 650, 'EE1110', 'Sec F (P)', 'Dr. Gaurav Mani Khanal', '3rd Semester', TRUE),
                ('5309536e-3d91-5638-9223-f61e88f9c5a5', (SELECT id FROM rooms WHERE room_number = 'EB1-002' LIMIT 1), 5, 660, 710, 'EE1122', 'Sec F', 'Dr. Vijaysekhar Chellaboina / Dr. H. P. Agarwal', '3rd Semester', TRUE),
                ('f7f5e91f-2000-5b93-86f9-8413b5ee7c49', (SELECT id FROM rooms WHERE room_number = 'EB1-002' LIMIT 1), 5, 720, 770, 'EE1126', 'Sec F', 'Dr. Gaurav Mani Khanal', '3rd Semester', TRUE),
                ('c37ac4ba-5650-5b5d-977e-d4ba187e6aba', (SELECT id FROM rooms WHERE room_number = 'EB1-002' LIMIT 1), 5, 840, 890, 'CC1103', 'Sec F', 'Mr. Shiva Singh', '3rd Semester', TRUE),
                ('32adde5f-2ed7-52dc-a32b-d55c273c310d', (SELECT id FROM rooms WHERE room_number = 'TB-106' LIMIT 1), 1, 540, 590, 'CS1139', '1st Sem Sec A (P)', 'Dr. Amar Nath Patra', '1st Semester', TRUE),
                ('49a9440f-5778-5b24-b512-b361d25e758c', (SELECT id FROM rooms WHERE room_number = 'TB-106' LIMIT 1), 1, 600, 650, 'CS1139', '1st Sem Sec A (P)', 'Dr. Amar Nath Patra', '1st Semester', TRUE)
                ON CONFLICT (id) DO NOTHING;
            """);
        } catch (Exception e) {
            log.debug("Slot batch notice: {}", e.getMessage());
        }

        // Batch 7
        try {
            jdbcTemplate.execute("""
                INSERT INTO timetable_slots (id, room_id, day_of_week, start_minute, end_minute, course_code, section, faculty, academic_term, active) VALUES
                ('bc76c9c1-0ad4-507f-9b37-b7a2f222ac1c', (SELECT id FROM rooms WHERE room_number = 'EB1-101' LIMIT 1), 1, 780, 830, 'CS1139', '1st Sem Sec A', 'Dr. Amar Nath Patra', '1st Semester', TRUE),
                ('6e58faf2-154b-5957-945f-5be447c0024c', (SELECT id FROM rooms WHERE room_number = 'EB1-101' LIMIT 1), 1, 840, 890, 'AS1109', '1st Sem Sec A', 'Dr. Anant Pratap Singh', '1st Semester', TRUE),
                ('8ca4cd2f-05f4-5e93-a542-f7f1adcaa69e', (SELECT id FROM rooms WHERE room_number = 'TB-102' LIMIT 1), 2, 540, 590, 'EE1118', '1st Sem Sec A (P-A2)', 'Dr. Rashmi Kushwaha', '1st Semester', TRUE),
                ('8b6241ee-f52d-524b-b04a-357b82c944d2', (SELECT id FROM rooms WHERE room_number = 'TB-102' LIMIT 1), 2, 600, 650, 'EE1118', '1st Sem Sec A (P-A2)', 'Dr. Rashmi Kushwaha', '1st Semester', TRUE),
                ('357c8486-a84b-5774-b5f1-3a473ac895ff', (SELECT id FROM rooms WHERE room_number = 'EB1-101' LIMIT 1), 2, 600, 650, 'AS1109', '1st Sem Sec A (T-A1)', 'Ms. Shobha Mangal', '1st Semester', TRUE),
                ('8fd3256f-a538-5c2f-af9f-88be31cdeeb7', (SELECT id FROM rooms WHERE room_number = 'EB1-101' LIMIT 1), 2, 660, 710, 'ES1115', '1st Sem Sec A', 'Dr. Shahnawaz Khan', '1st Semester', TRUE),
                ('bdc74ad3-3db3-5697-bed8-d9a9ab7d0588', (SELECT id FROM rooms WHERE room_number = 'EB1-101' LIMIT 1), 2, 780, 830, 'AI0000', '1st Sem Sec A', 'Dr. Devisha Tiwari', '1st Semester', TRUE),
                ('944418e7-6d3c-5be0-af6c-3db91c0e1436', (SELECT id FROM rooms WHERE room_number = 'EB1-101' LIMIT 1), 2, 840, 890, 'EE1118', '1st Sem Sec A', 'Dr. Rashmi Kushwaha', '1st Semester', TRUE),
                ('60a72161-5af4-5fa9-9c79-4a8bcd4f3dde', (SELECT id FROM rooms WHERE room_number = 'EB1-101' LIMIT 1), 2, 900, 950, 'CC1110', '1st Sem Sec A', 'Mr. Vishwajeet Sehwag', '1st Semester', TRUE),
                ('8083a7ff-5d2b-5de9-8b54-0148689ab418', (SELECT id FROM rooms WHERE room_number = 'EB1-101' LIMIT 1), 2, 960, 1010, 'PBL', '1st Sem Sec A', 'Project Based Learning', '1st Semester', TRUE),
                ('b495625b-a32b-59d6-adb2-1497ac50d371', (SELECT id FROM rooms WHERE room_number = 'EB1-101' LIMIT 1), 3, 540, 590, 'ES1115', '1st Sem Sec A', 'Dr. Shahnawaz Khan', '1st Semester', TRUE),
                ('f93f103f-e2a0-535e-8cba-738a6743252b', (SELECT id FROM rooms WHERE room_number = 'EB1-101' LIMIT 1), 3, 600, 650, 'AS1109', '1st Sem Sec A', 'Dr. Anant Pratap Singh', '1st Semester', TRUE),
                ('16eb5661-f3cf-5aa2-8db2-43c169abd2dd', (SELECT id FROM rooms WHERE room_number = 'EB1-101' LIMIT 1), 3, 660, 710, 'AI0000', '1st Sem Sec A', 'Dr. Devisha Tiwari', '1st Semester', TRUE),
                ('26fa999c-9ccd-5269-9aa2-1982bf94b34a', (SELECT id FROM rooms WHERE room_number = 'EB1-101' LIMIT 1), 3, 840, 890, 'CS1139', '1st Sem Sec A', 'Dr. Amar Nath Patra', '1st Semester', TRUE),
                ('778433b7-db4e-525b-aba3-a3b53209fc0c', (SELECT id FROM rooms WHERE room_number = 'TB-102' LIMIT 1), 3, 900, 950, 'EE1118', '1st Sem Sec A (P-A1)', 'Dr. Rashmi Kushwaha', '1st Semester', TRUE),
                ('c82ee4fd-d28e-5eba-a1a2-4b9037d4c801', (SELECT id FROM rooms WHERE room_number = 'EB1-101' LIMIT 1), 3, 900, 950, 'AS1109', '1st Sem Sec A (T-A2)', 'Dr. Anant Pratap Singh', '1st Semester', TRUE),
                ('9affd1f6-6fdb-5882-b74d-556ef132f677', (SELECT id FROM rooms WHERE room_number = 'TB-102' LIMIT 1), 3, 960, 1010, 'EE1118', '1st Sem Sec A (P-A1)', 'Dr. Rashmi Kushwaha', '1st Semester', TRUE),
                ('9fc0b61b-2f86-5afc-95f2-dfb9a4edfafb', (SELECT id FROM rooms WHERE room_number = 'EB1-101' LIMIT 1), 4, 600, 650, 'EE1118', '1st Sem Sec A', 'Dr. Rashmi Kushwaha', '1st Semester', TRUE),
                ('d1b88375-fa50-57a0-ba60-a7b06156042d', (SELECT id FROM rooms WHERE room_number = 'EB1-101' LIMIT 1), 4, 660, 710, 'CC1110', '1st Sem Sec A', 'Mr. Vishwajeet Sehwag', '1st Semester', TRUE),
                ('cfa48690-7b08-5fc2-81fa-d4a509a1d1c6', (SELECT id FROM rooms WHERE room_number = 'EB1-101' LIMIT 1), 4, 780, 830, 'AI0000', '1st Sem Sec A', 'Dr. Devisha Tiwari', '1st Semester', TRUE),
                ('b5269bbd-f1e6-500f-bafb-9c6aa062e43c', (SELECT id FROM rooms WHERE room_number = 'EB1-101' LIMIT 1), 4, 840, 890, 'AS1109', '1st Sem Sec A', 'Dr. Anant Pratap Singh', '1st Semester', TRUE),
                ('2dd71e8d-98e8-5b58-8904-0f59c4b5f6a0', (SELECT id FROM rooms WHERE room_number = 'EB1-101' LIMIT 1), 4, 900, 950, 'CODING', '1st Sem Sec A', 'Coding Hours', '1st Semester', TRUE),
                ('ffc38e2b-cebe-54b4-864e-6401b0f4596a', (SELECT id FROM rooms WHERE room_number = 'EB1-101' LIMIT 1), 4, 960, 1010, 'CODING', '1st Sem Sec A', 'Coding Hours', '1st Semester', TRUE),
                ('057cfc4d-57f9-58bd-9710-941d363be9f5', (SELECT id FROM rooms WHERE room_number = 'EB1-101' LIMIT 1), 5, 540, 590, 'CC1110', '1st Sem Sec A', 'Mr. Vishwajeet Sehwag', '1st Semester', TRUE),
                ('cc3d4c55-fe3c-5c50-bfa1-244aebc20b1a', (SELECT id FROM rooms WHERE room_number = 'EB1-101' LIMIT 1), 5, 780, 830, 'CS1139', '1st Sem Sec A', 'Dr. Amar Nath Patra', '1st Semester', TRUE),
                ('d4504158-1ab4-563d-aefa-d703dfccd9bd', (SELECT id FROM rooms WHERE room_number = 'EB1-101' LIMIT 1), 5, 840, 890, 'EE1118', '1st Sem Sec A', 'Dr. Rashmi Kushwaha', '1st Semester', TRUE),
                ('4e0f46a1-08e2-5988-a9d0-0c5d4d5bc4df', (SELECT id FROM rooms WHERE room_number = 'EB1-102' LIMIT 1), 1, 600, 650, 'EE1118', '1st Sem Sec B', 'Dr. Rajnish Kumar', '1st Semester', TRUE),
                ('88558246-5b00-521d-85b7-b7afdf5e5174', (SELECT id FROM rooms WHERE room_number = 'EB1-102' LIMIT 1), 1, 660, 710, 'AS1109', '1st Sem Sec B', 'Dr. Manushi Gupta', '1st Semester', TRUE),
                ('3a0694d6-dbe3-5351-bf93-e5b9ce3d8cd1', (SELECT id FROM rooms WHERE room_number = 'EB1-102' LIMIT 1), 1, 780, 830, 'AI0000', '1st Sem Sec B', 'Dr. Manju Rana', '1st Semester', TRUE),
                ('9c2dc2e2-6b22-5501-b56a-a4f729f243f3', (SELECT id FROM rooms WHERE room_number = 'EB1-102' LIMIT 1), 1, 840, 890, 'CS1139', '1st Sem Sec B', 'Dr. Ankush Soni', '1st Semester', TRUE)
                ON CONFLICT (id) DO NOTHING;
            """);
        } catch (Exception e) {
            log.debug("Slot batch notice: {}", e.getMessage());
        }

        // Batch 8
        try {
            jdbcTemplate.execute("""
                INSERT INTO timetable_slots (id, room_id, day_of_week, start_minute, end_minute, course_code, section, faculty, academic_term, active) VALUES
                ('8c95f62a-38e5-5dae-9840-a5a1d4f46bbf', (SELECT id FROM rooms WHERE room_number = 'EB1-102' LIMIT 1), 1, 900, 950, 'CC1110', '1st Sem Sec B', 'Mr. Vishwajeet Sehwag', '1st Semester', TRUE),
                ('4b2b3a2e-be71-5d63-b98a-7549bf675eeb', (SELECT id FROM rooms WHERE room_number = 'TB-106' LIMIT 1), 2, 540, 590, 'CS1139', '1st Sem Sec B (P)', 'Dr. Ankush Soni', '1st Semester', TRUE),
                ('346522ae-db67-5809-a7ce-12b5112537e7', (SELECT id FROM rooms WHERE room_number = 'TB-106' LIMIT 1), 2, 600, 650, 'CS1139', '1st Sem Sec B (P)', 'Dr. Ankush Soni', '1st Semester', TRUE),
                ('6b56faac-7229-5466-b7d5-96af94aafd43', (SELECT id FROM rooms WHERE room_number = 'EB1-102' LIMIT 1), 2, 780, 830, 'CC1110', '1st Sem Sec B', 'Mr. Vishwajeet Sehwag', '1st Semester', TRUE),
                ('f1bc10e1-20a2-5111-8e4d-331f2172f0be', (SELECT id FROM rooms WHERE room_number = 'EB1-102' LIMIT 1), 2, 840, 890, 'ES1115', '1st Sem Sec B', 'Dr. Shahnawaz Khan', '1st Semester', TRUE),
                ('01ce6b61-8ee6-58cc-83dc-a8e0b280a2fe', (SELECT id FROM rooms WHERE room_number = 'TB-102' LIMIT 1), 2, 900, 950, 'EE1118', '1st Sem Sec B (P-B1)', 'Dr. Rajnish Kumar', '1st Semester', TRUE),
                ('5cb9fe0b-a251-52ca-8a40-bad67cc1166a', (SELECT id FROM rooms WHERE room_number = 'EB1-102' LIMIT 1), 2, 900, 950, 'AS1109', '1st Sem Sec B (T-B2)', 'Dr. Shobha Mangal', '1st Semester', TRUE),
                ('1d5762b5-613f-5c12-86ff-6afdea9941b2', (SELECT id FROM rooms WHERE room_number = 'TB-102' LIMIT 1), 2, 960, 1010, 'EE1118', '1st Sem Sec B (P-B1)', 'Dr. Rajnish Kumar', '1st Semester', TRUE),
                ('f611ba5e-be48-5c74-a648-9f5f7aa369c7', (SELECT id FROM rooms WHERE room_number = 'EB1-102' LIMIT 1), 3, 540, 590, 'CS1139', '1st Sem Sec B', 'Dr. Ankush Soni', '1st Semester', TRUE),
                ('309baf35-da81-5850-81c8-2cf755004774', (SELECT id FROM rooms WHERE room_number = 'EB1-102' LIMIT 1), 3, 600, 650, 'AS1109', '1st Sem Sec B', 'Dr. Manushi Gupta', '1st Semester', TRUE),
                ('1a2f931e-24c8-5822-a5da-0c8ca445f9ea', (SELECT id FROM rooms WHERE room_number = 'EB1-102' LIMIT 1), 3, 660, 710, 'EE1118', '1st Sem Sec B', 'Dr. Rajnish Kumar', '1st Semester', TRUE),
                ('3e05603c-467c-5545-92b4-b71fd15be9b4', (SELECT id FROM rooms WHERE room_number = 'TB-112' LIMIT 1), 3, 780, 830, 'EE1118', '1st Sem Sec B (P-B2)', 'Dr. Rajnish Kumar', '1st Semester', TRUE),
                ('35b10996-3278-58f6-819c-21fcc28300c5', (SELECT id FROM rooms WHERE room_number = 'EB1-102' LIMIT 1), 3, 780, 830, 'AS1109', '1st Sem Sec B (T-B1)', 'Dr. Manushi Gupta', '1st Semester', TRUE),
                ('4dc13ac0-6f04-5618-b168-e18e4d00eed5', (SELECT id FROM rooms WHERE room_number = 'TB-112' LIMIT 1), 3, 840, 890, 'EE1118', '1st Sem Sec B (P-B2)', 'Dr. Rajnish Kumar', '1st Semester', TRUE),
                ('b84c77f5-e0fb-5730-b709-b337f9ae180b', (SELECT id FROM rooms WHERE room_number = 'EB1-102' LIMIT 1), 3, 900, 950, 'AI0000', '1st Sem Sec B', 'Dr. Manju Rana', '1st Semester', TRUE),
                ('8da2a7cb-4c7f-5d06-81f8-c2d889baae7d', (SELECT id FROM rooms WHERE room_number = 'EB1-102' LIMIT 1), 3, 960, 1010, 'PBL', '1st Sem Sec B', 'Project Based Learning', '1st Semester', TRUE),
                ('033cb707-18bc-5fc2-86cb-044a0519ffec', (SELECT id FROM rooms WHERE room_number = 'EB1-102' LIMIT 1), 4, 540, 590, 'EE1118', '1st Sem Sec B', 'Dr. Rajnish Kumar', '1st Semester', TRUE),
                ('e119c08b-2d55-5895-a810-db55270b24b2', (SELECT id FROM rooms WHERE room_number = 'EB1-102' LIMIT 1), 4, 660, 710, 'AS1109', '1st Sem Sec B', 'Dr. Manushi Gupta', '1st Semester', TRUE),
                ('31e4606b-e480-5163-9ca0-f70ad11715d3', (SELECT id FROM rooms WHERE room_number = 'EB1-102' LIMIT 1), 4, 780, 830, 'CS1139', '1st Sem Sec B', 'Dr. Ankush Soni', '1st Semester', TRUE),
                ('1bf1ad99-b9bd-5d4a-ada1-3e183460caf5', (SELECT id FROM rooms WHERE room_number = 'EB1-102' LIMIT 1), 4, 900, 950, 'CODING', '1st Sem Sec B', 'Coding Hours', '1st Semester', TRUE),
                ('598c7fb5-69cb-53fe-ac11-5fdb2b54f5e9', (SELECT id FROM rooms WHERE room_number = 'EB1-102' LIMIT 1), 4, 960, 1010, 'CODING', '1st Sem Sec B', 'Coding Hours', '1st Semester', TRUE),
                ('71c84611-0d00-5e11-90e6-17af3f52b0b1', (SELECT id FROM rooms WHERE room_number = 'EB1-102' LIMIT 1), 5, 540, 590, 'AI0000', '1st Sem Sec B', 'Dr. Manju Rana', '1st Semester', TRUE),
                ('5eca7789-39bf-515c-a52f-29ffc25d9939', (SELECT id FROM rooms WHERE room_number = 'EB1-102' LIMIT 1), 5, 660, 710, 'CC1110', '1st Sem Sec B', 'Mr. Vishwajeet Sehwag', '1st Semester', TRUE),
                ('70d4e809-305e-5aed-a62a-170f9774024c', (SELECT id FROM rooms WHERE room_number = 'EB1-102' LIMIT 1), 5, 840, 890, 'ES1115', '1st Sem Sec B', 'Dr. Shahnawaz Khan', '1st Semester', TRUE),
                ('13f0d6ed-0594-54f2-89ec-f9849e652cc1', (SELECT id FROM rooms WHERE room_number = 'EB1-103' LIMIT 1), 1, 540, 590, 'AI0000', '1st Sem Sec C', 'Dr. Devisha Tiwari', '1st Semester', TRUE),
                ('72c65eec-7a4b-510f-8474-8be85a0deed0', (SELECT id FROM rooms WHERE room_number = 'EB1-103' LIMIT 1), 1, 600, 650, 'EE1118', '1st Sem Sec C', 'Dr. Rashmi Kushwaha', '1st Semester', TRUE),
                ('f8347257-a052-56b0-bd7a-6264e098dd9f', (SELECT id FROM rooms WHERE room_number = 'EB1-103' LIMIT 1), 1, 660, 710, 'CS1139', '1st Sem Sec C', 'Dr. Amit Sinhal', '1st Semester', TRUE),
                ('f6ea34f9-8002-5b64-ac53-d132404ce928', (SELECT id FROM rooms WHERE room_number = 'EB1-103' LIMIT 1), 1, 840, 890, 'ES1115', '1st Sem Sec C', 'Dr. Shahnawaz Khan', '1st Semester', TRUE),
                ('1fd9e4bc-51f5-5417-823d-b83f42cc2577', (SELECT id FROM rooms WHERE room_number = 'TB-102' LIMIT 1), 1, 900, 950, 'EE1118', '1st Sem Sec C (P-C1)', 'Dr. Rashmi Kushwaha', '1st Semester', TRUE),
                ('7f9fe115-3a5f-55fc-b782-cfdbad79ceb3', (SELECT id FROM rooms WHERE room_number = 'EB1-103' LIMIT 1), 1, 900, 950, 'AS1109', '1st Sem Sec C (T-C2)', 'Dr. Anshu Yadav', '1st Semester', TRUE)
                ON CONFLICT (id) DO NOTHING;
            """);
        } catch (Exception e) {
            log.debug("Slot batch notice: {}", e.getMessage());
        }

        // Batch 9
        try {
            jdbcTemplate.execute("""
                INSERT INTO timetable_slots (id, room_id, day_of_week, start_minute, end_minute, course_code, section, faculty, academic_term, active) VALUES
                ('e643b37b-85b2-5ffe-9914-f4daa135603c', (SELECT id FROM rooms WHERE room_number = 'TB-102' LIMIT 1), 1, 960, 1010, 'EE1118', '1st Sem Sec C (P-C1)', 'Dr. Rashmi Kushwaha', '1st Semester', TRUE),
                ('2d9b2b54-87b5-5a1d-a605-dbd6f3ce36d5', (SELECT id FROM rooms WHERE room_number = 'EB1-103' LIMIT 1), 2, 540, 590, 'CC1110', '1st Sem Sec C', 'Mr. Vishwajeet Sehwag', '1st Semester', TRUE),
                ('2d204de7-1408-5a19-9b87-ccbd92bb5039', (SELECT id FROM rooms WHERE room_number = 'EB1-103' LIMIT 1), 2, 660, 710, 'AI0000', '1st Sem Sec C', 'Dr. Devisha Tiwari', '1st Semester', TRUE),
                ('8cb11419-34c1-55f9-9fab-2bf841bb754d', (SELECT id FROM rooms WHERE room_number = 'EB1-103' LIMIT 1), 2, 780, 830, 'CS1139', '1st Sem Sec C', 'Dr. Amit Sinhal', '1st Semester', TRUE),
                ('8fd86f2f-5add-563e-b7bc-222b232d6528', (SELECT id FROM rooms WHERE room_number = 'EB1-103' LIMIT 1), 2, 840, 890, 'AS1109', '1st Sem Sec C', 'Dr. Anant Pratap Singh', '1st Semester', TRUE),
                ('8aca2cf7-385b-5143-a5b6-caa75448e113', (SELECT id FROM rooms WHERE room_number = 'TB-106' LIMIT 1), 3, 540, 590, 'CS1139', '1st Sem Sec C (P)', 'Dr. Amit Sinhal', '1st Semester', TRUE),
                ('ccff787e-8e5d-53e2-872f-9bc67dab0a03', (SELECT id FROM rooms WHERE room_number = 'TB-106' LIMIT 1), 3, 600, 650, 'CS1139', '1st Sem Sec C (P)', 'Dr. Amit Sinhal', '1st Semester', TRUE),
                ('dad98336-1dde-5dc6-a03e-a4e52b0d35c9', (SELECT id FROM rooms WHERE room_number = 'EB1-103' LIMIT 1), 3, 660, 710, 'EE1118', '1st Sem Sec C', 'Dr. Rashmi Kushwaha', '1st Semester', TRUE),
                ('0ca51d9e-e269-5d80-93a4-6fbec9781165', (SELECT id FROM rooms WHERE room_number = 'EB1-103' LIMIT 1), 3, 780, 830, 'CC1110', '1st Sem Sec C', 'Mr. Vishwajeet Sehwag', '1st Semester', TRUE),
                ('9e177fef-e885-53ca-8d04-409a71a3bfff', (SELECT id FROM rooms WHERE room_number = 'EB1-103' LIMIT 1), 3, 840, 890, 'AS1109', '1st Sem Sec C', 'Dr. Anant Pratap Singh', '1st Semester', TRUE),
                ('7c77ee72-f747-5b50-b61a-4da88957a1ef', (SELECT id FROM rooms WHERE room_number = 'EB1-103' LIMIT 1), 4, 600, 650, 'AS1109', '1st Sem Sec C', 'Dr. Anant Pratap Singh', '1st Semester', TRUE),
                ('8e74c80b-afc3-56e6-bf3e-3e58df029b9b', (SELECT id FROM rooms WHERE room_number = 'EB1-103' LIMIT 1), 4, 660, 710, 'CS1139', '1st Sem Sec C', 'Dr. Amit Sinhal', '1st Semester', TRUE),
                ('bd339aae-ae11-5317-a493-adcff27b6a3f', (SELECT id FROM rooms WHERE room_number = 'TB-112' LIMIT 1), 4, 780, 830, 'EE1118', '1st Sem Sec C (P-C2)', 'Dr. Rashmi Kushwaha', '1st Semester', TRUE),
                ('c98f922f-5d08-558f-8733-df72b9de4105', (SELECT id FROM rooms WHERE room_number = 'TB-112' LIMIT 1), 4, 840, 890, 'EE1118', '1st Sem Sec C (P-C2)', 'Dr. Rashmi Kushwaha', '1st Semester', TRUE),
                ('3f11e821-4789-52c2-897b-5d50ee41c1cf', (SELECT id FROM rooms WHERE room_number = 'EB1-103' LIMIT 1), 4, 900, 950, 'CODING', '1st Sem Sec C', 'Coding Hours', '1st Semester', TRUE),
                ('9be7d3ad-7acf-5d5f-8892-9db8c4d4bb1f', (SELECT id FROM rooms WHERE room_number = 'EB1-103' LIMIT 1), 4, 960, 1010, 'CODING', '1st Sem Sec C', 'Coding Hours', '1st Semester', TRUE),
                ('89f510db-abd7-5236-aadc-7c3d8773e3c8', (SELECT id FROM rooms WHERE room_number = 'EB1-103' LIMIT 1), 5, 540, 590, 'AS1109', '1st Sem Sec C (T-C1)', 'Dr. Anant Pratap Singh', '1st Semester', TRUE),
                ('ce5a8186-fb97-5890-8a3a-cc36d144f31f', (SELECT id FROM rooms WHERE room_number = 'EB1-103' LIMIT 1), 5, 600, 650, 'EE1118', '1st Sem Sec C', 'Dr. Rashmi Kushwaha', '1st Semester', TRUE),
                ('396dad2d-25a7-52b3-8bea-4468a22f84ca', (SELECT id FROM rooms WHERE room_number = 'EB1-103' LIMIT 1), 5, 660, 710, 'ES1115', '1st Sem Sec C', 'Dr. Shahnawaz Khan', '1st Semester', TRUE),
                ('c2a4849e-043e-5bcb-8a58-09021c0c4733', (SELECT id FROM rooms WHERE room_number = 'EB1-103' LIMIT 1), 5, 780, 830, 'AI0000', '1st Sem Sec C', 'Dr. Devisha Tiwari', '1st Semester', TRUE),
                ('609dbd70-9f67-5b35-abd8-5edf94657d19', (SELECT id FROM rooms WHERE room_number = 'EB1-103' LIMIT 1), 5, 840, 890, 'CC1110', '1st Sem Sec C', 'Mr. Vishwajeet Sehwag', '1st Semester', TRUE),
                ('2f5d9c7b-1c66-5f7b-9d48-8569c6c9be61', (SELECT id FROM rooms WHERE room_number = 'EB1-103' LIMIT 1), 5, 960, 1010, 'PBL', '1st Sem Sec C', 'Project Based Learning', '1st Semester', TRUE),
                ('e1369886-49c9-5a52-95fe-0fec90979229', (SELECT id FROM rooms WHERE room_number = 'EB1-104' LIMIT 1), 1, 540, 590, 'CC1110', '1st Sem Sec D', 'Mr. Vishwajeet Sehwag', '1st Semester', TRUE),
                ('465c30eb-19c1-5924-8591-f3dce76ceefa', (SELECT id FROM rooms WHERE room_number = 'EB1-104' LIMIT 1), 1, 660, 710, 'EE1118', '1st Sem Sec D', 'Dr. Megha Sahu', '1st Semester', TRUE),
                ('52f61be4-cb5b-5b56-9809-624219fd87ae', (SELECT id FROM rooms WHERE room_number = 'EB1-104' LIMIT 1), 1, 780, 830, 'CC1110', '1st Sem Sec D', 'Mr. Vishwajeet Sehwag', '1st Semester', TRUE),
                ('042a639b-52ee-5b22-9482-4631733a9e77', (SELECT id FROM rooms WHERE room_number = 'EB1-104' LIMIT 1), 1, 840, 890, 'AI0000', '1st Sem Sec D', 'Dr. Manju Rana', '1st Semester', TRUE),
                ('3bdca278-7932-59b8-bd4b-4bb1fd814ba4', (SELECT id FROM rooms WHERE room_number = 'TB-102' LIMIT 1), 1, 900, 950, 'EE1118', '1st Sem Sec D (P-D1)', 'Dr. Megha Sahu', '1st Semester', TRUE),
                ('52faa2e2-531f-5625-a9f6-5416c01cb935', (SELECT id FROM rooms WHERE room_number = 'EB1-104' LIMIT 1), 1, 900, 950, 'AS1109', '1st Sem Sec D (T-D2)', 'Dr. Sushant Pokhriyal', '1st Semester', TRUE),
                ('0ba1c0af-8f02-53af-a6c8-f4c2d57b9443', (SELECT id FROM rooms WHERE room_number = 'TB-102' LIMIT 1), 1, 960, 1010, 'EE1118', '1st Sem Sec D (P-D1)', 'Dr. Megha Sahu', '1st Semester', TRUE),
                ('30001421-6339-5944-8d33-473d84e9a368', (SELECT id FROM rooms WHERE room_number = 'EB1-104' LIMIT 1), 2, 540, 590, 'EE1118', '1st Sem Sec D', 'Dr. Megha Sahu', '1st Semester', TRUE)
                ON CONFLICT (id) DO NOTHING;
            """);
        } catch (Exception e) {
            log.debug("Slot batch notice: {}", e.getMessage());
        }

        // Batch 10
        try {
            jdbcTemplate.execute("""
                INSERT INTO timetable_slots (id, room_id, day_of_week, start_minute, end_minute, course_code, section, faculty, academic_term, active) VALUES
                ('c9667ff0-1cca-57d1-acb4-9cc21b21efa0', (SELECT id FROM rooms WHERE room_number = 'EB1-104' LIMIT 1), 2, 600, 650, 'AS1109', '1st Sem Sec D', 'Dr. Rabia Kamra', '1st Semester', TRUE),
                ('5245132c-9796-565f-a3aa-c730c8410e2c', (SELECT id FROM rooms WHERE room_number = 'EB1-104' LIMIT 1), 2, 660, 710, 'CS1139', '1st Sem Sec D', 'Dr. Ankush Soni', '1st Semester', TRUE),
                ('f01a71ad-5329-5a5e-9941-6ef4118f838d', (SELECT id FROM rooms WHERE room_number = 'TB-102' LIMIT 1), 2, 780, 830, 'EE1118', '1st Sem Sec D (P-D2)', 'Dr. Megha Sahu', '1st Semester', TRUE),
                ('dbb117f2-2b91-5576-86c0-28d6f043f862', (SELECT id FROM rooms WHERE room_number = 'TB-102' LIMIT 1), 2, 840, 890, 'EE1118', '1st Sem Sec D (P-D2)', 'Dr. Megha Sahu', '1st Semester', TRUE),
                ('3275d582-14db-547c-8c22-3e71e518bf52', (SELECT id FROM rooms WHERE room_number = 'EB1-104' LIMIT 1), 2, 840, 890, 'AS1109', '1st Sem Sec D (T-D1)', 'Dr. Rabia Kamra', '1st Semester', TRUE),
                ('98b5b949-6b06-55b4-8896-c01200b417d9', (SELECT id FROM rooms WHERE room_number = 'EB1-104' LIMIT 1), 2, 960, 1010, 'ES1115', '1st Sem Sec D', 'Dr. Shahnawaz Khan', '1st Semester', TRUE),
                ('ae2308e8-2244-5d52-8c16-583892d0514c', (SELECT id FROM rooms WHERE room_number = 'EB1-104' LIMIT 1), 3, 540, 590, 'AI0000', '1st Sem Sec D', 'Dr. Manju Rana', '1st Semester', TRUE),
                ('1f10569e-fef3-5223-acd1-b466517cdd42', (SELECT id FROM rooms WHERE room_number = 'EB1-104' LIMIT 1), 3, 600, 650, 'AS1109', '1st Sem Sec D', 'Dr. Rabia Kamra', '1st Semester', TRUE),
                ('a7227b50-7edb-555d-a327-872d7c7fa92a', (SELECT id FROM rooms WHERE room_number = 'EB1-104' LIMIT 1), 3, 660, 710, 'CS1139', '1st Sem Sec D', 'Dr. Ankush Soni', '1st Semester', TRUE),
                ('91badb39-1a8f-5ab6-9d8e-b9f50f264e76', (SELECT id FROM rooms WHERE room_number = 'EB1-104' LIMIT 1), 3, 840, 890, 'ES1115', '1st Sem Sec D', 'Dr. Shahnawaz Khan', '1st Semester', TRUE),
                ('4abc6732-eec0-5c81-a547-02346feaa71d', (SELECT id FROM rooms WHERE room_number = 'EB1-104' LIMIT 1), 3, 900, 950, 'CC1110', '1st Sem Sec D', 'Mr. Vishwajeet Sehwag', '1st Semester', TRUE),
                ('5328321f-54af-50b6-a7e9-ddec183f59bb', (SELECT id FROM rooms WHERE room_number = 'TB-106' LIMIT 1), 4, 540, 590, 'CS1139', '1st Sem Sec D (P)', 'Dr. Ankush Soni', '1st Semester', TRUE),
                ('1056cf7f-a6e3-5f5e-8e21-00bcfe08054e', (SELECT id FROM rooms WHERE room_number = 'TB-106' LIMIT 1), 4, 600, 650, 'CS1139', '1st Sem Sec D (P)', 'Dr. Ankush Soni', '1st Semester', TRUE),
                ('ee849542-25a3-56c1-bea9-7cbf2015c64b', (SELECT id FROM rooms WHERE room_number = 'EB1-104' LIMIT 1), 4, 660, 710, 'AI0000', '1st Sem Sec D', 'Dr. Manju Rana', '1st Semester', TRUE),
                ('38903829-4e32-5448-9d91-e2bb6ef8501c', (SELECT id FROM rooms WHERE room_number = 'EB1-104' LIMIT 1), 4, 840, 890, 'AS1109', '1st Sem Sec D', 'Dr. Rabia Kamra', '1st Semester', TRUE),
                ('28c5c46f-8392-54b6-9be9-8f1db893887d', (SELECT id FROM rooms WHERE room_number = 'EB1-104' LIMIT 1), 4, 900, 950, 'CODING', '1st Sem Sec D', 'Coding Hours', '1st Semester', TRUE),
                ('a2b18cf2-1b68-5ff0-b7fe-82a5b449987a', (SELECT id FROM rooms WHERE room_number = 'EB1-104' LIMIT 1), 4, 960, 1010, 'CODING', '1st Sem Sec D', 'Coding Hours', '1st Semester', TRUE),
                ('d4cc9d4d-667c-58a5-a0f0-8abdfc753f02', (SELECT id FROM rooms WHERE room_number = 'EB1-104' LIMIT 1), 5, 540, 590, 'EE1118', '1st Sem Sec D', 'Dr. Megha Sahu', '1st Semester', TRUE),
                ('9b26ec64-c65c-56b7-aa7e-4cf4c0553b0d', (SELECT id FROM rooms WHERE room_number = 'EB1-104' LIMIT 1), 5, 600, 650, 'CS1139', '1st Sem Sec D', 'Dr. Ankush Soni', '1st Semester', TRUE),
                ('a71b4ffe-9fca-593b-a6b1-8efd53741abb', (SELECT id FROM rooms WHERE room_number = 'EB1-104' LIMIT 1), 5, 780, 830, 'CS1139', '1st Sem Sec D', 'Dr. Ankush Soni', '1st Semester', TRUE),
                ('55b552f5-10e8-5e61-be92-661f0f1d3eca', (SELECT id FROM rooms WHERE room_number = 'EB1-104' LIMIT 1), 5, 960, 1010, 'PBL', '1st Sem Sec D', 'Project Based Learning', '1st Semester', TRUE),
                ('1f1a502f-a200-5b01-8114-919c4eae5a25', (SELECT id FROM rooms WHERE room_number = 'EB2-104' LIMIT 1), 1, 540, 590, 'AI0000', '1st Sem Sec E', 'Dr. Manju Rana', '1st Semester', TRUE),
                ('b6e50a18-0d22-552c-b28a-176cfe68545b', (SELECT id FROM rooms WHERE room_number = 'EB2-104' LIMIT 1), 1, 600, 650, 'CS1139', '1st Sem Sec E', 'Mr. Mayank Kundalwal', '1st Semester', TRUE),
                ('4e45c171-1c18-5fed-b641-c4dca44835f4', (SELECT id FROM rooms WHERE room_number = 'EB2-104' LIMIT 1), 1, 660, 710, 'ES1115', '1st Sem Sec E', 'Dr. Shahnawaz Khan', '1st Semester', TRUE),
                ('f923bfbe-1731-52d8-99d9-b160941a3975', (SELECT id FROM rooms WHERE room_number = 'TB-112' LIMIT 1), 1, 780, 830, 'EE1118', '1st Sem Sec E (P-E1)', 'Dr. H. P. Agrawal', '1st Semester', TRUE),
                ('e07ce9c8-c953-534b-8842-e3865569f651', (SELECT id FROM rooms WHERE room_number = 'EB2-104' LIMIT 1), 1, 780, 830, 'AS1109', '1st Sem Sec E (T-E2)', 'Dr. Sushant Pokhriyal', '1st Semester', TRUE),
                ('4896ad67-779e-5fcf-ae8f-035d19da9928', (SELECT id FROM rooms WHERE room_number = 'TB-112' LIMIT 1), 1, 840, 890, 'EE1118', '1st Sem Sec E (P-E1)', 'Dr. H. P. Agrawal', '1st Semester', TRUE),
                ('02b105bc-5b0a-5209-8cb0-ebab1c7def6b', (SELECT id FROM rooms WHERE room_number = 'EB2-104' LIMIT 1), 1, 960, 1010, 'PBL', '1st Sem Sec E', 'Project Based Learning', '1st Semester', TRUE),
                ('19c79e93-ad7b-5939-8ff3-d5e857a7ab66', (SELECT id FROM rooms WHERE room_number = 'EB2-104' LIMIT 1), 2, 540, 590, 'AS1109', '1st Sem Sec E', 'Dr. Sushant Pokhriyal', '1st Semester', TRUE),
                ('96a0a752-2962-5b07-b46d-0021d8889d4b', (SELECT id FROM rooms WHERE room_number = 'EB2-104' LIMIT 1), 2, 600, 650, 'EE1118', '1st Sem Sec E', 'Dr. H. P. Agrawal', '1st Semester', TRUE)
                ON CONFLICT (id) DO NOTHING;
            """);
        } catch (Exception e) {
            log.debug("Slot batch notice: {}", e.getMessage());
        }

        // Batch 11
        try {
            jdbcTemplate.execute("""
                INSERT INTO timetable_slots (id, room_id, day_of_week, start_minute, end_minute, course_code, section, faculty, academic_term, active) VALUES
                ('dd8869a4-a069-5b59-916a-dc374fa84866', (SELECT id FROM rooms WHERE room_number = 'EB2-104' LIMIT 1), 2, 660, 710, 'CS1139', '1st Sem Sec E', 'Mr. Mayank Kundalwal', '1st Semester', TRUE),
                ('26e03a3e-24f2-559f-9cd6-6b4ae5bfab6a', (SELECT id FROM rooms WHERE room_number = 'TB-102' LIMIT 1), 2, 780, 830, 'EE1118', '1st Sem Sec E (P-E2)', 'Dr. H. P. Agrawal', '1st Semester', TRUE),
                ('86820792-2923-5de3-bc78-11e138d97fcc', (SELECT id FROM rooms WHERE room_number = 'EB2-104' LIMIT 1), 2, 780, 830, 'AS1109', '1st Sem Sec E (T-E1)', 'Dr. Sushant Pokhriyal', '1st Semester', TRUE),
                ('9f0da86f-fa4c-5530-8fc8-52829f9e3eda', (SELECT id FROM rooms WHERE room_number = 'TB-102' LIMIT 1), 2, 840, 890, 'EE1118', '1st Sem Sec E (P-E2)', 'Dr. H. P. Agrawal', '1st Semester', TRUE),
                ('f1c1d706-396a-5f7a-8bd6-eadf8cbb66aa', (SELECT id FROM rooms WHERE room_number = 'EB2-104' LIMIT 1), 2, 900, 950, 'AI0000', '1st Sem Sec E', 'Dr. Manju Rana', '1st Semester', TRUE),
                ('3be0d3a2-8dac-557a-aab4-8d6e457c6a0a', (SELECT id FROM rooms WHERE room_number = 'EB2-104' LIMIT 1), 3, 540, 590, 'CC1110', '1st Sem Sec E', 'Mr. Vishwajeet Sehwag', '1st Semester', TRUE),
                ('a66fccb6-6e16-5e89-be00-95b29551f710', (SELECT id FROM rooms WHERE room_number = 'EB2-104' LIMIT 1), 3, 600, 650, 'AS1109', '1st Sem Sec E', 'Dr. Sushant Pokhriyal', '1st Semester', TRUE),
                ('b5d9adc6-ce91-5cc1-b0a9-2c16367cbd9e', (SELECT id FROM rooms WHERE room_number = 'EB2-104' LIMIT 1), 3, 660, 710, 'EE1118', '1st Sem Sec E', 'Dr. H. P. Agrawal', '1st Semester', TRUE),
                ('13f31ce4-f37b-5ef4-ab63-33097b0ea5b1', (SELECT id FROM rooms WHERE room_number = 'EB2-104' LIMIT 1), 3, 960, 1010, 'ES1115', '1st Sem Sec E', 'Dr. Shahnawaz Khan', '1st Semester', TRUE),
                ('c7ac2ce9-178a-5c3d-a34d-a37e4c9b69ba', (SELECT id FROM rooms WHERE room_number = 'EB2-104' LIMIT 1), 4, 540, 590, 'EE1118', '1st Sem Sec E', 'Dr. H. P. Agrawal', '1st Semester', TRUE),
                ('55983447-6c27-5a14-87e8-146b6f48645b', (SELECT id FROM rooms WHERE room_number = 'EB2-104' LIMIT 1), 4, 600, 650, 'AI0000', '1st Sem Sec E', 'Dr. Manju Rana', '1st Semester', TRUE),
                ('0cdd0846-3b8e-521f-a6e4-4ea94e4f278d', (SELECT id FROM rooms WHERE room_number = 'EB2-104' LIMIT 1), 4, 660, 710, 'CC1110', '1st Sem Sec E', 'Mr. Vishwajeet Sehwag', '1st Semester', TRUE),
                ('78786271-d898-5c9f-89aa-d3c1f5615e01', (SELECT id FROM rooms WHERE room_number = 'EB2-104' LIMIT 1), 4, 840, 890, 'CC1110', '1st Sem Sec E', 'Mr. Vishwajeet Sehwag', '1st Semester', TRUE),
                ('0ab18245-77e4-56c0-b483-423a1a281215', (SELECT id FROM rooms WHERE room_number = 'EB2-104' LIMIT 1), 4, 900, 950, 'CODING', '1st Sem Sec E', 'Coding Hours', '1st Semester', TRUE),
                ('a4fa772a-bbf3-5a7c-8353-23578aa3ed6c', (SELECT id FROM rooms WHERE room_number = 'EB2-104' LIMIT 1), 4, 960, 1010, 'CODING', '1st Sem Sec E', 'Coding Hours', '1st Semester', TRUE),
                ('066769b0-83f7-5d47-bc51-308d230cfe5f', (SELECT id FROM rooms WHERE room_number = 'TB-106' LIMIT 1), 5, 540, 590, 'CS1139', '1st Sem Sec E (P)', 'Dr. Surbhi Chhabra', '1st Semester', TRUE),
                ('bd926d10-e139-583b-b342-2dfd70869375', (SELECT id FROM rooms WHERE room_number = 'TB-106' LIMIT 1), 5, 600, 650, 'CS1139', '1st Sem Sec E (P)', 'Dr. Surbhi Chhabra', '1st Semester', TRUE),
                ('b5db8a13-d80e-5e7c-a972-1af21e957670', (SELECT id FROM rooms WHERE room_number = 'EB2-104' LIMIT 1), 5, 660, 710, 'CS1139', '1st Sem Sec E', 'Mr. Mayank Kundalwal', '1st Semester', TRUE),
                ('7dfc2821-4b13-5c8a-bba8-245aa34d6481', (SELECT id FROM rooms WHERE room_number = 'EB2-104' LIMIT 1), 5, 780, 830, 'EE1118', '1st Sem Sec E', 'Dr. H. P. Agrawal', '1st Semester', TRUE),
                ('3f22373f-1f81-56bd-b41c-3f7fa14ddd01', (SELECT id FROM rooms WHERE room_number = 'EB2-104' LIMIT 1), 5, 840, 890, 'AS1109', '1st Sem Sec E', 'Dr. Sushant Pokhriyal', '1st Semester', TRUE),
                ('0bf041e4-02bd-5ebe-80ad-6dbc54836c00', (SELECT id FROM rooms WHERE room_number = 'EB2-105' LIMIT 1), 1, 540, 590, 'CC1110', '1st Sem Sec F', 'Dr. Akshay', '1st Semester', TRUE),
                ('51c37389-de50-565c-9c4f-73baec349602', (SELECT id FROM rooms WHERE room_number = 'EB2-105' LIMIT 1), 1, 660, 710, 'AS1109', '1st Sem Sec F', 'Ms. Shobha Mangal', '1st Semester', TRUE),
                ('ebc0274f-294e-5e26-992a-fdae49bc0119', (SELECT id FROM rooms WHERE room_number = 'EB2-105' LIMIT 1), 1, 780, 830, 'AS1108', '1st Sem Sec F', 'Dr. Abhinav Kumar Khorwal', '1st Semester', TRUE),
                ('d82154cc-bc2e-5fab-b904-a8eb323da5cb', (SELECT id FROM rooms WHERE room_number = 'TB-106' LIMIT 1), 1, 900, 950, 'CS1139', '1st Sem Sec F (P)', 'Dr. Amit Sinhal', '1st Semester', TRUE),
                ('f34a345c-de19-5cac-b92c-0a774ccaf58c', (SELECT id FROM rooms WHERE room_number = 'TB-106' LIMIT 1), 1, 960, 1010, 'CS1139', '1st Sem Sec F (P)', 'Dr. Amit Sinhal', '1st Semester', TRUE),
                ('5893f760-4e57-5a32-a7b5-7e72e8af721d', (SELECT id FROM rooms WHERE room_number = 'EB2-105' LIMIT 1), 2, 600, 650, 'CS1139', '1st Sem Sec F', 'Dr. Amit Sinhal', '1st Semester', TRUE),
                ('cbdd0910-5474-52e7-b408-34db53160e6f', (SELECT id FROM rooms WHERE room_number = 'EB2-105' LIMIT 1), 2, 660, 710, 'AS1109', '1st Sem Sec F', 'Ms. Shobha Mangal', '1st Semester', TRUE),
                ('f2d57445-cfd6-5ea8-8ab6-eb9bed4113e6', (SELECT id FROM rooms WHERE room_number = 'EB2-105' LIMIT 1), 2, 900, 950, 'CC1110', '1st Sem Sec F', 'Dr. Akshay', '1st Semester', TRUE),
                ('7a619aeb-b364-5202-9d56-4881bd25f6f4', (SELECT id FROM rooms WHERE room_number = 'EB2-105' LIMIT 1), 2, 960, 1010, 'EE1118', '1st Sem Sec F', 'Dr. Megha Sahu', '1st Semester', TRUE),
                ('8c4375e9-c740-5a50-810f-c07a2b4be221', (SELECT id FROM rooms WHERE room_number = 'EB2-105' LIMIT 1), 3, 540, 590, 'AS1108', '1st Sem Sec F', 'Dr. Abhinav Kumar Khorwal', '1st Semester', TRUE)
                ON CONFLICT (id) DO NOTHING;
            """);
        } catch (Exception e) {
            log.debug("Slot batch notice: {}", e.getMessage());
        }

        // Batch 12
        try {
            jdbcTemplate.execute("""
                INSERT INTO timetable_slots (id, room_id, day_of_week, start_minute, end_minute, course_code, section, faculty, academic_term, active) VALUES
                ('dde9eb74-206a-5ce9-8d67-298a2c8e591c', (SELECT id FROM rooms WHERE room_number = 'EB2-105' LIMIT 1), 3, 600, 650, 'AS1109', '1st Sem Sec F', 'Ms. Shobha Mangal', '1st Semester', TRUE),
                ('a36e159e-55a4-585d-9b26-c9342d3e38f0', (SELECT id FROM rooms WHERE room_number = 'EB2-105' LIMIT 1), 3, 660, 710, 'EE1118', '1st Sem Sec F', 'Dr. Megha Sahu', '1st Semester', TRUE),
                ('5a8906fb-489b-5320-8535-3f7ee480084d', (SELECT id FROM rooms WHERE room_number = 'TB-102' LIMIT 1), 3, 780, 830, 'EE1118', '1st Sem Sec F (P-F2)', 'Dr. Megha Sahu', '1st Semester', TRUE),
                ('f8c54672-6664-54d9-8a1e-d2ba37a44950', (SELECT id FROM rooms WHERE room_number = 'TB-102' LIMIT 1), 3, 840, 890, 'EE1118', '1st Sem Sec F (P-F2)', 'Dr. Megha Sahu', '1st Semester', TRUE),
                ('42ef1ff4-5777-5718-ba8e-d907e19adf8d', (SELECT id FROM rooms WHERE room_number = 'EB2-105' LIMIT 1), 3, 900, 950, 'CS1139', '1st Sem Sec F', 'Dr. Amit Sinhal', '1st Semester', TRUE),
                ('5b6c24cd-d6d6-5481-930f-91b0bb221cde', (SELECT id FROM rooms WHERE room_number = 'EB2-105' LIMIT 1), 3, 960, 1010, 'PBL', '1st Sem Sec F', 'Project Based Learning', '1st Semester', TRUE),
                ('7a4ec7f5-58d3-58d5-8b52-c07cf142877d', (SELECT id FROM rooms WHERE room_number = 'EB2-105' LIMIT 1), 4, 540, 590, 'CC1110', '1st Sem Sec F', 'Dr. Akshay', '1st Semester', TRUE),
                ('f9976dd4-6fa0-5279-94c8-4fcc03e44a3e', (SELECT id FROM rooms WHERE room_number = 'EB2-105' LIMIT 1), 4, 600, 650, 'EE1118', '1st Sem Sec F', 'Dr. Megha Sahu', '1st Semester', TRUE),
                ('883e8b3e-785d-51c1-988f-b1b2022321c9', (SELECT id FROM rooms WHERE room_number = 'EB2-105' LIMIT 1), 4, 660, 710, 'AS1108', '1st Sem Sec F', 'Dr. Abhinav Kumar Khorwal', '1st Semester', TRUE),
                ('584eb12d-cfc5-562d-80f7-b14ccb0317fa', (SELECT id FROM rooms WHERE room_number = 'TB-112' LIMIT 1), 4, 780, 830, 'EE1118', '1st Sem Sec F (P-F1)', 'Dr. Megha Sahu', '1st Semester', TRUE),
                ('4b154b62-88e6-55cf-912a-32ea285239f8', (SELECT id FROM rooms WHERE room_number = 'EB2-105' LIMIT 1), 4, 780, 830, 'AS1109', '1st Sem Sec F (T-F2)', 'Ms. Shobha Mangal', '1st Semester', TRUE),
                ('ec4c1fc8-8652-564c-9cb1-843ef60e1b89', (SELECT id FROM rooms WHERE room_number = 'TB-112' LIMIT 1), 4, 840, 890, 'EE1118', '1st Sem Sec F (P-F1)', 'Dr. Megha Sahu', '1st Semester', TRUE),
                ('ad21763c-cb6f-5d31-869a-dd26e4cd18fe', (SELECT id FROM rooms WHERE room_number = 'EB2-105' LIMIT 1), 4, 900, 950, 'CODING', '1st Sem Sec F', 'Coding Hours', '1st Semester', TRUE),
                ('a39be0b4-b754-5539-8fe8-0522b4a54364', (SELECT id FROM rooms WHERE room_number = 'EB2-105' LIMIT 1), 4, 960, 1010, 'CODING', '1st Sem Sec F', 'Coding Hours', '1st Semester', TRUE),
                ('adad858d-599c-5618-95cd-1b0824b6df2f', (SELECT id FROM rooms WHERE room_number = 'EB2-105' LIMIT 1), 5, 540, 590, 'IL1107', '1st Sem Sec F', 'Mr. Aman Mishra', '1st Semester', TRUE),
                ('38dd4451-d06b-58b6-8838-69edb08885ac', (SELECT id FROM rooms WHERE room_number = 'EB2-105' LIMIT 1), 5, 600, 650, 'AS1109', '1st Sem Sec F (T-F1)', 'Ms. Shobha Mangal', '1st Semester', TRUE),
                ('1a5f9d0d-b99e-5688-b5e1-84cc4997c8db', (SELECT id FROM rooms WHERE room_number = 'EB2-105' LIMIT 1), 5, 660, 710, 'CS1139', '1st Sem Sec F', 'Dr. Amit Sinhal', '1st Semester', TRUE),
                ('ff572734-ec27-5c46-8a18-eed5d358d275', (SELECT id FROM rooms WHERE room_number = 'EB2-105' LIMIT 1), 5, 780, 830, 'IL1107', '1st Sem Sec F', 'Mr. Aman Mishra', '1st Semester', TRUE),
                ('a3d0a738-95dd-567f-8954-c0a41d25c0ec', (SELECT id FROM rooms WHERE room_number = 'TB-102' LIMIT 1), 1, 540, 590, 'EE1118', '1st Sem Sec G (P-G1)', 'Dr. H. P. Agrawal', '1st Semester', TRUE),
                ('ea70b248-e2e3-5812-8609-e29db6ca9694', (SELECT id FROM rooms WHERE room_number = 'TB-102' LIMIT 1), 1, 600, 650, 'EE1118', '1st Sem Sec G (P-G1)', 'Dr. H. P. Agrawal', '1st Semester', TRUE),
                ('e0be292f-07a0-5dc7-9ba8-8110edcd0b60', (SELECT id FROM rooms WHERE room_number = 'EB2-106' LIMIT 1), 1, 600, 650, 'AS1109', '1st Sem Sec G (T-G2)', 'Dr. Anshu Yadav', '1st Semester', TRUE),
                ('6e8005da-29e3-5e3d-a164-6fdcf8531766', (SELECT id FROM rooms WHERE room_number = 'EB2-106' LIMIT 1), 1, 660, 710, 'AS1109', '1st Sem Sec G', 'Dr. Anshu Yadav', '1st Semester', TRUE),
                ('e583d1a7-e008-5174-a7f3-69c728f5310b', (SELECT id FROM rooms WHERE room_number = 'EB2-106' LIMIT 1), 1, 900, 950, 'EE1118', '1st Sem Sec G', 'Dr. Devika Kataria', '1st Semester', TRUE),
                ('d2941c98-b018-5dc0-a7ea-9084d4a01446', (SELECT id FROM rooms WHERE room_number = 'EB2-106' LIMIT 1), 2, 540, 590, 'CS1139', '1st Sem Sec G', 'Mr. Mayank Kundalwal', '1st Semester', TRUE),
                ('5834850b-c19d-5a66-bce5-c44ba869132a', (SELECT id FROM rooms WHERE room_number = 'EB2-106' LIMIT 1), 2, 600, 650, 'CC1110', '1st Sem Sec G', 'Dr. Akshay', '1st Semester', TRUE),
                ('cd5f91f0-0980-5aa1-aaeb-ddcf549f79a3', (SELECT id FROM rooms WHERE room_number = 'EB2-106' LIMIT 1), 2, 660, 710, 'AS1109', '1st Sem Sec G', 'Dr. Anshu Yadav', '1st Semester', TRUE),
                ('6231a6d2-2832-5bfe-ba95-baf62e16e2b9', (SELECT id FROM rooms WHERE room_number = 'EB2-106' LIMIT 1), 2, 780, 830, 'AS1108', '1st Sem Sec G', 'Dr. Abhinav Kumar Khorwal', '1st Semester', TRUE),
                ('7740a683-e85d-506e-b281-dcc7ea7b665d', (SELECT id FROM rooms WHERE room_number = 'TB-106' LIMIT 1), 2, 900, 950, 'CS1139', '1st Sem Sec G (P)', 'Mr. Mayank Kundalwal', '1st Semester', TRUE),
                ('593c4462-0a91-5f1a-a3cc-38a9628f2193', (SELECT id FROM rooms WHERE room_number = 'TB-106' LIMIT 1), 2, 960, 1010, 'CS1139', '1st Sem Sec G (P)', 'Mr. Mayank Kundalwal', '1st Semester', TRUE),
                ('3436ffef-a23e-523c-8be5-2aecf9b325ff', (SELECT id FROM rooms WHERE room_number = 'EB2-106' LIMIT 1), 3, 540, 590, 'IL1107', '1st Sem Sec G', 'Mr. Aman Mishra', '1st Semester', TRUE)
                ON CONFLICT (id) DO NOTHING;
            """);
        } catch (Exception e) {
            log.debug("Slot batch notice: {}", e.getMessage());
        }

        // Batch 13
        try {
            jdbcTemplate.execute("""
                INSERT INTO timetable_slots (id, room_id, day_of_week, start_minute, end_minute, course_code, section, faculty, academic_term, active) VALUES
                ('1872c9d7-fb67-5e85-b69f-246853ad8d13', (SELECT id FROM rooms WHERE room_number = 'EB2-106' LIMIT 1), 3, 600, 650, 'CS1139', '1st Sem Sec G', 'Mr. Mayank Kundalwal', '1st Semester', TRUE),
                ('934e3e9d-fe1c-581d-bd60-592aa3976a2b', (SELECT id FROM rooms WHERE room_number = 'EB2-106' LIMIT 1), 3, 660, 710, 'EE1118', '1st Sem Sec G', 'Dr. Devika Kataria', '1st Semester', TRUE),
                ('5373cce8-9370-5585-902f-d2c8c4c38721', (SELECT id FROM rooms WHERE room_number = 'EB2-106' LIMIT 1), 3, 780, 830, 'AS1108', '1st Sem Sec G', 'Dr. Abhinav Kumar Khorwal', '1st Semester', TRUE),
                ('b76b76e9-a748-5919-ad9e-1d8ce38f96a9', (SELECT id FROM rooms WHERE room_number = 'EB2-106' LIMIT 1), 3, 840, 890, 'IL1107', '1st Sem Sec G', 'Mr. Aman Mishra', '1st Semester', TRUE),
                ('20f6c5d8-446b-5a44-97a0-647e8d41b464', (SELECT id FROM rooms WHERE room_number = 'EB2-106' LIMIT 1), 3, 900, 950, 'CC1110', '1st Sem Sec G', 'Dr. Akshay', '1st Semester', TRUE),
                ('676c4b6e-1a5f-5bcf-929c-ddd26ab66c3a', (SELECT id FROM rooms WHERE room_number = 'EB2-106' LIMIT 1), 3, 960, 1010, 'PBL', '1st Sem Sec G', 'Project Based Learning', '1st Semester', TRUE),
                ('a31b1929-4ba5-5f8e-a1b6-7b57472bc4e4', (SELECT id FROM rooms WHERE room_number = 'EB2-106' LIMIT 1), 4, 600, 650, 'AS1109', '1st Sem Sec G', 'Dr. Anshu Yadav', '1st Semester', TRUE),
                ('9fdb0943-a81a-5c70-8edc-c5f8f3087a54', (SELECT id FROM rooms WHERE room_number = 'EB2-106' LIMIT 1), 4, 660, 710, 'CS1139', '1st Sem Sec G', 'Mr. Mayank Kundalwal', '1st Semester', TRUE),
                ('c302afe0-1717-5bfa-9cfe-d1e638daaa9a', (SELECT id FROM rooms WHERE room_number = 'EB2-106' LIMIT 1), 4, 780, 830, 'AS1108', '1st Sem Sec G', 'Dr. Abhinav Kumar Khorwal', '1st Semester', TRUE),
                ('439e0888-cebe-52b5-abd9-4dfb3462a12b', (SELECT id FROM rooms WHERE room_number = 'EB2-106' LIMIT 1), 4, 840, 890, 'EE1118', '1st Sem Sec G', 'Dr. Devika Kataria', '1st Semester', TRUE),
                ('d51c678c-76b8-558a-9b5e-d4c9387d39f8', (SELECT id FROM rooms WHERE room_number = 'EB2-106' LIMIT 1), 4, 900, 950, 'CODING', '1st Sem Sec G', 'Coding Hours', '1st Semester', TRUE),
                ('0af3cfbc-ecd4-55bd-9c89-1cfcf0db2d47', (SELECT id FROM rooms WHERE room_number = 'EB2-106' LIMIT 1), 4, 960, 1010, 'CODING', '1st Sem Sec G', 'Coding Hours', '1st Semester', TRUE),
                ('39eb4d89-e2aa-5b0a-a1c5-972c40f24335', (SELECT id FROM rooms WHERE room_number = 'EB2-106' LIMIT 1), 5, 540, 590, 'CC1110', '1st Sem Sec G', 'Dr. Akshay', '1st Semester', TRUE),
                ('66a9b075-a013-5677-9d9d-cb1a38426bc0', (SELECT id FROM rooms WHERE room_number = 'EB2-106' LIMIT 1), 5, 660, 710, 'CC1110', '1st Sem Sec G', 'Dr. Akshay', '1st Semester', TRUE),
                ('fec27313-56f6-59c3-b772-f7546e0f099c', (SELECT id FROM rooms WHERE room_number = 'TB-112' LIMIT 1), 5, 780, 830, 'EE1118', '1st Sem Sec G (P-G2)', 'Dr. H. P. Agrawal', '1st Semester', TRUE),
                ('d528d0dd-d4e6-5795-a7c2-167c9c6b3e55', (SELECT id FROM rooms WHERE room_number = 'TB-112' LIMIT 1), 5, 840, 890, 'EE1118', '1st Sem Sec G (P-G2)', 'Dr. H. P. Agrawal', '1st Semester', TRUE),
                ('1c72fa0a-69bb-5d47-9df9-441fdb8f4e29', (SELECT id FROM rooms WHERE room_number = 'EB2-106' LIMIT 1), 5, 840, 890, 'AS1109', '1st Sem Sec G (T-G1)', 'Dr. Anshu Yadav', '1st Semester', TRUE),
                ('f33ff1cf-a537-5d25-a59f-e202ce43f3b7', (SELECT id FROM rooms WHERE room_number = 'EB2-202' LIMIT 1), 1, 540, 590, 'AS1108', '1st Sem Sec J', 'Dr. Abhinav Kumar Khorwal', '1st Semester', TRUE),
                ('7507ff78-cc4b-52b5-9d37-5caf04219594', (SELECT id FROM rooms WHERE room_number = 'EB2-202' LIMIT 1), 1, 600, 650, 'EE1118', '1st Sem Sec J', 'Dr. Devika Kataria', '1st Semester', TRUE),
                ('d6c513a4-3b68-5414-844e-cab84335d56f', (SELECT id FROM rooms WHERE room_number = 'EB2-202' LIMIT 1), 1, 780, 830, 'IL1107', '1st Sem Sec J', 'Mr. Aman Mishra', '1st Semester', TRUE),
                ('1b249ae8-9392-5d8d-8682-a70ea5721354', (SELECT id FROM rooms WHERE room_number = 'EB2-202' LIMIT 1), 1, 900, 950, 'CC1110', '1st Sem Sec J', 'Dr. Akshay', '1st Semester', TRUE),
                ('8792bc5c-1da5-5c66-a6ca-f8f116d0146c', (SELECT id FROM rooms WHERE room_number = 'EB2-202' LIMIT 1), 1, 960, 1010, 'CC1110', '1st Sem Sec J', 'Dr. Akshay', '1st Semester', TRUE),
                ('f3f836ae-edc0-5736-8022-7dbc6e7b6b3d', (SELECT id FROM rooms WHERE room_number = 'EB2-202' LIMIT 1), 2, 600, 650, 'CS1139', '1st Sem Sec J', 'Dr. Surbhi Chhabra', '1st Semester', TRUE),
                ('99cbbe21-5837-57a5-8def-492741ce2c85', (SELECT id FROM rooms WHERE room_number = 'EB2-202' LIMIT 1), 2, 660, 710, 'AS1108', '1st Sem Sec J', 'Dr. Abhinav Kumar Khorwal', '1st Semester', TRUE),
                ('060da82f-9e08-59bd-a575-1ab1b9d1c712', (SELECT id FROM rooms WHERE room_number = 'EB2-202' LIMIT 1), 2, 780, 830, 'IL1107', '1st Sem Sec J', 'Mr. Aman Mishra', '1st Semester', TRUE),
                ('7822354c-435e-5d4f-b3c8-1d4b74f1dfa3', (SELECT id FROM rooms WHERE room_number = 'EB2-202' LIMIT 1), 2, 840, 890, 'AS1109', '1st Sem Sec J', 'Ms. Shobha Mangal', '1st Semester', TRUE),
                ('a7765e92-f2d2-54f3-a582-f1031889f529', (SELECT id FROM rooms WHERE room_number = 'EB2-202' LIMIT 1), 2, 960, 1010, 'PBL', '1st Sem Sec J', 'Project Based Learning', '1st Semester', TRUE),
                ('c0e23b9c-2137-56d5-99b2-5efa996cc338', (SELECT id FROM rooms WHERE room_number = 'TB-102' LIMIT 1), 3, 540, 590, 'EE1118', '1st Sem Sec J (P-J2)', 'Dr. Devika Kataria', '1st Semester', TRUE),
                ('48a6f5ce-7400-582f-980c-081022968c90', (SELECT id FROM rooms WHERE room_number = 'EB2-202' LIMIT 1), 3, 540, 590, 'AS1109', '1st Sem Sec J (T-J1)', 'Ms. Shobha Mangal', '1st Semester', TRUE),
                ('0dcad1fa-25e4-5d14-b1bd-355266109e8c', (SELECT id FROM rooms WHERE room_number = 'TB-102' LIMIT 1), 3, 600, 650, 'EE1118', '1st Sem Sec J (P-J2)', 'Dr. Devika Kataria', '1st Semester', TRUE)
                ON CONFLICT (id) DO NOTHING;
            """);
        } catch (Exception e) {
            log.debug("Slot batch notice: {}", e.getMessage());
        }

        // Batch 14
        try {
            jdbcTemplate.execute("""
                INSERT INTO timetable_slots (id, room_id, day_of_week, start_minute, end_minute, course_code, section, faculty, academic_term, active) VALUES
                ('cad1440a-d293-5862-bdd3-3d4760f756b5', (SELECT id FROM rooms WHERE room_number = 'EB2-202' LIMIT 1), 3, 660, 710, 'CS1139', '1st Sem Sec J', 'Dr. Surbhi Chhabra', '1st Semester', TRUE),
                ('85d118da-d4b2-5f81-a825-c1d853e83e46', (SELECT id FROM rooms WHERE room_number = 'EB2-202' LIMIT 1), 3, 840, 890, 'AS1109', '1st Sem Sec J', 'Ms. Shobha Mangal', '1st Semester', TRUE),
                ('896cc90a-92c7-5d34-85b9-94fd75dcc982', (SELECT id FROM rooms WHERE room_number = 'TB-106' LIMIT 1), 3, 900, 950, 'CS1139', '1st Sem Sec J (P)', 'Dr. Surbhi Chhabra', '1st Semester', TRUE),
                ('9825e78b-acec-5d7d-8ed7-0dd1f80332f1', (SELECT id FROM rooms WHERE room_number = 'TB-106' LIMIT 1), 3, 960, 1010, 'CS1139', '1st Sem Sec J (P)', 'Dr. Surbhi Chhabra', '1st Semester', TRUE),
                ('a23add77-58b7-5405-a697-7f840c46d07a', (SELECT id FROM rooms WHERE room_number = 'EB2-202' LIMIT 1), 4, 600, 650, 'EE1118', '1st Sem Sec J', 'Dr. Devika Kataria', '1st Semester', TRUE),
                ('685a76b2-bb75-5070-a896-5bf61d8a062a', (SELECT id FROM rooms WHERE room_number = 'EB2-202' LIMIT 1), 4, 660, 710, 'AS1109', '1st Sem Sec J', 'Ms. Shobha Mangal', '1st Semester', TRUE),
                ('ce52903e-12aa-574a-b7d4-8bc6bd897ac2', (SELECT id FROM rooms WHERE room_number = 'EB2-202' LIMIT 1), 4, 840, 890, 'CC1110', '1st Sem Sec J', 'Dr. Akshay', '1st Semester', TRUE),
                ('e28e924f-605d-5c74-bcbb-318093fbc8a1', (SELECT id FROM rooms WHERE room_number = 'EB2-202' LIMIT 1), 4, 900, 950, 'CODING', '1st Sem Sec J', 'Coding Hours', '1st Semester', TRUE),
                ('08a8ba5d-59ba-525c-ad87-025f02280665', (SELECT id FROM rooms WHERE room_number = 'EB2-202' LIMIT 1), 4, 960, 1010, 'CODING', '1st Sem Sec J', 'Coding Hours', '1st Semester', TRUE),
                ('6d5cebba-6b38-530a-a3b5-fd8ad804e395', (SELECT id FROM rooms WHERE room_number = 'EB2-202' LIMIT 1), 5, 600, 650, 'EE1118', '1st Sem Sec J', 'Dr. Devika Kataria', '1st Semester', TRUE),
                ('183ebbb5-e044-570b-94d6-aec56473d207', (SELECT id FROM rooms WHERE room_number = 'EB2-202' LIMIT 1), 5, 660, 710, 'AS1108', '1st Sem Sec J', 'Dr. Abhinav Kumar Khorwal', '1st Semester', TRUE),
                ('fbb508c0-2c6e-5a07-9133-a5d48a518ffc', (SELECT id FROM rooms WHERE room_number = 'EB2-202' LIMIT 1), 5, 780, 830, 'CS1139', '1st Sem Sec J', 'Dr. Surbhi Chhabra', '1st Semester', TRUE),
                ('5b782467-8426-5c4a-9a65-892b71e7a0d2', (SELECT id FROM rooms WHERE room_number = 'TB-102' LIMIT 1), 5, 840, 890, 'EE1118', '1st Sem Sec J (P-J1)', 'Dr. Devika Kataria', '1st Semester', TRUE),
                ('9ec491b7-c20d-521a-ac7b-8102b24f9327', (SELECT id FROM rooms WHERE room_number = 'EB2-202' LIMIT 1), 5, 900, 950, 'AS1109', '1st Sem Sec J (T-J2)', 'Dr. Suprita Talnikar', '1st Semester', TRUE),
                ('6fc15a7b-ad01-5a7e-ab0e-06d14fb407e0', (SELECT id FROM rooms WHERE room_number = 'TB-102' LIMIT 1), 5, 900, 950, 'EE1118', '1st Sem Sec J (P-J1)', 'Dr. Devika Kataria', '1st Semester', TRUE),
                ('e937b776-1893-53cd-ae9b-a8e1ae501ff8', (SELECT id FROM rooms WHERE room_number = 'TB-102' LIMIT 1), 5, 960, 1010, 'EE1118', '1st Sem Sec J (P-J1)', 'Dr. Devika Kataria', '1st Semester', TRUE),
                ('0d3d6782-22f1-5b71-9e5c-e963d55585ab', (SELECT id FROM rooms WHERE room_number = 'EB2-204' LIMIT 1), 1, 540, 590, 'EE1118', '1st Sem Sec I', 'Mr. Divanshu Jain', '1st Semester', TRUE),
                ('e6876a27-14bc-53f2-8ed0-e245905506d5', (SELECT id FROM rooms WHERE room_number = 'EB2-204' LIMIT 1), 1, 600, 650, 'AS1109', '1st Sem Sec I', 'Dr. Rabia Kamra', '1st Semester', TRUE),
                ('ce27884a-508c-5ee8-9426-286feafc8779', (SELECT id FROM rooms WHERE room_number = 'EB2-204' LIMIT 1), 1, 660, 710, 'AS1108', '1st Sem Sec I', 'Dr. Krishna Chaitanya Solasa', '1st Semester', TRUE),
                ('22d2febc-b7d2-53b9-8a3f-4309c5c19d5a', (SELECT id FROM rooms WHERE room_number = 'TB-112' LIMIT 1), 1, 780, 830, 'EE1118', '1st Sem Sec I (P-I1)', 'Mr. Divanshu Jain', '1st Semester', TRUE),
                ('947344da-8ae2-58fe-86dd-2958dd1e5176', (SELECT id FROM rooms WHERE room_number = 'EB2-204' LIMIT 1), 1, 780, 830, 'AS1109', '1st Sem Sec I (T-I2)', 'Ms. Shobha Mangal', '1st Semester', TRUE),
                ('385ddd7b-af0d-5279-9d1d-c7c03c135d53', (SELECT id FROM rooms WHERE room_number = 'TB-112' LIMIT 1), 1, 840, 890, 'EE1118', '1st Sem Sec I (P-I1)', 'Mr. Divanshu Jain', '1st Semester', TRUE),
                ('df1955d2-0bfa-55c5-a9d9-d3801b9fd3c6', (SELECT id FROM rooms WHERE room_number = 'EB2-204' LIMIT 1), 1, 960, 1010, 'PBL', '1st Sem Sec I', 'Project Based Learning', '1st Semester', TRUE),
                ('3a2b8fcc-bdce-5b85-8c96-25639d68bebd', (SELECT id FROM rooms WHERE room_number = 'EB2-204' LIMIT 1), 2, 540, 590, 'AS1108', '1st Sem Sec I', 'Dr. Krishna Chaitanya Solasa', '1st Semester', TRUE),
                ('cb95e0c3-dcae-5a81-89c5-796548a0c591', (SELECT id FROM rooms WHERE room_number = 'EB2-204' LIMIT 1), 2, 600, 650, 'EE1118', '1st Sem Sec I', 'Mr. Divanshu Jain', '1st Semester', TRUE),
                ('573192ca-bd68-5cad-8573-a85f5d21ba73', (SELECT id FROM rooms WHERE room_number = 'EB2-204' LIMIT 1), 2, 660, 710, 'IL1107', '1st Sem Sec I', 'Dr. Nitya Pawar', '1st Semester', TRUE),
                ('1c62373a-2a38-5f7c-b356-8b9f5785fd30', (SELECT id FROM rooms WHERE room_number = 'EB2-204' LIMIT 1), 2, 840, 890, 'CC1110', '1st Sem Sec I', 'Dr. Akshay', '1st Semester', TRUE),
                ('d4856c0c-1ee3-5a32-a066-675af141831d', (SELECT id FROM rooms WHERE room_number = 'EB2-204' LIMIT 1), 2, 900, 950, 'CS1139', '1st Sem Sec I', 'Dr. Amar Nath Patra', '1st Semester', TRUE),
                ('e5e7d34b-99dc-5ed0-9275-8af454a87bfe', (SELECT id FROM rooms WHERE room_number = 'EB2-204' LIMIT 1), 3, 540, 590, 'CC1110', '1st Sem Sec I', 'Dr. Akshay', '1st Semester', TRUE),
                ('a0ccdf08-5dec-565e-ade4-7c42cacd48bb', (SELECT id FROM rooms WHERE room_number = 'EB2-204' LIMIT 1), 3, 600, 650, 'CS1139', '1st Sem Sec I', 'Dr. Amar Nath Patra', '1st Semester', TRUE)
                ON CONFLICT (id) DO NOTHING;
            """);
        } catch (Exception e) {
            log.debug("Slot batch notice: {}", e.getMessage());
        }

        // Batch 15
        try {
            jdbcTemplate.execute("""
                INSERT INTO timetable_slots (id, room_id, day_of_week, start_minute, end_minute, course_code, section, faculty, academic_term, active) VALUES
                ('02dae190-693e-5bce-aa4f-f8abd4fc2ac1', (SELECT id FROM rooms WHERE room_number = 'EB2-204' LIMIT 1), 3, 780, 830, 'CC1110', '1st Sem Sec I', 'Dr. Akshay', '1st Semester', TRUE),
                ('35e600a5-4d59-5edd-9b35-61497f77e34a', (SELECT id FROM rooms WHERE room_number = 'EB2-204' LIMIT 1), 3, 840, 890, 'AS1109', '1st Sem Sec I', 'Dr. Rabia Kamra', '1st Semester', TRUE),
                ('0c790793-854f-5f55-8ef0-57180cf399cd', (SELECT id FROM rooms WHERE room_number = 'EB2-204' LIMIT 1), 3, 900, 950, 'CS1139', '1st Sem Sec I', 'Dr. Amar Nath Patra', '1st Semester', TRUE),
                ('a08b4df4-463c-5f4b-9827-6a908fab0db2', (SELECT id FROM rooms WHERE room_number = 'TB-102' LIMIT 1), 4, 540, 590, 'EE1118', '1st Sem Sec I (P-I2)', 'Mr. Divanshu Jain', '1st Semester', TRUE),
                ('4d2e8184-4578-5165-a806-af128861bebe', (SELECT id FROM rooms WHERE room_number = 'EB2-204' LIMIT 1), 4, 540, 590, 'AS1109', '1st Sem Sec I (T-I1)', 'Dr. Rabia Kamra', '1st Semester', TRUE),
                ('671b18f7-6664-5bd0-aa4a-8d868d069976', (SELECT id FROM rooms WHERE room_number = 'TB-102' LIMIT 1), 4, 600, 650, 'EE1118', '1st Sem Sec I (P-I2)', 'Mr. Divanshu Jain', '1st Semester', TRUE),
                ('877e780c-e6b0-5320-b417-14bf4d48a7ef', (SELECT id FROM rooms WHERE room_number = 'EB2-204' LIMIT 1), 4, 660, 710, 'IL1107', '1st Sem Sec I', 'Dr. Nitya Pawar', '1st Semester', TRUE),
                ('b689e8d0-01ee-595b-b023-760df5e01b32', (SELECT id FROM rooms WHERE room_number = 'EB2-204' LIMIT 1), 4, 780, 830, 'EE1118', '1st Sem Sec I', 'Mr. Divanshu Jain', '1st Semester', TRUE),
                ('13393a7d-e594-558b-9f25-8f017b8c1611', (SELECT id FROM rooms WHERE room_number = 'EB2-204' LIMIT 1), 4, 840, 890, 'CS1139', '1st Sem Sec I', 'Dr. Amar Nath Patra', '1st Semester', TRUE),
                ('f1ac7eea-a9e5-5566-b30e-6ad5a0a1740d', (SELECT id FROM rooms WHERE room_number = 'EB2-204' LIMIT 1), 4, 900, 950, 'CODING', '1st Sem Sec I', 'Coding Hours', '1st Semester', TRUE),
                ('71a061d1-e54d-5aa1-9354-5ebe57535e88', (SELECT id FROM rooms WHERE room_number = 'EB2-204' LIMIT 1), 4, 960, 1010, 'CODING', '1st Sem Sec I', 'Coding Hours', '1st Semester', TRUE),
                ('091c11b6-98cc-5e25-910b-acde4815b590', (SELECT id FROM rooms WHERE room_number = 'EB2-204' LIMIT 1), 5, 540, 590, 'CC1110', '1st Sem Sec I', 'Dr. Akshay', '1st Semester', TRUE),
                ('2bacea11-bc4b-5beb-aa5d-e23c00beafee', (SELECT id FROM rooms WHERE room_number = 'EB2-204' LIMIT 1), 5, 600, 650, 'AS1109', '1st Sem Sec I', 'Dr. Rabia Kamra', '1st Semester', TRUE),
                ('9d90ecb5-5e08-56b9-a0e9-0d63261fb241', (SELECT id FROM rooms WHERE room_number = 'EB2-204' LIMIT 1), 5, 660, 710, 'AS1108', '1st Sem Sec I', 'Dr. Krishna Chaitanya Solasa', '1st Semester', TRUE),
                ('0bac11d1-2264-5163-b1df-61085ccd1ffd', (SELECT id FROM rooms WHERE room_number = 'TB-106' LIMIT 1), 5, 900, 950, 'CS1139', '1st Sem Sec I (P)', 'Dr. Amar Nath Patra', '1st Semester', TRUE),
                ('8023da87-8a0c-5b7f-aede-114d65c1ff60', (SELECT id FROM rooms WHERE room_number = 'TB-106' LIMIT 1), 5, 960, 1010, 'CS1139', '1st Sem Sec I (P)', 'Dr. Amar Nath Patra', '1st Semester', TRUE),
                ('c8cd70f7-b598-5924-93a4-7bcfefaf0be9', (SELECT id FROM rooms WHERE room_number = 'EB2-205' LIMIT 1), 1, 540, 590, 'IL1107', '1st Sem Sec H', 'Mr. Aman Mishra', '1st Semester', TRUE),
                ('6b326efb-3e6c-5590-ad6e-0fe1336f36e2', (SELECT id FROM rooms WHERE room_number = 'EB2-205' LIMIT 1), 1, 600, 650, 'EE1118', '1st Sem Sec H', 'Dr. Gaurav Mani Khanal', '1st Semester', TRUE),
                ('0399bb32-26bb-52e8-820a-826d86001973', (SELECT id FROM rooms WHERE room_number = 'EB2-205' LIMIT 1), 1, 660, 710, 'AS1109', '1st Sem Sec H', 'Dr. Suprita Talnikar', '1st Semester', TRUE),
                ('a7f85077-3877-509c-9970-04bb60b1da3f', (SELECT id FROM rooms WHERE room_number = 'EB2-205' LIMIT 1), 1, 780, 830, 'CS1139', '1st Sem Sec H', 'Dr. Surbhi Chhabra', '1st Semester', TRUE),
                ('199e5977-4ae4-5988-82cc-bb5466ee9215', (SELECT id FROM rooms WHERE room_number = 'EB2-205' LIMIT 1), 2, 600, 650, 'EE1118', '1st Sem Sec H', 'Dr. Gaurav Mani Khanal', '1st Semester', TRUE),
                ('508fa471-5b10-58d4-ae73-619d40f07287', (SELECT id FROM rooms WHERE room_number = 'EB2-205' LIMIT 1), 2, 660, 710, 'AS1109', '1st Sem Sec H', 'Dr. Suprita Talnikar', '1st Semester', TRUE),
                ('8abbb150-2437-5b4f-aeb5-703c1bbb4ffc', (SELECT id FROM rooms WHERE room_number = 'EB2-205' LIMIT 1), 2, 780, 830, 'CS1139', '1st Sem Sec H', 'Dr. Surbhi Chhabra', '1st Semester', TRUE),
                ('dcd16b6e-d952-50dd-9262-242aefc4b48c', (SELECT id FROM rooms WHERE room_number = 'EB2-205' LIMIT 1), 2, 900, 950, 'AS1108', '1st Sem Sec H', 'Dr. Krishna Chaitanya Solasa', '1st Semester', TRUE),
                ('4be44bad-1153-5cf1-9b57-8ac4af39589f', (SELECT id FROM rooms WHERE room_number = 'EB2-205' LIMIT 1), 3, 540, 590, 'AS1108', '1st Sem Sec H', 'Dr. Krishna Chaitanya Solasa', '1st Semester', TRUE),
                ('82a2597c-310f-5f2c-94fb-75a156565089', (SELECT id FROM rooms WHERE room_number = 'EB2-205' LIMIT 1), 3, 600, 650, 'EE1118', '1st Sem Sec H', 'Dr. Gaurav Mani Khanal', '1st Semester', TRUE),
                ('1a1edcaf-acff-550d-bb12-a128ec038599', (SELECT id FROM rooms WHERE room_number = 'EB2-205' LIMIT 1), 3, 660, 710, 'IL1107', '1st Sem Sec H', 'Mr. Aman Mishra', '1st Semester', TRUE),
                ('fe3562cf-1fcf-52c5-9db1-cb137dbc8a3f', (SELECT id FROM rooms WHERE room_number = 'EB2-205' LIMIT 1), 3, 780, 830, 'CC1110', '1st Sem Sec H', 'Dr. Akshay', '1st Semester', TRUE),
                ('a6ff5dca-6504-5a2c-b65a-16dfe00c66e4', (SELECT id FROM rooms WHERE room_number = 'TB-102' LIMIT 1), 3, 840, 890, 'EE1118', '1st Sem Sec H (P-H2)', 'Dr. Rajnish Kumar', '1st Semester', TRUE),
                ('a0fb69ce-1ee8-5b2c-824e-1ce328cb2a9a', (SELECT id FROM rooms WHERE room_number = 'EB2-205' LIMIT 1), 3, 840, 890, 'AS1109', '1st Sem Sec H (T-H1)', 'Dr. Suprita Talnikar', '1st Semester', TRUE)
                ON CONFLICT (id) DO NOTHING;
            """);
        } catch (Exception e) {
            log.debug("Slot batch notice: {}", e.getMessage());
        }

        // Batch 16
        try {
            jdbcTemplate.execute("""
                INSERT INTO timetable_slots (id, room_id, day_of_week, start_minute, end_minute, course_code, section, faculty, academic_term, active) VALUES
                ('4b122143-cad9-5384-92f0-8a34e3ddda99', (SELECT id FROM rooms WHERE room_number = 'TB-102' LIMIT 1), 3, 900, 950, 'EE1118', '1st Sem Sec H (P-H2)', 'Dr. Rajnish Kumar', '1st Semester', TRUE),
                ('40dbf835-488b-555d-8d1b-e662c0d78a1c', (SELECT id FROM rooms WHERE room_number = 'EB2-205' LIMIT 1), 4, 540, 590, 'AS1108', '1st Sem Sec H', 'Dr. Krishna Chaitanya Solasa', '1st Semester', TRUE),
                ('08cc38f0-928c-5d72-af57-76bef31aee98', (SELECT id FROM rooms WHERE room_number = 'EB2-205' LIMIT 1), 4, 600, 650, 'AS1109', '1st Sem Sec H', 'Dr. Suprita Talnikar', '1st Semester', TRUE),
                ('202436f0-cd49-5633-bc79-2548c2c1f739', (SELECT id FROM rooms WHERE room_number = 'EB2-205' LIMIT 1), 4, 660, 710, 'CC1110', '1st Sem Sec H', 'Dr. Akshay', '1st Semester', TRUE),
                ('75abfbde-6f4b-59c7-af97-01ca8adaa8ca', (SELECT id FROM rooms WHERE room_number = 'TB-106' LIMIT 1), 4, 780, 830, 'CS1139', '1st Sem Sec H (P)', 'Dr. Surbhi Chhabra', '1st Semester', TRUE),
                ('61220b6c-1132-5afb-a48c-7073c9aaca3e', (SELECT id FROM rooms WHERE room_number = 'TB-106' LIMIT 1), 4, 840, 890, 'CS1139', '1st Sem Sec H (P)', 'Dr. Surbhi Chhabra', '1st Semester', TRUE),
                ('f515d1cb-8551-5477-a481-ea1356e0bfc7', (SELECT id FROM rooms WHERE room_number = 'EB2-205' LIMIT 1), 4, 900, 950, 'CODING', '1st Sem Sec H', 'Coding Hours', '1st Semester', TRUE),
                ('0fcacc10-ad0e-506b-abd8-b80ac36eaa00', (SELECT id FROM rooms WHERE room_number = 'EB2-205' LIMIT 1), 4, 960, 1010, 'CODING', '1st Sem Sec H', 'Coding Hours', '1st Semester', TRUE),
                ('16e45753-b9d0-5cb4-a599-35e59f89767c', (SELECT id FROM rooms WHERE room_number = 'EB2-205' LIMIT 1), 5, 540, 590, 'CS1139', '1st Sem Sec H', 'Dr. Surbhi Chhabra', '1st Semester', TRUE),
                ('f5629ae6-a383-5a08-ad66-acdabb224e96', (SELECT id FROM rooms WHERE room_number = 'TB-112' LIMIT 1), 5, 600, 650, 'EE1118', '1st Sem Sec H (P-H1)', 'Dr. Rajnish Kumar', '1st Semester', TRUE),
                ('e4d87223-9ea8-5413-8603-178820856570', (SELECT id FROM rooms WHERE room_number = 'TB-112' LIMIT 1), 5, 660, 710, 'EE1118', '1st Sem Sec H (P-H1)', 'Dr. Rajnish Kumar', '1st Semester', TRUE),
                ('4d3c3394-bdd4-50c9-987e-5931c0e68587', (SELECT id FROM rooms WHERE room_number = 'EB2-205' LIMIT 1), 5, 780, 830, 'CC1110', '1st Sem Sec H', 'Dr. Akshay', '1st Semester', TRUE),
                ('1f21700c-92af-5ea8-8e9d-090628213ca7', (SELECT id FROM rooms WHERE room_number = 'EB2-205' LIMIT 1), 5, 780, 830, 'AS1109', '1st Sem Sec H (T-H2)', 'Dr. Suprita Talnikar', '1st Semester', TRUE),
                ('95d66e06-fb81-561f-8b38-f139c0d03090', (SELECT id FROM rooms WHERE room_number = 'EB2-205' LIMIT 1), 5, 900, 950, 'PBL', '1st Sem Sec H', 'Project Based Learning', '1st Semester', TRUE),
                ('63bff72a-0aa1-55c5-b225-3ad84e8ed1eb', (SELECT id FROM rooms WHERE room_number = 'C-204' LIMIT 1), 4, 900, 960, 'CS201', 'Sem 3 Data Structures', 'Dr. Ramanathan', 'Spring 2027', TRUE)
                ON CONFLICT (id) DO NOTHING;
            """);
        } catch (Exception e) {
            log.debug("Slot batch notice: {}", e.getMessage());
        }

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
                CASE (i % 3)
                    WHEN 0 THEN 'LOGIN_SUCCESS'
                    WHEN 1 THEN 'ROOM_AVAILABILITY_SEARCH'
                    ELSE 'TIMETABLE_QUERY'
                END,
                CASE (i % 3)
                    WHEN 0 THEN 'AUTH'
                    WHEN 1 THEN 'ROOM'
                    ELSE 'TIMETABLE'
                END,
                'SYS-AUDIT-' || i,
                '127.0.0.1',
                'Mozilla/5.0 (Windows NT 10.0; Win64; x64) CampusFlow/1.0',
                NOW() - (i || ' hours')::interval
            FROM generate_series(1, 62) AS i
        """);
    }

    private void logRowCounts() {
        Integer blockCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM blocks", Integer.class);
        Integer roomCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rooms", Integer.class);
        Integer slotCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM timetable_slots", Integer.class);
        Integer guestRoomCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM guest_rooms", Integer.class);
        Integer zoneCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM parking_zones", Integer.class);
        Integer userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        Integer bookingCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM bookings", Integer.class);
        Integer auditCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_logs", Integer.class);

        log.info("Demo seed verified row counts: blocks={}, rooms={}, timetable_slots={}, guest_rooms={}, parking_zones={}, users={}, bookings={}, audit_log={}",
                blockCount, roomCount, slotCount, guestRoomCount, zoneCount, userCount, bookingCount, auditCount);
    }
}
