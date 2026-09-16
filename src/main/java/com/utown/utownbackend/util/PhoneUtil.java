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

        String trimmed = phone.trim();
        boolean startsWithPlus = trimmed.startsWith("+");
        String digitsOnly = trimmed.replaceAll("[^0-9]", "");

        if (digitsOnly.length() < 7) {
            throw new IllegalArgumentException("Phone number does not contain enough digits");
        }

        if (startsWithPlus) {
            return "+" + digitsOnly;
        } else if (digitsOnly.startsWith("0")) {
            return "+82" + digitsOnly.substring(1);
        } else {
            return "+82" + digitsOnly;
        }
    }
}
