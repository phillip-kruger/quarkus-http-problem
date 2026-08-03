package io.quarkiverse.httpproblem.postprocessing;

import static io.quarkiverse.httpproblem.postprocessing.ProblemContextMother.simpleContext;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;
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

}
