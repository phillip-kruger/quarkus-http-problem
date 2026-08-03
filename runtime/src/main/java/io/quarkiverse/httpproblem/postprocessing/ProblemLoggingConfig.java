package io.quarkiverse.httpproblem.postprocessing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ProblemLoggingConfig {

    public static final Map<String, ProblemLogLevel> DEFAULT_LEVELS = Map.of(
            "4xx", ProblemLogLevel.INFO,
            "5xx", ProblemLogLevel.ERROR);

    public static final List<String> DEFAULT_STACK_TRACE_PATTERNS = List.of("5xx");

    private final Map<Integer, ProblemLogLevel> classLevels;
    private final Map<Integer, ProblemLogLevel> exactLevels;
    private final Set<Integer> stackTraceClasses;
    private final Set<Integer> stackTraceCodes;

    public static ProblemLoggingConfig defaults() {
        return new ProblemLoggingConfig(DEFAULT_LEVELS, DEFAULT_STACK_TRACE_PATTERNS);
    }

    public ProblemLoggingConfig(Map<String, ProblemLogLevel> levels, List<String> stackTracePatterns) {
        this.classLevels = new HashMap<>();
        this.exactLevels = new HashMap<>();
        this.stackTraceClasses = new HashSet<>();
        this.stackTraceCodes = new HashSet<>();

        for (Map.Entry<String, ProblemLogLevel> entry : levels.entrySet()) {
            String key = entry.getKey().toLowerCase(Locale.ROOT);
            if (isStatusClass(key)) {
                classLevels.put(Character.getNumericValue(key.charAt(0)), entry.getValue());
            } else {
                exactLevels.put(parseStatusCode(entry.getKey(),
                        "Invalid logging level key '%s': expected a status class (e.g. 4xx) or exact code (e.g. 401)"),
                        entry.getValue());
            }
        }

        for (String pattern : stackTracePatterns) {
            String trimmed = pattern.trim().toLowerCase(Locale.ROOT);
            if (isStatusClass(trimmed)) {
                stackTraceClasses.add(Character.getNumericValue(trimmed.charAt(0)));
            } else {
                stackTraceCodes.add(parseStatusCode(pattern.trim(),
                        "Invalid include-stack-trace pattern '%s': expected a status class (e.g. 5xx) or exact code (e.g. 403)"));
            }
        }
    }

    public ProblemLogLevel resolveLevel(int statusCode) {
        ProblemLogLevel exact = exactLevels.get(statusCode);
        if (exact != null) {
            return exact;
        }
        return classLevels.getOrDefault(statusCode / 100, ProblemLogLevel.INFO);
    }

    public boolean includeStackTrace(int statusCode) {
        return stackTraceCodes.contains(statusCode) || stackTraceClasses.contains(statusCode / 100);
    }

    private static boolean isStatusClass(String pattern) {
        return pattern.length() == 3
                && pattern.charAt(0) >= '1' && pattern.charAt(0) <= '5'
                && pattern.charAt(1) == 'x'
                && pattern.charAt(2) == 'x';
    }

    private static int parseStatusCode(String value, String errorFormat) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, errorFormat, value));
        }
    }
}
