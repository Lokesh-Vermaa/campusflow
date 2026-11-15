package com.jklu.campusflow.timetable.model;

import com.jklu.campusflow.campus.model.Room;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "timetable_slots")
public class TimetableSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "day_of_week", nullable = false)
    private int dayOfWeek; // 1 (Mon) - 7 (Sun)

    @Column(name = "start_minute", nullable = false)
    private int startMinute; // e.g. 540 for 09:00

    @Column(name = "end_minute", nullable = false)
    private int endMinute; // e.g. 600 for 10:00

    @Column(name = "course_code", nullable = false, length = 50)
    private String courseCode;

    @Column(length = 50)
    private String section;

    @Column(length = 100)
    private String faculty;

    @Column(name = "academic_term", nullable = false, length = 50)
    private String academicTerm;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public TimetableSlot() {}

    public TimetableSlot(Room room, int dayOfWeek, int startMinute, int endMinute, String courseCode, String section, String faculty, String academicTerm) {
        this.room = room;
        this.dayOfWeek = dayOfWeek;
        this.startMinute = startMinute;
        this.endMinute = endMinute;
        this.courseCode = courseCode;
        this.section = section;
        this.faculty = faculty;
        this.academicTerm = academicTerm;
        this.active = true;
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }
    public int getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(int dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public int getStartMinute() { return startMinute; }
    public void setStartMinute(int startMinute) { this.startMinute = startMinute; }
    public int getEndMinute() { return endMinute; }
    public void setEndMinute(int endMinute) { this.endMinute = endMinute; }
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
    public String getFaculty() { return faculty; }
    public void setFaculty(String faculty) { this.faculty = faculty; }
    public String getAcademicTerm() { return academicTerm; }
    public void setAcademicTerm(String academicTerm) { this.academicTerm = academicTerm; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}