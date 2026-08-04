package io.quarkiverse.httpproblem.postprocessing;

import static io.quarkiverse.httpproblem.postprocessing.ProblemContextMother.simpleContext;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkiverse.httpproblem.HttpProblem;
import io.quarkiverse.httpproblem.validation.Violation;

class ProblemLoggerTest {

    static final ProblemLoggingConfig DEFAULT_CONFIG = ProblemLoggingConfig.defaults();

    Logger logger = mock(Logger.class);
    ProblemLogger processor = new ProblemLogger(logger, DEFAULT_CONFIG);

    @BeforeEach
    void init() {
        when(logger.isEnabled(Logger.Level.ERROR)).thenReturn(true);
        when(logger.isInfoEnabled()).thenReturn(true);
        when(logger.isEnabled(Logger.Level.WARN)).thenReturn(true);
        when(logger.isDebugEnabled()).thenReturn(true);
        when(logger.isTraceEnabled()).thenReturn(true);
    }

    @Test
    void shouldPrintOnlyNotNullFields() {
        HttpProblem problem = HttpProblem.builder()
                .withTitle("your fault")
                .withStatus(BAD_REQUEST)
                .build();

        processor.apply(problem, simpleContext());

        verify(logger).info("status=400, title=\"your fault\"");
    }

    @Test
    void shouldPrintCustomParameters() {
        HttpProblem problem = HttpProblem.builder()
                .withTitle("your fault")
                .withStatus(BAD_REQUEST)
                .with("custom-field", "123")
                .with("violations", Collections.singletonList(Violation.In.body.field("key").message("too small")))
                .with("nullable_field", null)
                .build();

        processor.apply(problem, simpleContext());

        verify(logger).info(
                "status=400, title=\"your fault\", custom-field=\"123\", violations=[Violation{field='key', in='body', message='too small'}], nullable_field=null");
    }

    @Test
    void shouldPrintStackTraceFor500s() {
        HttpProblem problem = HttpProblem.builder()
                .withTitle("my fault")
                .withStatus(INTERNAL_SERVER_ERROR)
                .build();
        RuntimeException cause = new RuntimeException("hey");

        processor.apply(problem, ProblemContextMother.withCause(cause));

        verify(logger).error("status=500, title=\"my fault\"", cause);
    }

    @Test
    void shouldLog4xxAtInfoWhenErrorIsDisabled() {
        when(logger.isEnabled(Logger.Level.ERROR)).thenReturn(false);
        when(logger.isInfoEnabled()).thenReturn(true);

        HttpProblem problem = HttpProblem.builder()
                .withTitle("your fault")
                .withStatus(BAD_REQUEST)
                .build();

        processor.apply(problem, simpleContext());

        verify(logger).info("status=400, title=\"your fault\"");
    }

    @Test
    void shouldNotLog4xxWhenInfoIsDisabled() {
        when(logger.isEnabled(Logger.Level.ERROR)).thenReturn(true);
        when(logger.isInfoEnabled()).thenReturn(false);

        HttpProblem problem = HttpProblem.builder()
                .withTitle("your fault")
                .withStatus(BAD_REQUEST)
                .build();

        processor.apply(problem, simpleContext());

        verify(logger, never()).info(anyString());
    }

    @Test
    void shouldNotLog5xxWhenErrorIsDisabled() {
        when(logger.isEnabled(Logger.Level.ERROR)).thenReturn(false);
        when(logger.isInfoEnabled()).thenReturn(true);

        HttpProblem problem = HttpProblem.builder()
                .withTitle("my fault")
                .withStatus(INTERNAL_SERVER_ERROR)
                .build();
        RuntimeException cause = new RuntimeException("hey");

        processor.apply(problem, ProblemContextMother.withCause(cause));

        verify(logger, never()).error(anyString(), any(Throwable.class));
    }

    @Test
    void shouldLogAtWarnWhenConfiguredForStatusClass() {
        ProblemLoggingConfig config = new ProblemLoggingConfig(
                Map.of("4xx", ProblemLogLevel.WARN, "5xx", ProblemLogLevel.ERROR),
                List.of("5xx"));
        ProblemLogger warnProcessor = new ProblemLogger(logger, config);

        HttpProblem problem = HttpProblem.builder()
                .withTitle("your fault")
                .withStatus(BAD_REQUEST)
                .build();

        warnProcessor.apply(problem, simpleContext());

        verify(logger).warn("status=400, title=\"your fault\"");
    }

    @Test
    void shouldLogAtExactCodeLevelOverridingClass() {
        ProblemLoggingConfig config = new ProblemLoggingConfig(
                Map.of("4xx", ProblemLogLevel.INFO, "401", ProblemLogLevel.WARN),
                List.of("5xx"));
        ProblemLogger overrideProcessor = new ProblemLogger(logger, config);

        HttpProblem problem = HttpProblem.builder()
                .withTitle("unauthorized")
                .withStatus(UNAUTHORIZED)
                .build();

        overrideProcessor.apply(problem, simpleContext());

        verify(logger).warn("status=401, title=\"unauthorized\"");
        verify(logger, never()).info(anyString());
    }

    @Test
    void shouldIncludeStackTraceForExactCode() {
        ProblemLoggingConfig config = new ProblemLoggingConfig(
                Map.of("4xx", ProblemLogLevel.WARN),
                List.of("403"));
        ProblemLogger stackProcessor = new ProblemLogger(logger, config);

        HttpProblem problem = HttpProblem.builder()
                .withTitle("forbidden")
                .withStatus(FORBIDDEN)
                .build();
        RuntimeException cause = new RuntimeException("denied");

        stackProcessor.apply(problem, ProblemContextMother.withCause(cause));

        verify(logger).warn("status=403, title=\"forbidden\"", cause);
    }

    @Test
    void shouldNotLogWhenLevelIsOff() {
        ProblemLoggingConfig config = new ProblemLoggingConfig(
                Map.of("4xx", ProblemLogLevel.OFF),
                List.of());
        ProblemLogger offProcessor = new ProblemLogger(logger, config);

        HttpProblem problem = HttpProblem.builder()
                .withTitle("your fault")
                .withStatus(BAD_REQUEST)
                .build();

        offProcessor.apply(problem, simpleContext());

        verify(logger, never()).info(anyString());
        verify(logger, never()).warn(anyString());
        verify(logger, never()).error(anyString());
        verify(logger, never()).debug(anyString());
        verify(logger, never()).trace(anyString());
    }

    @Test
    void shouldSanitizeNewlinesInTitle() {
        HttpProblem problem = HttpProblem.builder()
                .withTitle("legit\nINFO [fake] forged log line")
                .withStatus(BAD_REQUEST)
                .build();

        processor.apply(problem, simpleContext());

        verify(logger).info("status=400, title=\"legit_INFO [fake] forged log line\"");
    }

    @Test
    void shouldSanitizeNewlinesInDetail() {
        HttpProblem problem = HttpProblem.builder()
                .withTitle("error")
                .withDetail("line1\r\nline2")
                .withStatus(BAD_REQUEST)
                .build();

        processor.apply(problem, simpleContext());

        verify(logger).info("status=400, title=\"error\", detail=\"line1__line2\"");
    }

    @Test
    void shouldSanitizeAnsiEscapesInDetail() {
        HttpProblem problem = HttpProblem.builder()
                .withTitle("error")
                .withDetail("normal [31mred text[0m end")
                .withStatus(BAD_REQUEST)
                .build();

        processor.apply(problem, simpleContext());

        verify(logger).info("status=400, title=\"error\", detail=\"normal _red text_ end\"");
    }

    @Test
    void shouldSanitizeCustomParameterValues() {
        HttpProblem problem = HttpProblem.builder()
                .withTitle("error")
                .withStatus(BAD_REQUEST)
                .with("injected", "value\nERROR [fake] forged")
                .build();

        processor.apply(problem, simpleContext());

        verify(logger).info("status=400, title=\"error\", injected=\"value_ERROR [fake] forged\"");
    }

    @Test
    void shouldSanitizeParameterKeys() {
        HttpProblem problem = HttpProblem.builder()
                .withTitle("error")
                .withStatus(BAD_REQUEST)
                .with("bad\nkey", "value")
                .build();

        processor.apply(problem, simpleContext());

        verify(logger).info("status=400, title=\"error\", bad_key=\"value\"");
    }

    @Test
    void sanitizeShouldReturnNullForNull() {
        assertThat(ProblemLogger.sanitize(null)).isNull();
    }

    @Test
    void sanitizeShouldPassThroughCleanStrings() {
        assertThat(ProblemLogger.sanitize("clean string 123")).isEqualTo("clean string 123");
    }

    @Test
    void sanitizeShouldReplaceTabsAndOtherControlChars() {
        assertThat(ProblemLogger.sanitize("a\tb c")).isEqualTo("a_b_c");
    }

}
