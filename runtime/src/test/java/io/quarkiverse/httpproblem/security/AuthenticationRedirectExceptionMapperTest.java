package io.quarkiverse.httpproblem.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import io.quarkiverse.httpproblem.postprocessing.PostProcessorsRegistry;
import io.quarkiverse.httpproblem.postprocessing.ProblemDefaultsProvider;
import io.quarkiverse.httpproblem.postprocessing.ProblemLogger;
import io.quarkiverse.httpproblem.postprocessing.ProblemLoggingConfig;
import io.quarkus.security.AuthenticationRedirectException;

class AuthenticationRedirectExceptionMapperTest {

    PostProcessorsRegistry registry = new PostProcessorsRegistry(
            List.of(new ProblemLogger(ProblemLoggingConfig.defaults()), new ProblemDefaultsProvider()));
    AuthenticationRedirectExceptionMapper mapper = new AuthenticationRedirectExceptionMapper(registry);

    @Test
    void shouldProduceHttp302WithAllNeededHeaders() {
        AuthenticationRedirectException exception = new AuthenticationRedirectException("/login");

        Response response = mapper.toResponse(exception);

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getHeaderString(HttpHeaders.LOCATION)).isEqualTo("/login");
        assertThat(response.getHeaderString(HttpHeaders.CACHE_CONTROL)).isEqualTo("no-store");
        assertThat(response.getHeaderString("Pragma")).isEqualTo("no-cache");
    }
}
