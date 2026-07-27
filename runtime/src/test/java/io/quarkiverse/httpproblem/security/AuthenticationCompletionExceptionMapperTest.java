package io.quarkiverse.httpproblem.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import io.quarkiverse.httpproblem.postprocessing.PostProcessorsRegistry;
import io.quarkiverse.httpproblem.postprocessing.ProblemDefaultsProvider;
import io.quarkiverse.httpproblem.postprocessing.ProblemLogger;
import io.quarkiverse.httpproblem.postprocessing.ProblemLoggingConfig;
import io.quarkus.security.AuthenticationCompletionException;

class AuthenticationCompletionExceptionMapperTest {

    PostProcessorsRegistry registry = new PostProcessorsRegistry(
            List.of(new ProblemLogger(ProblemLoggingConfig.defaults()), new ProblemDefaultsProvider()));
    AuthenticationCompletionExceptionMapper mapper = new AuthenticationCompletionExceptionMapper(registry);

    @Test
    void shouldProduceHttp401() {
        AuthenticationCompletionException exception = new AuthenticationCompletionException();

        Response response = mapper.toResponse(exception);

        assertThat(response.getStatus()).isEqualTo(401);
    }
}
