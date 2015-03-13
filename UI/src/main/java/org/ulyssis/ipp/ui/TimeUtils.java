package org.ulyssis.ipp.ui;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class TimeUtils {
	private TimeUtils() {
	}
	
	public static String formatDateForInstant(Instant instant) {
        try {
            LocalDateTime localDate = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy");
            return localDate.format(formatter);
        } catch (DateTimeException ignored) {
            return "N/A";
        }
	}
	
	public static String formatTimeForInstant(Instant instant) {
        try {
            LocalDateTime localTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            return localTime.format(formatter);
        } catch (DateTimeException ignored) {
            return "N/A";
        }
	}
}
