package io.quarkiverse.httpproblem.deployment;

import java.util.Map;

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
}
