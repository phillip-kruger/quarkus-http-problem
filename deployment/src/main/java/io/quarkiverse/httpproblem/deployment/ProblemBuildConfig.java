package io.quarkiverse.httpproblem.deployment;

import java.util.List;
import java.util.Map;

import io.quarkiverse.httpproblem.postprocessing.ProblemLogLevel;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "quarkus.http-problem")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface ProblemBuildConfig {

    /**
     * OpenApi related configuration
     */
    @WithName("openapi")
    OpenApiConfig openapi();

    interface OpenApiConfig {

        /**
         * Which schema should be used by default for problem responses
         */
        @WithName("default-schema")
        @WithDefault("HttpProblem")
        String defaultSchema();

        /**
         * Which schema should be used by default for validation problem responses
         */
        @WithName("validation-problem-schema")
        @WithDefault("HttpValidationProblem")
        String validationProblemSchema();
    }

    /**
     * Per-mapper configuration, keyed by exception simple name in kebab-case
     * (e.g. {@code web-application-exception}, {@code not-found-exception}).
     * <p>
     * Use this to disable specific built-in exception mappers:
     *
     * <pre>
     * quarkus.http-problem.mapper.not-found-exception.enabled=false
     * </pre>
     */
    @WithName("mapper")
    Map<String, MapperConfig> mapper();

    interface MapperConfig {
        /**
         * Whether the mapper for this exception type should be registered.
         */
        @WithDefault("true")
        boolean enabled();
    }

    /**
     * Logging configuration for HTTP problem responses.
     */
    @WithName("logging")
    LoggingConfig logging();

    interface LoggingConfig {
        /**
         * Whether problem logging is enabled.
         */
        @WithDefault("true")
        boolean enabled();

        /**
         * Log level per HTTP status code class ({@code 4xx}, {@code 5xx}) or exact status code ({@code 401}).
         * <p>
         * Exact codes take precedence over status classes. Unconfigured classes default to {@code INFO}.
         * <p>
         * Example:
         *
         * <pre>
         * quarkus.http-problem.logging.level.4xx=DEBUG
         * quarkus.http-problem.logging.level.5xx=ERROR
         * quarkus.http-problem.logging.level.401=WARN
         * </pre>
         */
        @WithName("level")
        Map<String, ProblemLogLevel> level();

        /**
         * Comma-separated list of status code classes ({@code 5xx}) or exact codes ({@code 403})
         * for which the original exception stack trace is included in the log output.
         */
        @WithDefault("5xx")
        @WithName("include-stack-trace")
        List<String> includeStackTrace();
    }
}
