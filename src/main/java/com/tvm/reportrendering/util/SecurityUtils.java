package com.tvm.reportrendering.util;

/**
 * Security utility class providing common security-related functions.
 *
 * This class contains methods to help prevent security vulnerabilities
 * such as log injection attacks.
 */
public class SecurityUtils {

    /**
     * Sanitizes input strings for safe logging by replacing potentially
     * dangerous characters that could be used for log injection attacks.
     *
     * This method removes or replaces:
     * - Carriage return (\r) → underscore (_)
     * - Line feed/newline (\n) → underscore (_)
     * - Tab character (\t) → underscore (_)
     *
     * @param input the input string to sanitize, may be null
     * @return sanitized string safe for logging, "null" if input is null
     */
    public static String sanitizeForLogging(String input) {
        if (input == null) {
            return "null";
        }
        return input.replace('\r', '_')
                   .replace('\n', '_')
                   .replace('\t', '_');
    }
}