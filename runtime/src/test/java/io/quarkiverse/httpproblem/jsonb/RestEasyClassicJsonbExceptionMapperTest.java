package io.quarkiverse.httpproblem.jsonb;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.json.bind.JsonbException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import io.quarkiverse.httpproblem.DetailSanitizer;
import io.quarkiverse.httpproblem.HttpProblem;
import io.quarkiverse.httpproblem.postprocessing.PostProcessorsRegistry;
import io.quarkiverse.httpproblem.postprocessing.ProblemDefaultsProvider;
import io.quarkiverse.httpproblem.postprocessing.ProblemLogger;

class RestEasyClassicJsonbExceptionMapperTest {

    PostProcessorsRegistry registry = new PostProcessorsRegistry(
            List.of(new ProblemLogger(), new ProblemDefaultsProvider()));
    RestEasyClassicJsonbExceptionMapper mapper = new RestEasyClassicJsonbExceptionMapper(registry, new DetailSanitizer(true));

    @Test
    void processingExceptionShouldProduceHttp500() {
        ProcessingException exception = new ProcessingException("Something is wrong");

        Response response = mapper.toResponse(exception);

        assertThat(response.getStatus()).isEqualTo(500);
    }

    @Test
    void processingExceptionWithJsonbExceptionCauseShouldProduceHttp400() {
        ProcessingException exception = new ProcessingException(new JsonbException("Something is wrong"));

        Response response = mapper.toResponse(exception);

        assertThat(response.getStatus()).isEqualTo(400);
    }

    @Test
    void shouldSanitizeDetailByDefault() {
        RestEasyClassicJsonbExceptionMapper sanitizedMapper = new RestEasyClassicJsonbExceptionMapper(registry,
                new DetailSanitizer(false));
        ProcessingException exception = new ProcessingException(new JsonbException("Internal class details leaked"));

        Response response = sanitizedMapper.toResponse(exception);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity())
                .isInstanceOf(HttpProblem.class)
                .hasFieldOrPropertyWithValue("detail", DetailSanitizer.SANITIZED_DETAIL);
    }

    @Test
    void shouldPreserveDetailWhenIncludeDetails() {
        ProcessingException exception = new ProcessingException(new JsonbException("Something is wrong"));

        Response response = mapper.toResponse(exception);

        assertThat(response.getEntity())
                .isInstanceOf(HttpProblem.class)
                .hasFieldOrPropertyWithValue("detail", "Something is wrong");
    }
}
