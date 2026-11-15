package com.jklu.campusflow.timetable.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "timetable_rows")
public class TimetableRow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "upload_id", nullable = false)
    private UUID uploadId;

    @Column(name = "row_number", nullable = false)
    private int rowNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_data", columnDefinition = "jsonb")
    private String rawData;

    @Column(nullable = false, length = 50)
    private String status; // VALID, INVALID, WARNING

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public TimetableRow() {}

    public TimetableRow(UUID uploadId, int rowNumber, String rawData, String status, String errorMessage) {
        this.uploadId = uploadId;
        this.rowNumber = rowNumber;
        this.rawData = rawData;
        this.status = status;
        this.errorMessage = errorMessage;
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUploadId() { return uploadId; }
    public void setUploadId(UUID uploadId) { this.uploadId = uploadId; }
    public int getRowNumber() { return rowNumber; }
    public void setRowNumber(int rowNumber) { this.rowNumber = rowNumber; }
    public String getRawData() { return rawData; }
    public void setRawData(String rawData) { this.rawData = rawData; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}