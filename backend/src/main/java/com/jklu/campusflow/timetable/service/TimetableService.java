package com.jklu.campusflow.timetable.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jklu.campusflow.campus.model.Room;
import com.jklu.campusflow.campus.repository.RoomRepository;
import com.jklu.campusflow.common.error.BusinessRuleException;
import com.jklu.campusflow.common.error.ConflictException;
import com.jklu.campusflow.common.error.ResourceNotFoundException;
import com.jklu.campusflow.timetable.dto.*;
import com.jklu.campusflow.timetable.model.TimetableRow;
import com.jklu.campusflow.timetable.model.TimetableSlot;
import com.jklu.campusflow.timetable.model.TimetableUpload;
import com.jklu.campusflow.timetable.parser.TimetableParser;
import com.jklu.campusflow.timetable.repository.TimetableRowRepository;
import com.jklu.campusflow.timetable.repository.TimetableSlotRepository;
import com.jklu.campusflow.timetable.repository.TimetableUploadRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class TimetableService {

    private final TimetableUploadRepository uploadRepository;
    private final TimetableRowRepository rowRepository;
    private final TimetableSlotRepository slotRepository;
    private final RoomRepository roomRepository;
    private final ObjectMapper objectMapper;
    private final TimetableParser parser = new TimetableParser();

    public TimetableService(
            TimetableUploadRepository uploadRepository,
            TimetableRowRepository rowRepository,
            TimetableSlotRepository slotRepository,
            RoomRepository roomRepository,
            ObjectMapper objectMapper
    ) {
        this.uploadRepository = uploadRepository;
        this.rowRepository = rowRepository;
        this.slotRepository = slotRepository;
        this.roomRepository = roomRepository;
        this.objectMapper = objectMapper;
    }

    public TimetableUploadResponse uploadAndValidate(MultipartFile file, String academicTerm, UUID userId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Timetable file is required and cannot be empty");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BusinessRuleException("File size exceeds 10MB limit");
        }

        String term = (academicTerm != null && !academicTerm.isBlank()) ? academicTerm : "Spring 2027";
        List<Room> allRooms = roomRepository.findAllActiveWithBlock();
        Set<String> activeRoomNumbers = allRooms.stream().map(Room::getRoomNumber).collect(Collectors.toSet());

        TimetableParser.ParseResult result;
        try (InputStream in = file.getInputStream()) {
            result = parser.parse(in, file.getOriginalFilename(), term, activeRoomNumbers);
        } catch (Exception e) {
            throw new BusinessRuleException("Failed to parse timetable file: " + e.getMessage());
        }

        TimetableUpload upload = new TimetableUpload(
                file.getOriginalFilename(),
                term,
                "PARSED",
                result.totalRows(),
                userId
        );
        if (!result.errorRows().isEmpty()) {
            upload.setErrorSummary(result.errorRows().size() + " invalid rows found");
        }
        upload = uploadRepository.save(upload);

        // Stage rows
        List<TimetableRow> stagedRows = new ArrayList<>();
        for (TimetableRowParsed r : result.rows()) {
            String json;
            try {
                json = objectMapper.writeValueAsString(r);
            } catch (Exception e) {
                json = "{}";
            }
            stagedRows.add(new TimetableRow(
                    upload.getId(),
                    r.rowNumber(),
                    json,
                    r.isValid() ? "VALID" : "INVALID",
                    r.error()
            ));
        }
        rowRepository.saveAll(stagedRows);

        return new TimetableUploadResponse(
                upload.getId(),
                result.totalRows(),
                result.validRows(),
                result.errorRows(),
                result.rows()
        );
    }

    public TimetablePublishResponse publishTimetable(TimetablePublishRequest request) {
        String semester = (request.semester() != null && !request.semester().isBlank()) ? request.semester() : "Spring 2027";

        List<TimetableRowParsed> toPublish = new ArrayList<>();

        if (request.entries() != null && !request.entries().isEmpty()) {
            toPublish.addAll(request.entries().stream().filter(TimetableRowParsed::isValid).toList());
        } else if (request.uploadId() != null) {
            TimetableUpload upload = uploadRepository.findById(request.uploadId())
                    .orElseThrow(() -> new ResourceNotFoundException("Timetable upload not found: " + request.uploadId()));
            List<TimetableRow> rows = rowRepository.findAllByUploadIdOrderByRowNumberAsc(upload.getId());
            for (TimetableRow r : rows) {
                if ("VALID".equals(r.getStatus()) && r.getRawData() != null) {
                    try {
                        TimetableRowParsed parsed = objectMapper.readValue(r.getRawData(), TimetableRowParsed.class);
                        toPublish.add(parsed);
                    } catch (Exception ignored) {}
                }
            }
            upload.setStatus("PUBLISHED");
            uploadRepository.save(upload);
        } else {
            throw new BusinessRuleException("Either uploadId or entries must be provided to publish timetable");
        }

        // Room cache map
        Map<String, Room> roomMap = roomRepository.findAllActiveWithBlock().stream()
                .collect(Collectors.toMap(Room::getRoomNumber, r -> r, (a, b) -> a));

        List<TimetableSlot> slotsToSave = new ArrayList<>();
        for (TimetableRowParsed p : toPublish) {
            Room room = roomMap.get(p.roomNumber());
            if (room == null) {
                throw new ConflictException("Room " + p.roomNumber() + " not found in active master data");
            }

            slotsToSave.add(new TimetableSlot(
                    room,
                    p.dayOfWeek(),
                    p.startMinute(),
                    p.endMinute(),
                    p.course(),
                    p.section(),
                    p.faculty(),
                    semester
            ));
        }

        try {
            slotRepository.saveAllAndFlush(slotsToSave);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Timetable publication conflict: overlapping slot detected in master schedule");
        }

        return new TimetablePublishResponse("PUBLISHED", semester, slotsToSave.size());
    }
}