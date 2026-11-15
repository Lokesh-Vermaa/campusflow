package com.jklu.campusflow.timetable.parser;

import com.jklu.campusflow.timetable.dto.TimetableErrorRow;
import com.jklu.campusflow.timetable.dto.TimetableRowParsed;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TimetableParser {

    public record ParseResult(int totalRows, int validRows, List<TimetableErrorRow> errorRows, List<TimetableRowParsed> rows) {}

    public ParseResult parse(InputStream inputStream, String filename, String defaultTerm, Set<String> activeRooms) throws Exception {
        String lowerName = filename != null ? filename.toLowerCase() : "";
        if (lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls")) {
            return parseExcel(inputStream, defaultTerm, activeRooms);
        } else {
            return parseCsv(inputStream, defaultTerm, activeRooms);
        }
    }

    public ParseResult parseCsv(InputStream in, String defaultTerm, Set<String> activeRooms) throws Exception {
        List<TimetableRowParsed> rows = new ArrayList<>();
        List<TimetableErrorRow> errors = new ArrayList<>();

        CSVFormat format = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .build();

        try (CSVParser parser = new CSVParser(new InputStreamReader(in, StandardCharsets.UTF_8), format)) {
            int rowNum = 1;
            for (CSVRecord rec : parser) {
                rowNum++;
                String course = getField(rec, "course", "course_code", "courseCode", "subject");
                String room = getField(rec, "room", "room_number", "roomNumber", "classroom");
                String dayStr = getField(rec, "day", "day_of_week", "dayOfWeek");
                String startStr = getField(rec, "start", "start_time", "startTime");
                String endStr = getField(rec, "end", "end_time", "endTime");
                String section = getField(rec, "section");
                String faculty = getField(rec, "faculty", "instructor");
                String term = getField(rec, "term", "academic_term", "semester");
                if (term == null || term.isBlank()) term = defaultTerm;

                validateAndAdd(rowNum, course, room, dayStr, startStr, endStr, section, faculty, term, activeRooms, rows, errors);
            }
        }

        int validCount = (int) rows.stream().filter(TimetableRowParsed::isValid).count();
        return new ParseResult(rows.size(), validCount, errors, rows);
    }

    public ParseResult parseExcel(InputStream in, String defaultTerm, Set<String> activeRooms) throws Exception {
        List<TimetableRowParsed> rows = new ArrayList<>();
        List<TimetableErrorRow> errors = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            if (!rowIterator.hasNext()) {
                return new ParseResult(0, 0, errors, rows);
            }

            // Header map
            Row headerRow = rowIterator.next();
            Map<String, Integer> headerMap = new HashMap<>();
            for (Cell cell : headerRow) {
                headerMap.put(cell.getStringCellValue().trim().toLowerCase(), cell.getColumnIndex());
            }

            int rowNum = 1;
            DataFormatter formatter = new DataFormatter();
            while (rowIterator.hasNext()) {
                rowNum++;
                Row row = rowIterator.next();
                if (isRowEmpty(row)) continue;

                String course = getCellVal(row, headerMap, formatter, "course", "course_code", "courseCode", "subject");
                String room = getCellVal(row, headerMap, formatter, "room", "room_number", "roomNumber", "classroom");
                String dayStr = getCellVal(row, headerMap, formatter, "day", "day_of_week", "dayOfWeek");
                String startStr = getCellVal(row, headerMap, formatter, "start", "start_time", "startTime");
                String endStr = getCellVal(row, headerMap, formatter, "end", "end_time", "endTime");
                String section = getCellVal(row, headerMap, formatter, "section");
                String faculty = getCellVal(row, headerMap, formatter, "faculty", "instructor");
                String term = getCellVal(row, headerMap, formatter, "term", "academic_term", "semester");
                if (term == null || term.isBlank()) term = defaultTerm;

                validateAndAdd(rowNum, course, room, dayStr, startStr, endStr, section, faculty, term, activeRooms, rows, errors);
            }
        }

        int validCount = (int) rows.stream().filter(TimetableRowParsed::isValid).count();
        return new ParseResult(rows.size(), validCount, errors, rows);
    }

    private void validateAndAdd(
            int rowNum,
            String course,
            String room,
            String dayStr,
            String startStr,
            String endStr,
            String section,
            String faculty,
            String term,
            Set<String> activeRooms,
            List<TimetableRowParsed> rows,
            List<TimetableErrorRow> errors
    ) {
        String error = null;

        if (course == null || course.isBlank()) {
            error = "Course code is required";
        } else if (room == null || room.isBlank()) {
            error = "Room number is required";
        } else if (!activeRooms.contains(room.trim())) {
            error = "Room " + room + " does not exist in master data";
        }

        int dayOfWeek = parseDayOfWeek(dayStr);
        if (error == null && (dayOfWeek < 1 || dayOfWeek > 7)) {
            error = "Invalid day of week: " + dayStr;
        }

        int startMin = parseMinute(startStr);
        int endMin = parseMinute(endStr);
        if (error == null && (startMin < 0 || endMin < 0 || startMin >= endMin)) {
            error = "Invalid time interval: " + startStr + " to " + endStr;
        }

        boolean isValid = (error == null);
        if (!isValid) {
            errors.add(new TimetableErrorRow(rowNum, course != null ? course : "", room != null ? room : "", error));
        }

        rows.add(new TimetableRowParsed(
                rowNum,
                course != null ? course.trim() : "",
                room != null ? room.trim() : "",
                dayOfWeek,
                formatMin(startMin),
                formatMin(endMin),
                startMin,
                endMin,
                section != null ? section.trim() : "",
                faculty != null ? faculty.trim() : "",
                term != null ? term.trim() : "Spring 2027",
                isValid,
                error
        ));
    }

    private int parseDayOfWeek(String dayStr) {
        if (dayStr == null || dayStr.isBlank()) return -1;
        String s = dayStr.trim().toLowerCase();
        try {
            int d = Integer.parseInt(s);
            if (d >= 1 && d <= 7) return d;
        } catch (NumberFormatException ignored) {}

        if (s.startsWith("mon")) return 1;
        if (s.startsWith("tue")) return 2;
        if (s.startsWith("wed")) return 3;
        if (s.startsWith("thu")) return 4;
        if (s.startsWith("fri")) return 5;
        if (s.startsWith("sat")) return 6;
        if (s.startsWith("sun")) return 7;
        return -1;
    }

    private int parseMinute(String timeStr) {
        if (timeStr == null || timeStr.isBlank()) return -1;
        String s = timeStr.trim();
        if (s.contains(":")) {
            String[] parts = s.split(":");
            try {
                int h = Integer.parseInt(parts[0].trim());
                int m = Integer.parseInt(parts[1].trim());
                if (h >= 0 && h <= 24 && m >= 0 && m < 60) {
                    return h * 60 + m;
                }
            } catch (NumberFormatException ignored) {}
        } else {
            try {
                int val = Integer.parseInt(s);
                if (val >= 100 && val <= 2400) {
                    int h = val / 100;
                    int m = val % 100;
                    return h * 60 + m;
                } else if (val >= 0 && val <= 24) {
                    return val * 60;
                }
            } catch (NumberFormatException ignored) {}
        }
        return -1;
    }

    private String formatMin(int min) {
        if (min < 0) return "00:00";
        int h = min / 60;
        int m = min % 60;
        return String.format("%02d:%02d", h, m);
    }

    private String getField(CSVRecord rec, String... names) {
        for (String name : names) {
            if (rec.isMapped(name)) {
                String val = rec.get(name);
                if (val != null && !val.isBlank()) return val;
            }
        }
        return null;
    }

    private String getCellVal(Row row, Map<String, Integer> headerMap, DataFormatter formatter, String... names) {
        for (String name : names) {
            Integer col = headerMap.get(name.toLowerCase());
            if (col != null) {
                Cell cell = row.getCell(col);
                if (cell != null) {
                    String val = formatter.formatCellValue(cell);
                    if (val != null && !val.isBlank()) return val;
                }
            }
        }
        return null;
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }
}