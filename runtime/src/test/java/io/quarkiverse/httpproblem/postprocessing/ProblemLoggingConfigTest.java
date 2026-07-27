package io.quarkiverse.httpproblem.postprocessing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ProblemLoggingConfigTest {

    @Test
    void shouldResolveClassLevel() {
        ProblemLoggingConfig config = new ProblemLoggingConfig(
                Map.of("4xx", ProblemLogLevel.WARN, "5xx", ProblemLogLevel.ERROR),
                List.of());

        assertThat(config.resolveLevel(400)).isEqualTo(ProblemLogLevel.WARN);
        assertThat(config.resolveLevel(404)).isEqualTo(ProblemLogLevel.WARN);
        assertThat(config.resolveLevel(500)).isEqualTo(ProblemLogLevel.ERROR);
        assertThat(config.resolveLevel(503)).isEqualTo(ProblemLogLevel.ERROR);
    }

    @Test
    void shouldResolveExactCodeOverClass() {
        ProblemLoggingConfig config = new ProblemLoggingConfig(
                Map.of("4xx", ProblemLogLevel.INFO, "401", ProblemLogLevel.WARN, "403", ProblemLogLevel.ERROR),
                List.of());

        assertThat(config.resolveLevel(400)).isEqualTo(ProblemLogLevel.INFO);
        assertThat(config.resolveLevel(401)).isEqualTo(ProblemLogLevel.WARN);
        assertThat(config.resolveLevel(403)).isEqualTo(ProblemLogLevel.ERROR);
        assertThat(config.resolveLevel(404)).isEqualTo(ProblemLogLevel.INFO);
    }

    @Test
    void shouldFallbackToInfoForUnconfiguredClass() {
        ProblemLoggingConfig config = new ProblemLoggingConfig(
                Map.of("5xx", ProblemLogLevel.ERROR),
                List.of());

        assertThat(config.resolveLevel(400)).isEqualTo(ProblemLogLevel.INFO);
        assertThat(config.resolveLevel(302)).isEqualTo(ProblemLogLevel.INFO);
    }

    @Test
    void shouldIncludeStackTraceForClass() {
        ProblemLoggingConfig config = new ProblemLoggingConfig(
                Map.of(),
                List.of("5xx"));

        assertThat(config.includeStackTrace(500)).isTrue();
        assertThat(config.includeStackTrace(503)).isTrue();
        assertThat(config.includeStackTrace(400)).isFalse();
    }

    @Test
    void shouldIncludeStackTraceForExactCode() {
        ProblemLoggingConfig config = new ProblemLoggingConfig(
                Map.of(),
                List.of("5xx", "403"));

        assertThat(config.includeStackTrace(500)).isTrue();
        assertThat(config.includeStackTrace(403)).isTrue();
        assertThat(config.includeStackTrace(401)).isFalse();
    }

    @Test
    void shouldExcludeStackTraceWhenEmpty() {
        ProblemLoggingConfig config = new ProblemLoggingConfig(
                Map.of(),
                List.of());

        assertThat(config.includeStackTrace(500)).isFalse();
        assertThat(config.includeStackTrace(400)).isFalse();
    }

    @Test
    void shouldHandleMixedCasePatterns() {
        ProblemLoggingConfig config = new ProblemLoggingConfig(
                Map.of("4XX", ProblemLogLevel.WARN),
                List.of("5XX"));

        assertThat(config.resolveLevel(400)).isEqualTo(ProblemLogLevel.WARN);
        assertThat(config.includeStackTrace(500)).isTrue();
    }

    @Test
    void shouldHandleStackTracePatternsWithWhitespace() {
        ProblemLoggingConfig config = new ProblemLoggingConfig(
                Map.of(),
                List.of(" 5xx ", " 403 "));

        assertThat(config.includeStackTrace(500)).isTrue();
        assertThat(config.includeStackTrace(403)).isTrue();
    }

    @Test
    void shouldRejectInvalidLevelKey() {
        assertThatThrownBy(() -> new ProblemLoggingConfig(Map.of("abc", ProblemLogLevel.WARN), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("abc")
                .hasMessageContaining("expected a status class");
    }

    @Test
    void shouldRejectInvalidStackTracePattern() {
        assertThatThrownBy(() -> new ProblemLoggingConfig(Map.of(), List.of("abc")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("abc")
                .hasMessageContaining("expected a status class");
    }
}
