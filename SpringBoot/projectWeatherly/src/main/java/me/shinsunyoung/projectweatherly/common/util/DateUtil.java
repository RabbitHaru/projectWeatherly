package me.shinsunyoung.projectweatherly.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtil {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 E요일 HH:mm");

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");

    private static final DateTimeFormatter DATE_ONLY_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DATE_FORMATTER);
    }

    public static String formatTime(LocalDateTime dateTime) {
        return dateTime.format(TIME_FORMATTER);
    }

    public static String formatDateOnly(LocalDateTime dateTime) {
        return dateTime.format(DATE_ONLY_FORMATTER);
    }

    public static String getCurrentFormattedDateTime() {
        return formatDateTime(LocalDateTime.now());
    }

    public static String getBaseTime() {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        int minute = now.getMinute();

        // 기상청 API 기준 시간 계산 (매 시 40분 이후부터 해당 시 기준)
        if (minute < 40) {
            hour = hour - 1;
            if (hour < 0) hour = 23;
        }

        return String.format("%02d00", hour);
    }
}