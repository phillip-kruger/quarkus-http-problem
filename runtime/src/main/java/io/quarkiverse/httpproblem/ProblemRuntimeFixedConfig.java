package io.quarkiverse.httpproblem;

import java.util.Set;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "quarkus.http-problem")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface ProblemRuntimeFixedConfig {

    /**
     * MDC properties that should be included in problem responses.
     */
    @WithDefault("uuid")
    Set<String> includeMdcProperties();

    /**
     * When enabled, JSON parsing/binding exception mappers include the raw exception message
     * in the problem detail field. When disabled (the default), a generic message is used
     * to avoid leaking internal class names and implementation details.
     */
    @WithDefault("false")
    boolean includeDetails();

    /**
     * Constraint violation configuration.
     */
    @WithName("constraint-violation")
    ConstraintViolationConfig constraintViolation();

    interface ConstraintViolationConfig {

        /**
         * Response status code when ConstraintViolationException is thrown.
         */
        @WithDefault("400")
        int status();

        /**
         * Response title when ConstraintViolationException is thrown.
         */
        @WithDefault("Bad Request")
        String title();

        /**
         * OpenApi description for ConstraintViolationExceptions.
         */
        @WithDefault("Bad request: server would not process the request due to something the server considered to be a client error")
        String description();
    }
}
