package io.quarkiverse.httpproblem.jsonb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import jakarta.json.bind.JsonbException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import io.quarkiverse.httpproblem.HttpProblem;
import io.quarkiverse.httpproblem.ProblemRuntimeFixedConfig;
import io.quarkiverse.httpproblem.postprocessing.PostProcessorsRegistry;
import io.quarkiverse.httpproblem.postprocessing.ProblemDefaultsProvider;
import io.quarkiverse.httpproblem.postprocessing.ProblemLogger;

class JsonbExceptionMapperTest {

    PostProcessorsRegistry registry = new PostProcessorsRegistry(
            List.of(new ProblemLogger(), new ProblemDefaultsProvider()));

    @Test
    void shouldProduceHttp400WithCauseMessageWhenIncludeDetails() {
        JsonbExceptionMapper mapper = new JsonbExceptionMapper(registry, configWith(true));
        JsonbException exception = new JsonbException("wrapper", new RuntimeException("Invalid UUID string: ABC"));

        Response response = mapper.toResponse(exception);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getMediaType()).isEqualTo(HttpProblem.MEDIA_TYPE);
        assertThat(response.getEntity())
                .isInstanceOf(HttpProblem.class)
                .hasFieldOrPropertyWithValue("detail", "Invalid UUID string: ABC");
    }

    @Test
    void shouldSanitizeDetailByDefault() {
        JsonbExceptionMapper mapper = new JsonbExceptionMapper(registry, configWith(false));
        JsonbException exception = new JsonbException("wrapper", new RuntimeException("Invalid UUID string: ABC"));

        Response response = mapper.toResponse(exception);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity())
                .isInstanceOf(HttpProblem.class)
                .hasFieldOrPropertyWithValue("detail", JsonbExceptionMapper.SANITIZED_DETAIL);
    }

    @Test
    void shouldPreserveDetailWhenIncludeDetails() {
        JsonbExceptionMapper mapper = new JsonbExceptionMapper(registry, configWith(true));
        JsonbException exception = new JsonbException("wrapper", new RuntimeException("Internal error details"));

        Response response = mapper.toResponse(exception);

        assertThat(response.getEntity())
                .isInstanceOf(HttpProblem.class)
                .hasFieldOrPropertyWithValue("detail", "Internal error details");
    }

    @Test
    void shouldSanitizeEvenWithNoCause() {
        JsonbExceptionMapper mapper = new JsonbExceptionMapper(registry, configWith(false));
        JsonbException exception = new JsonbException("wrapper");

        Response response = mapper.toResponse(exception);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity())
                .isInstanceOf(HttpProblem.class)
                .hasFieldOrPropertyWithValue("detail", JsonbExceptionMapper.SANITIZED_DETAIL);
    }

    private static ProblemRuntimeFixedConfig configWith(boolean includeDetails) {
        ProblemRuntimeFixedConfig config = mock(ProblemRuntimeFixedConfig.class);
        when(config.includeDetails()).thenReturn(includeDetails);
        return config;
    }
}
