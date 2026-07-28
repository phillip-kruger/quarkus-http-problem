package io.quarkiverse.httpproblem.jackson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;

import io.quarkiverse.httpproblem.DetailSanitizer;
import io.quarkiverse.httpproblem.HttpProblem;
import io.quarkiverse.httpproblem.postprocessing.PostProcessorsRegistry;
import io.quarkiverse.httpproblem.postprocessing.ProblemDefaultsProvider;
import io.quarkiverse.httpproblem.postprocessing.ProblemLogger;

class InvalidDefinitionExceptionMapperTest {

    PostProcessorsRegistry registry = new PostProcessorsRegistry(
            List.of(new ProblemLogger(), new ProblemDefaultsProvider()));
    InvalidDefinitionExceptionMapper mapper = new InvalidDefinitionExceptionMapper(registry, new DetailSanitizer(true));

    @Test
    void shouldProduceHttp400WithFieldInfo() {
        InvalidDefinitionException exception = buildExceptionWithPath(
                new JsonMappingException.Reference(this, "customFieldName"));

        Response response = mapper.toResponse(exception);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getMediaType()).isEqualTo(HttpProblem.MEDIA_TYPE);
        assertThat(response.getEntity())
                .isInstanceOf(HttpProblem.class)
                .hasFieldOrPropertyWithValue("detail", "Invalid definition")
                .hasFieldOrPropertyWithValue("parameters.field", "customFieldName");
    }

    @Test
    void emptyPathShouldNotCrash() {
        InvalidDefinitionException exception = buildExceptionWithPath();

        Response response = mapper.toResponse(exception);

        assertThat(response.getEntity())
                .isInstanceOf(HttpProblem.class)
                .hasFieldOrPropertyWithValue("parameters.field", "?");
    }

    @Test
    void shouldSanitizeDetailByDefault() {
        InvalidDefinitionExceptionMapper sanitizedMapper = new InvalidDefinitionExceptionMapper(registry,
                new DetailSanitizer(false));
        InvalidDefinitionException exception = buildExceptionWithPath(
                new JsonMappingException.Reference(this, "customFieldName"));

        Response response = sanitizedMapper.toResponse(exception);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity())
                .isInstanceOf(HttpProblem.class)
                .hasFieldOrPropertyWithValue("detail", DetailSanitizer.SANITIZED_DETAIL)
                .hasFieldOrPropertyWithValue("parameters.field", "customFieldName");
    }

    @Test
    void shouldPreserveDetailWhenIncludeDetails() {
        InvalidDefinitionException exception = buildExceptionWithPath(
                new JsonMappingException.Reference(this, "customFieldName"));

        Response response = mapper.toResponse(exception);

        assertThat(response.getEntity())
                .isInstanceOf(HttpProblem.class)
                .hasFieldOrPropertyWithValue("detail", "Invalid definition");
    }

    private InvalidDefinitionException buildExceptionWithPath(JsonMappingException.Reference... pathSegments) {
        InvalidDefinitionException exception = InvalidDefinitionException.from(mock(JsonParser.class),
                "Invalid definition", (com.fasterxml.jackson.databind.JavaType) null);

        for (int i = pathSegments.length - 1; i >= 0; --i) {
            exception.prependPath(pathSegments[i]);
        }
        return exception;
    }
}
