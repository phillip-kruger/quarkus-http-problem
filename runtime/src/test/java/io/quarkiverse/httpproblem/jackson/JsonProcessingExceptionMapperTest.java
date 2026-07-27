package io.quarkiverse.httpproblem.jackson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParseException;

import io.quarkiverse.httpproblem.HttpProblem;
import io.quarkiverse.httpproblem.ProblemRuntimeFixedConfig;
import io.quarkiverse.httpproblem.postprocessing.PostProcessorsRegistry;
import io.quarkiverse.httpproblem.postprocessing.ProblemDefaultsProvider;
import io.quarkiverse.httpproblem.postprocessing.ProblemLogger;

class JsonProcessingExceptionMapperTest {

    PostProcessorsRegistry registry = new PostProcessorsRegistry(
            List.of(new ProblemLogger(), new ProblemDefaultsProvider()));

    @Test
    void shouldProduceHttp400WithOriginalMessageWhenIncludeDetails() {
        JsonProcessingExceptionMapper mapper = new JsonProcessingExceptionMapper(registry, configWith(true));
        JsonParseException exception = new JsonParseException("Unexpected end-of-input");

        Response response = mapper.toResponse(exception);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getMediaType()).isEqualTo(HttpProblem.MEDIA_TYPE);
        assertThat(response.getEntity())
                .isInstanceOf(HttpProblem.class)
                .hasFieldOrPropertyWithValue("detail", "Unexpected end-of-input");
    }

    @Test
    void shouldSanitizeDetailByDefault() {
        JsonProcessingExceptionMapper mapper = new JsonProcessingExceptionMapper(registry, configWith(false));
        JsonParseException exception = new JsonParseException("Unexpected end-of-input");

        Response response = mapper.toResponse(exception);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity())
                .isInstanceOf(HttpProblem.class)
                .hasFieldOrPropertyWithValue("detail", JsonProcessingExceptionMapper.SANITIZED_DETAIL);
    }

    private static ProblemRuntimeFixedConfig configWith(boolean includeDetails) {
        ProblemRuntimeFixedConfig config = mock(ProblemRuntimeFixedConfig.class);
        when(config.includeDetails()).thenReturn(includeDetails);
        return config;
    }
}
