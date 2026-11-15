-- =============================================================================
-- JKLU CampusFlow: Master Campus Data Seed (V2)
-- Note: User seeding is handled exclusively at runtime in demo/dev profiles
-- =============================================================================

-- 1. Campus Blocks
INSERT INTO blocks (id, name, code, active) VALUES
('00000000-0000-0000-0000-000000000011', 'Academic Block', 'AB', TRUE),
('00000000-0000-0000-0000-000000000012', 'Technology Block', 'TB', TRUE),
('00000000-0000-0000-0000-000000000013', 'Management Block', 'MB', TRUE)
ON CONFLICT (code) DO NOTHING;

-- 2. Rooms
INSERT INTO rooms (id, block_id, room_number, name, type, capacity, features, active, bookable_outside) VALUES
('00000000-0000-0000-0000-000000000021', '00000000-0000-0000-0000-000000000011', 'A-101', 'Seminar Hall 1', 'CLASSROOM', 60, ARRAY['projector', 'ac', 'smartboard'], TRUE, TRUE),
('00000000-0000-0000-0000-000000000022', '00000000-0000-0000-0000-000000000011', 'A-102', 'Lecture Room 102', 'CLASSROOM', 60, ARRAY['projector', 'ac'], TRUE, TRUE),
('00000000-0000-0000-0000-000000000023', '00000000-0000-0000-0000-000000000011', 'A-201', 'Main Auditorium', 'AMPHITHEATRE', 120, ARRAY['projector', 'ac', 'audio_system', 'stage'], TRUE, TRUE),
('00000000-0000-0000-0000-000000000024', '00000000-0000-0000-0000-000000000011', 'A-202', 'Discussion Room 202', 'CLASSROOM', 40, ARRAY['projector'], TRUE, TRUE),
('00000000-0000-0000-0000-000000000025', '00000000-0000-0000-0000-000000000012', 'T-101', 'Computing Lab 1', 'LAB', 35, ARRAY['computers', 'lan', 'ac', 'projector'], TRUE, TRUE),
('00000000-0000-0000-0000-000000000026', '00000000-0000-0000-0000-000000000012', 'T-102', 'Networks Lab', 'LAB', 35, ARRAY['computers', 'lan', 'ac'], TRUE, TRUE),
('00000000-0000-0000-0000-000000000027', '00000000-0000-0000-0000-000000000012', 'T-201', 'Tech Amphitheatre', 'CLASSROOM', 70, ARRAY['projector', 'ac', 'smartboard'], TRUE, TRUE),
('00000000-0000-0000-0000-000000000028', '00000000-0000-0000-0000-000000000013', 'M-101', 'Case Study Room 1', 'CLASSROOM', 50, ARRAY['projector', 'ac'], TRUE, TRUE),
('00000000-0000-0000-0000-000000000029', '00000000-0000-0000-0000-000000000013', 'M-102', 'Case Study Room 2', 'CLASSROOM', 50, ARRAY['projector', 'ac'], TRUE, TRUE),
('00000000-0000-0000-0000-000000000030', '00000000-0000-0000-0000-000000000013', 'M-201', 'Management Amphitheatre', 'AMPHITHEATRE', 150, ARRAY['projector', 'ac', 'audio_system', 'stage'], TRUE, TRUE)
ON CONFLICT (block_id, room_number) DO NOTHING;

-- 3. Guest Rooms
INSERT INTO guest_rooms (id, room_number, type, capacity, price_per_night, active, features) VALUES
('00000000-0000-0000-0000-000000000031', 'G-101', 'STANDARD', 2, 1200.00, TRUE, ARRAY['wifi', 'ac', 'tv']),
('00000000-0000-0000-0000-000000000032', 'G-102', 'STANDARD', 2, 1200.00, TRUE, ARRAY['wifi', 'ac', 'tv']),
('00000000-0000-0000-0000-000000000033', 'G-201', 'DELUXE', 3, 2000.00, TRUE, ARRAY['wifi', 'ac', 'tv', 'mini_fridge']),
('00000000-0000-0000-0000-000000000034', 'G-202', 'SUITE', 4, 3500.00, TRUE, ARRAY['wifi', 'ac', 'tv', 'mini_fridge', 'living_area'])
ON CONFLICT (room_number) DO NOTHING;

-- 4. Parking Zones
INSERT INTO parking_zones (id, name, code, total_slots, active) VALUES
('00000000-0000-0000-0000-000000000041', 'Faculty & Staff Parking', 'P-STAFF', 60, TRUE),
('00000000-0000-0000-0000-000000000042', 'Visitor Parking Area', 'P-VISITOR', 40, TRUE)
ON CONFLICT (code) DO NOTHING;
