package com.jklu.campusflow.agent.planner;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AgentRequestParser {

    public static final ZoneId ASIA_KOLKATA = ZoneId.of("Asia/Kolkata");

    public record ParsedEventRequest(
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            Integer headcount,
            boolean needsGuestHouse,
            boolean needsParking,
            boolean missingRequiredFields
    ) {}

    public static ParsedEventRequest parse(String message, LocalDate baseDate) {
        if (message == null || message.isBlank()) {
            return new ParsedEventRequest(null, null, null, null, false, false, true);
        }

        if (baseDate == null) {
            baseDate = LocalDate.now(ASIA_KOLKATA);
        }

        String lower = message.toLowerCase().trim();

        // 1. Headcount parsing: e.g. "80 people", "for 40 people", "40 students", "capacity 100", "for 40", "80 ppl"
        Integer headcount = null;
        Matcher hcMatcher = Pattern.compile("(\\d+)\\s*(?:people|peopl|ppl|attendees|students|persons|persns|participants|capacity|seats)\\b").matcher(lower);
        if (hcMatcher.find()) {
            headcount = Integer.parseInt(hcMatcher.group(1));
        } else {
            Matcher altHc = Pattern.compile("for\\s+(\\d+)(?:\\s+(?:people|peopl|ppl))?\\b").matcher(lower);
            if (altHc.find()) {
                headcount = Integer.parseInt(altHc.group(1));
            }
        }

        // 2. Date parsing (Asia/Kolkata)
        LocalDate date = null;
        if (lower.contains("next thursday")) {
            date = baseDate.with(TemporalAdjusters.next(DayOfWeek.THURSDAY));
        } else if (lower.contains("next friday")) {
            date = baseDate.with(TemporalAdjusters.next(DayOfWeek.FRIDAY));
        } else if (lower.contains("next monday")) {
            date = baseDate.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        } else if (lower.contains("next tuesday")) {
            date = baseDate.with(TemporalAdjusters.next(DayOfWeek.TUESDAY));
        } else if (lower.contains("next wednesday")) {
            date = baseDate.with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY));
        } else if (lower.contains("next saturday")) {
            date = baseDate.with(TemporalAdjusters.next(DayOfWeek.SATURDAY));
        } else if (lower.contains("this thursday")) {
            date = baseDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.THURSDAY));
        } else if (lower.contains("this friday")) {
            date = baseDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
        } else if (lower.contains("tomorrow")) {
            date = baseDate.plusDays(1);
        } else if (lower.contains("today")) {
            date = baseDate;
        } else {
            // Check bare weekday e.g. "on friday", "thursday"
            for (DayOfWeek dow : DayOfWeek.values()) {
                if (lower.contains(dow.name().toLowerCase())) {
                    date = baseDate.with(TemporalAdjusters.nextOrSame(dow));
                    break;
                }
            }
            if (date == null) {
                // Check YYYY-MM-DD
                Matcher dateMatcher = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})").matcher(lower);
                if (dateMatcher.find()) {
                    date = LocalDate.parse(dateMatcher.group(1));
                }
            }
        }

        // 3. Time parsing: e.g. "3-5 pm", "3–5 pm", "3 to 5 pm", "3pm to 5pm", "3pm-5pm", "10-11 am", "10am to 11am"
        LocalTime startTime = null;
        LocalTime endTime = null;

        Matcher rangeMatcher = Pattern.compile("(?<![-:\\d])(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\s*(?:-|–|to)\\s*(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?(?![-\\d])").matcher(lower);
        if (rangeMatcher.find()) {
            int startH = Integer.parseInt(rangeMatcher.group(1));
            int startM = rangeMatcher.group(2) != null ? Integer.parseInt(rangeMatcher.group(2)) : 0;
            String startAmPm = rangeMatcher.group(3);
            int endH = Integer.parseInt(rangeMatcher.group(4));
            int endM = rangeMatcher.group(5) != null ? Integer.parseInt(rangeMatcher.group(5)) : 0;
            String endAmPm = rangeMatcher.group(6);

            if ("pm".equalsIgnoreCase(startAmPm) && startH < 12) {
                startH += 12;
            } else if ("am".equalsIgnoreCase(startAmPm) && startH == 12) {
                startH = 0;
            }

            if ("pm".equalsIgnoreCase(endAmPm) && endH < 12) {
                endH += 12;
            } else if ("am".equalsIgnoreCase(endAmPm) && endH == 12) {
                endH = 0;
            }

            // If startAmPm was omitted but endAmPm is PM (e.g. "3 to 5 pm")
            if (startAmPm == null && "pm".equalsIgnoreCase(endAmPm)) {
                if (startH < 12 && (startH >= 1 && startH <= 7 || startH <= endH)) {
                    startH += 12;
                }
            } else if (startAmPm == null && endAmPm == null) {
                // If both omitted and hours are 1..7, default to PM
                if (startH >= 1 && startH <= 6 && endH >= startH && endH <= 7) {
                    startH += 12;
                    endH += 12;
                }
            }

            if (startH >= 0 && startH <= 23 && endH >= 0 && endH <= 23) {
                startTime = LocalTime.of(startH, startM);
                endTime = LocalTime.of(endH, endM);
            }
        }

        // 4. Accommodation & Parking requirements (with negation awareness)
        boolean hasNegationStay = lower.contains("no guest house") || lower.contains("no stay") || lower.contains("without stay") || lower.contains("without guest house");
        boolean hasPositiveStay = lower.contains("stay") || lower.contains("guest house") || lower.contains("guesthouse")
                || lower.contains("accommodation") || lower.contains("hotel");
        boolean needsGuestHouse = hasPositiveStay && !hasNegationStay;

        boolean hasNegationParking = lower.contains("no parking") || lower.contains("without parking") || lower.contains("no vehicle") || lower.contains("or parking");
        boolean hasPositiveParking = lower.contains("parking") || lower.contains("vehicle") || lower.contains("car");
        boolean needsParking = hasPositiveParking && !hasNegationParking;

        // Missing required fields if either date or headcount or times are missing
        boolean missingRequired = (date == null || headcount == null || startTime == null || endTime == null);

        return new ParsedEventRequest(date, startTime, endTime, headcount, needsGuestHouse, needsParking, missingRequired);
    }
}
