package io.quarkiverse.httpproblem.deployment;

import static io.quarkiverse.httpproblem.validation.ConstraintViolationExceptionMapper.HTTP_VALIDATION_PROBLEM_STATUS_CODE;

import java.util.Map;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;

import io.quarkiverse.httpproblem.HttpProblem;
import io.quarkiverse.httpproblem.ProblemRuntimeFixedConfig;

/**
 * OpenAPI build-time filter that automatically augments various OpenApi model parts:
 * - error responses without explicit @Content defined in @APIResponse
 * - updates status code and description of HttpValidationProblem according to configuration
 */
public class OpenApiProblemFilter implements OASFilter {

    private final ProblemRuntimeFixedConfig runtimeConfig;
    private final Content problemContent;
    private final Content validationProblemContent;

    public OpenApiProblemFilter(ProblemBuildConfig config, ProblemRuntimeFixedConfig runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
        this.problemContent = createContent(config.openapi().defaultSchema());
        this.validationProblemContent = createContent(config.openapi().validationProblemSchema());
    }

    /**
     * HttpValidationProblem is configurable in regard to status code (e.g. 422 instead of default 400). But ApiResponse
     * annotation on ConstraintViolationExceptionMapper obviously cannot use even a build time config: annotations must
     * use pure constants. Because of this, a fake special responseCode for this specific responses is introduced:
     * <HttpValidationProblem>. Once it is detected, it is augmented with status code and description defined in the
     * config.
     */
    @Override
    public Operation filterOperation(Operation operation) {
        if (operation.getResponses().hasAPIResponse(HTTP_VALIDATION_PROBLEM_STATUS_CODE)) {
            APIResponse response = operation.getResponses().getAPIResponse(HTTP_VALIDATION_PROBLEM_STATUS_CODE)
                    .description(runtimeConfig.constraintViolation().description())
                    .content(validationProblemContent);

            operation.getResponses().addAPIResponse(String.valueOf(runtimeConfig.constraintViolation().status()), response);
            operation.getResponses().removeAPIResponse(HTTP_VALIDATION_PROBLEM_STATUS_CODE);
        }

        addProblemContentToErrorResponses(operation);

        return operation;
    }

    /**
     * Augments HttpProblem schema for 4xx and 5xx error @ApiResponses that don't have explicit @Content defined.
     */
    private void addProblemContentToErrorResponses(Operation operation) {
        Map<String, APIResponse> responses = operation.getResponses().getAPIResponses();
        if (responses == null) {
            return;
        }

        for (Map.Entry<String, APIResponse> entry : responses.entrySet()) {
            APIResponse apiResponse = entry.getValue();
            if (apiResponse == null || apiResponse.getRef() != null || apiResponse.getContent() != null) {
                continue;
            }

            try {
                int httpStatus = Integer.parseInt(entry.getKey());
                if (httpStatus >= 400) {
                    apiResponse.setContent(problemContent);
                }
            } catch (NumberFormatException e) {
                // skip non-numeric codes like "default"
            }
        }
    }

    private static Content createContent(String schemaName) {
        Schema schema = OASFactory.createSchema();
        schema.setRef("#/components/schemas/" + schemaName);

        MediaType mediaType = OASFactory.createMediaType();
        mediaType.setSchema(schema);

        Content content = OASFactory.createContent();
        content.addMediaType(HttpProblem.MEDIA_TYPE.toString(), mediaType);
        return content;
    }
}
