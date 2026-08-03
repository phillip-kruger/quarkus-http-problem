package io.quarkiverse.httpproblem.jaxrs;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import io.quarkiverse.httpproblem.postprocessing.PostProcessorsRegistry;
import io.quarkiverse.httpproblem.postprocessing.ProblemDefaultsProvider;
import io.quarkiverse.httpproblem.postprocessing.ProblemLogger;
import io.quarkiverse.httpproblem.postprocessing.ProblemLoggingConfig;

class NotFoundExceptionMapperTest {

    PostProcessorsRegistry registry = new PostProcessorsRegistry(
            List.of(new ProblemLogger(ProblemLoggingConfig.defaults()), new ProblemDefaultsProvider()));
    NotFoundExceptionMapper mapper = new NotFoundExceptionMapper(registry);

    @Test
    void shouldProduceHttp404() {
        NotFoundException exception = new NotFoundException();

        Response response = mapper.toResponse(exception);

        assertThat(response.getStatus()).isEqualTo(404);
    }

}
