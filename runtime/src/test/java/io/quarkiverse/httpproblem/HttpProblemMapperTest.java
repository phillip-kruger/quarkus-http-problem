package io.quarkiverse.httpproblem;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import io.quarkiverse.httpproblem.postprocessing.PostProcessorsRegistry;
import io.quarkiverse.httpproblem.postprocessing.ProblemDefaultsProvider;
import io.quarkiverse.httpproblem.postprocessing.ProblemLogger;
import io.quarkiverse.httpproblem.postprocessing.ProblemLoggingConfig;

class HttpProblemMapperTest {

    PostProcessorsRegistry registry = new PostProcessorsRegistry(
            List.of(new ProblemLogger(ProblemLoggingConfig.defaults()), new ProblemDefaultsProvider()));
    HttpProblemMapper mapper = new HttpProblemMapper(registry);

    @Test
    void responseShouldIncludeHeaders() {
        HttpProblem problem = HttpProblem.builder()
                .withHeader("X-Numeric-Header", 123)
                .withHeader("X-String-Header", "ABC")
                .build();

        Response response = mapper.toResponse(problem);

        assertThat(response.getHeaderString("X-Numeric-Header")).isEqualTo("123");
        assertThat(response.getHeaderString("X-String-Header")).isEqualTo("ABC");
    }

}
