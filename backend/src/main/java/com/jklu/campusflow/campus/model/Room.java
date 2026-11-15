package com.jklu.campusflow.campus.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Entity
@Table(name = "rooms")
public class Room {

    private static final Pattern FLOOR_PATTERN = Pattern.compile("^[A-Za-z]+-?([0-9])");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_id", nullable = false)
    private Block block;

    @Column(name = "room_number", nullable = false, length = 50)
    private String roomNumber;

    @Column(length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String type; // CLASSROOM, LAB, AMPHITHEATRE

    @Column(nullable = false)
    private int capacity;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "features", columnDefinition = "text[]", nullable = false)
    private List<String> features = new ArrayList<>();

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "bookable_outside", nullable = false)
    private boolean bookableOutside = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public Room() {}

    public Room(UUID id, Block block, String roomNumber, String name, String type, int capacity, List<String> features, boolean active, boolean bookableOutside) {
        this.id = id;
        this.block = block;
        this.roomNumber = roomNumber;
        this.name = name;
        this.type = type;
        this.capacity = capacity;
        this.features = features != null ? features : new ArrayList<>();
        this.active = active;
        this.bookableOutside = bookableOutside;
        this.createdAt = OffsetDateTime.now();
    }

    public int getFloor() {
        if (roomNumber == null) return 1;
        Matcher matcher = FLOOR_PATTERN.matcher(roomNumber);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {}
        }
        return 1;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Block getBlock() { return block; }
    public void setBlock(Block block) { this.block = block; }
    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public List<String> getFeatures() { return features; }
    public void setFeatures(List<String> features) { this.features = features; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isBookableOutside() { return bookableOutside; }
    public void setBookableOutside(boolean bookableOutside) { this.bookableOutside = bookableOutside; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}