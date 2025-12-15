package com.messkhata.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Utility class for date operations.
 */
public class DateUtils {

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private static final SimpleDateFormat DATE_TIME_FORMAT =
            new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
    private static final SimpleDateFormat MONTH_YEAR_FORMAT =
            new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
    private static final SimpleDateFormat DAY_FORMAT =
            new SimpleDateFormat("EEEE", Locale.getDefault());

    /**
     * Get start of day in milliseconds for a given timestamp.
     */
    public static long getStartOfDay(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    /**
     * Get end of day in milliseconds for a given timestamp.
     */
    public static long getEndOfDay(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }

    /**
     * Get start of month in milliseconds.
     */
    public static long getStartOfMonth(int year, int month) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1); // Calendar months are 0-indexed
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    /**
     * Get end of month in milliseconds.
     */
    public static long getEndOfMonth(int year, int month) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }

    /**
     * Get today's date at start of day.
     */
    public static long getTodayStart() {
        return getStartOfDay(System.currentTimeMillis());
    }

    /**
     * Get current year.
     */
    public static int getCurrentYear() {
        return Calendar.getInstance().get(Calendar.YEAR);
    }

    /**
     * Get current month (1-12).
     */
    public static int getCurrentMonth() {
        return Calendar.getInstance().get(Calendar.MONTH) + 1;
    }

    /**
     * Format date for display.
     */
    public static String formatDate(long timestamp) {
        return DATE_FORMAT.format(new Date(timestamp));
    }

    /**
     * Format date and time for display.
     */
    public static String formatDateTime(long timestamp) {
        return DATE_TIME_FORMAT.format(new Date(timestamp));
    }

    /**
     * Format month and year for display.
     */
    public static String formatMonthYear(int year, int month) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        return MONTH_YEAR_FORMAT.format(calendar.getTime());
    }

    /**
     * Get day name (Monday, Tuesday, etc.).
     */
    public static String getDayName(long timestamp) {
        return DAY_FORMAT.format(new Date(timestamp));
    }

    /**
     * Check if a timestamp is today.
     */
    public static boolean isToday(long timestamp) {
        return getStartOfDay(timestamp) == getTodayStart();
    }

    /**
     * Get dates for the current week.
     */
    public static long[] getCurrentWeekDates() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());

        long[] dates = new long[7];
        for (int i = 0; i < 7; i++) {
            dates[i] = getStartOfDay(calendar.getTimeInMillis());
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        return dates;
    }

    /**
     * Get the number of days in a month.
     */
    public static int getDaysInMonth(int year, int month) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
    }
}
