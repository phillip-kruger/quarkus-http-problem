package io.quarkiverse.httpproblem.jsonb;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.json.bind.JsonbException;
import jakarta.ws.rs.Priorities;

import io.quarkiverse.httpproblem.DetailSanitizer;
import io.quarkiverse.httpproblem.ExceptionMapperBase;
import io.quarkiverse.httpproblem.HttpProblem;
import io.quarkiverse.httpproblem.postprocessing.PostProcessorsRegistry;

@Priority(Priorities.USER)
public final class JsonbExceptionMapper extends ExceptionMapperBase<JsonbException> {

    private final DetailSanitizer detailSanitizer;

    public JsonbExceptionMapper() {
        this.detailSanitizer = new DetailSanitizer();
    }

    @Inject
    public JsonbExceptionMapper(PostProcessorsRegistry postProcessorsRegistry,
            DetailSanitizer detailSanitizer) {
        super(postProcessorsRegistry);
        this.detailSanitizer = detailSanitizer;
    }

    @Override
    protected HttpProblem toProblem(JsonbException exception) {
        String rawDetail = exception.getCause() == null ? null : exception.getCause().getMessage();
        return HttpProblem.valueOf(BAD_REQUEST, detailSanitizer.sanitize(rawDetail));
    }
}
