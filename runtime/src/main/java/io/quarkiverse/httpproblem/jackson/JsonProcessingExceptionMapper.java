package io.quarkiverse.httpproblem.jackson;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.quarkiverse.httpproblem.ExceptionMapperBase;
import io.quarkiverse.httpproblem.HttpProblem;
import io.quarkiverse.httpproblem.ProblemRuntimeFixedConfig;
import io.quarkiverse.httpproblem.postprocessing.PostProcessorsRegistry;

/**
 * Mapper for Jackson payload processing exceptions.
 */
@Priority(Priorities.USER)
public final class JsonProcessingExceptionMapper extends ExceptionMapperBase<JsonProcessingException> {

    static final String SANITIZED_DETAIL = "Malformed request body";

    private boolean includeDetails;

    public JsonProcessingExceptionMapper() {
        this.includeDetails = false;
    }

    @Inject
    public JsonProcessingExceptionMapper(PostProcessorsRegistry postProcessorsRegistry, ProblemRuntimeFixedConfig config) {
        super(postProcessorsRegistry);
        this.includeDetails = config.includeDetails();
    }

    @Override
    protected HttpProblem toProblem(JsonProcessingException exception) {
        String detail = includeDetails
                ? exception.getOriginalMessage()
                : SANITIZED_DETAIL;
        return HttpProblem.valueOf(BAD_REQUEST, detail);
    }
}
