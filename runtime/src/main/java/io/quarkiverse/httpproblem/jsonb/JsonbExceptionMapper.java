package io.quarkiverse.httpproblem.jsonb;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.json.bind.JsonbException;
import jakarta.ws.rs.Priorities;

import io.quarkiverse.httpproblem.ExceptionMapperBase;
import io.quarkiverse.httpproblem.HttpProblem;
import io.quarkiverse.httpproblem.ProblemRuntimeFixedConfig;
import io.quarkiverse.httpproblem.postprocessing.PostProcessorsRegistry;

@Priority(Priorities.USER)
public final class JsonbExceptionMapper extends ExceptionMapperBase<JsonbException> {

    static final String SANITIZED_DETAIL = "Malformed request body";

    private boolean includeDetails;

    public JsonbExceptionMapper() {
        this.includeDetails = false;
    }

    @Inject
    public JsonbExceptionMapper(PostProcessorsRegistry postProcessorsRegistry, ProblemRuntimeFixedConfig config) {
        super(postProcessorsRegistry);
        this.includeDetails = config.includeDetails();
    }

    @Override
    protected HttpProblem toProblem(JsonbException exception) {
        if (!includeDetails) {
            return HttpProblem.valueOf(BAD_REQUEST, SANITIZED_DETAIL);
        }
        return HttpProblem.valueOf(BAD_REQUEST, exception.getCause() == null ? null : exception.getCause().getMessage());
    }
}
