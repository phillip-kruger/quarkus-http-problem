package io.quarkiverse.httpproblem.jackson;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import io.quarkiverse.httpproblem.ExceptionMapperBase;
import io.quarkiverse.httpproblem.HttpProblem;
import io.quarkiverse.httpproblem.ProblemRuntimeFixedConfig;
import io.quarkiverse.httpproblem.postprocessing.PostProcessorsRegistry;

/**
 * Mapper for Jackson InvalidFormatException, which is more specialised version of JsonProcessingException
 */
@Priority(Priorities.USER)
public final class InvalidFormatExceptionMapper extends ExceptionMapperBase<InvalidFormatException> {

    static final String SANITIZED_DETAIL = "Malformed request body";

    private boolean includeDetails;

    public InvalidFormatExceptionMapper() {
        this.includeDetails = false;
    }

    @Inject
    public InvalidFormatExceptionMapper(PostProcessorsRegistry postProcessorsRegistry, ProblemRuntimeFixedConfig config) {
        super(postProcessorsRegistry);
        this.includeDetails = config.includeDetails();
    }

    @Override
    protected HttpProblem toProblem(InvalidFormatException exception) {
        String detail = includeDetails
                ? exception.getOriginalMessage()
                : SANITIZED_DETAIL;
        return HttpProblem.builder()
                .withStatus(Response.Status.BAD_REQUEST)
                .withTitle(Response.Status.BAD_REQUEST.getReasonPhrase())
                .withDetail(detail)
                .with("field", serializePath(exception.getPath()))
                .build();
    }

    private String serializePath(List<JsonMappingException.Reference> path) {
        String pathString = path.stream()
                .map(this::refToString)
                .collect(Collectors.joining());
        return removeFirstDot(pathString);
    }

    private String refToString(JsonMappingException.Reference ref) {
        if (ref.getFieldName() != null) {
            return "." + ref.getFieldName();
        }
        if (ref.getIndex() >= 0) {
            return "[" + ref.getIndex() + "]";
        }
        return ".?";
    }

    private String removeFirstDot(String field) {
        if (field.length() > 1) {
            return field.substring(1);
        } else {
            return "?";
        }
    }
}
