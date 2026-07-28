package io.quarkiverse.httpproblem.jsonb;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.ProcessingException;

import io.quarkiverse.httpproblem.DetailSanitizer;
import io.quarkiverse.httpproblem.ExceptionMapperBase;
import io.quarkiverse.httpproblem.HttpProblem;
import io.quarkiverse.httpproblem.postprocessing.PostProcessorsRegistry;

@Priority(Priorities.USER)
public final class RestEasyClassicJsonbExceptionMapper extends ExceptionMapperBase<ProcessingException> {

    private final DetailSanitizer detailSanitizer;

    public RestEasyClassicJsonbExceptionMapper() {
        this.detailSanitizer = new DetailSanitizer();
    }

    @Inject
    public RestEasyClassicJsonbExceptionMapper(PostProcessorsRegistry postProcessorsRegistry,
            DetailSanitizer detailSanitizer) {
        super(postProcessorsRegistry);
        this.detailSanitizer = detailSanitizer;
    }

    /**
     * Unfortunately Quarkus+JsonB throws ProcessingException, not JsonbException in case of malformed payload body, so `cause`
     * needs to be checked explicitly.
     *
     * For native mode compatibility instanceof operator is not used to check cause type.
     */
    @Override
    protected HttpProblem toProblem(ProcessingException exception) {
        if (exception.getCause() != null
                && exception.getCause().getClass().getName().equals("jakarta.json.bind.JsonbException")) {
            return HttpProblem.valueOf(BAD_REQUEST, detailSanitizer.sanitize(exception.getCause().getMessage()));
        } else {
            return HttpProblem.valueOf(INTERNAL_SERVER_ERROR);
        }
    }
}
