package com.messkhata.utils;

import java.util.UUID;

/**
 * Utility class for generating unique IDs.
 */
public class IdGenerator {

    /**
     * Generate a unique ID for local entities.
     * Uses UUID for guaranteed uniqueness.
     */
    public static String generateId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generate a composite ID for entities that need unique identification
     * based on multiple fields (e.g., meal entries).
     */
    public static String generateCompositeId(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            sb.append(parts[i]);
            if (i < parts.length - 1) {
                sb.append("_");
            }
        }
        return sb.toString();
    }

    /**
     * Generate an ID for monthly reports.
     */
    public static String generateReportId(String messId, int year, int month) {
        return String.format("%s_%d_%02d", messId, year, month);
    }

    /**
     * Generate an ID for member balance.
     */
    public static String generateBalanceId(String messId, String userId, int year, int month) {
        return String.format("%s_%s_%d_%02d", messId, userId, year, month);
    }
}
