package com.jklu.campusflow.timetable.parser;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TimetableParserTest {

    private TimetableParser parser;
    private final Set<String> activeRooms = Set.of("A-101", "A-102", "T-101", "M-101");

    @BeforeEach
    void setUp() {
        parser = new TimetableParser();
    }

    @Test
    @DisplayName("Parse valid CSV timetable with correct slot timings and active rooms")
    void testParseValidCsv() throws Exception {
        String csv = """
                course,room,day,start,end,section,faculty
                CS101,A-101,Monday,09:00,10:00,Sec A,Dr. Neha
                CS102,A-102,2,10:00,11:00,Sec B,Prof. Sharma
                """;

        ByteArrayInputStream in = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        TimetableParser.ParseResult result = parser.parse(in, "timetable.csv", "Spring 2027", activeRooms);

        assertThat(result.totalRows()).isEqualTo(2);
        assertThat(result.validRows()).isEqualTo(2);
        assertThat(result.errorRows()).isEmpty();
        assertThat(result.rows().get(0).course()).isEqualTo("CS101");
        assertThat(result.rows().get(0).dayOfWeek()).isEqualTo(1);
        assertThat(result.rows().get(0).startMinute()).isEqualTo(540); // 09:00
        assertThat(result.rows().get(0).endMinute()).isEqualTo(600);   // 10:00
    }

    @Test
    @DisplayName("CSV parse flags unknown room and invalid time interval in errorRows")
    void testParseCsvWithErrors() throws Exception {
        String csv = """
                course,room,day,start,end,section,faculty
                CS201,Z-999,Monday,09:00,10:00,Sec A,Dr. Neha
                CS202,A-101,Monday,12:00,11:00,Sec A,Dr. Neha
                """;

        ByteArrayInputStream in = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        TimetableParser.ParseResult result = parser.parse(in, "timetable.csv", "Spring 2027", activeRooms);

        assertThat(result.totalRows()).isEqualTo(2);
        assertThat(result.validRows()).isEqualTo(0);
        assertThat(result.errorRows()).hasSize(2);
        assertThat(result.errorRows().get(0).error()).contains("Room Z-999 does not exist in master data");
        assertThat(result.errorRows().get(1).error()).contains("Invalid time interval");
    }

    @Test
    @DisplayName("Parse valid Excel (.xlsx) workbook using Apache POI")
    void testParseValidExcel() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Timetable");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("course");
            header.createCell(1).setCellValue("room");
            header.createCell(2).setCellValue("day");
            header.createCell(3).setCellValue("start");
            header.createCell(4).setCellValue("end");

            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("EE101");
            r1.createCell(1).setCellValue("T-101");
            r1.createCell(2).setCellValue("3"); // Wednesday
            r1.createCell(3).setCellValue("14:00");
            r1.createCell(4).setCellValue("16:00");

            wb.write(out);
        }

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        TimetableParser.ParseResult result = parser.parse(in, "schedule.xlsx", "Spring 2027", activeRooms);

        assertThat(result.totalRows()).isEqualTo(1);
        assertThat(result.validRows()).isEqualTo(1);
        assertThat(result.errorRows()).isEmpty();
        assertThat(result.rows().get(0).course()).isEqualTo("EE101");
        assertThat(result.rows().get(0).roomNumber()).isEqualTo("T-101");
        assertThat(result.rows().get(0).dayOfWeek()).isEqualTo(3);
    }
}