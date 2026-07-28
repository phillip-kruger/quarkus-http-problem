package io.quarkiverse.httpproblem;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class DetailSanitizer {

    public static final String SANITIZED_DETAIL = "Malformed request body";

    private final boolean includeDetails;

    public DetailSanitizer() {
        this(false);
    }

    @Inject
    public DetailSanitizer(ProblemRuntimeFixedConfig config) {
        this(config.includeDetails());
    }

    public DetailSanitizer(boolean includeDetails) {
        this.includeDetails = includeDetails;
    }

    public String sanitize(String rawDetail) {
        return includeDetails ? rawDetail : SANITIZED_DETAIL;
    }
}
