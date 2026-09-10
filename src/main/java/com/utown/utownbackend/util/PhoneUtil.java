package com.utown.utownbackend.util;

import org.springframework.util.StringUtils;

public final class PhoneUtil {

    private PhoneUtil() {
    }

    /**
     * Normalizes a phone number to standard E.164-like format (or at least consistently formatted).
     * Strips whitespace, dashes, and other non-digit characters.
     * Assumes a default '+82' (South Korea) prefix if it's missing a country code.
     *
     * @param phone the raw phone number string
     * @return normalized phone number, or null if input is null or blank
     */
    public static String normalizePhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }

        String normalized = phone.replaceAll("[^0-9+]", "");

        // Ensure '+' is only at the beginning
        if (normalized.indexOf('+') > 0) {
            normalized = normalized.substring(0, 1) + normalized.substring(1).replace("+", "");
        }

        // Add default +82 country code for Korea
        if (!normalized.startsWith("+")) {
            if (normalized.startsWith("0")) {
                normalized = "+82" + normalized.substring(1);
            } else {
                normalized = "+82" + normalized;
            }
        }

        return normalized;
    }
}
