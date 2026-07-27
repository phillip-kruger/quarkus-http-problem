package io.quarkiverse.httpproblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.zalando.problem.Status.BAD_REQUEST;

import java.util.List;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;
import org.zalando.problem.ThrowableProblem;

import io.quarkiverse.httpproblem.postprocessing.PostProcessorsRegistry;
import io.quarkiverse.httpproblem.postprocessing.ProblemDefaultsProvider;
import io.quarkiverse.httpproblem.postprocessing.ProblemLogger;
import io.quarkiverse.httpproblem.postprocessing.ProblemLoggingConfig;

class ZalandoProblemMapperTest {

    PostProcessorsRegistry registry = new PostProcessorsRegistry(
            List.of(new ProblemLogger(ProblemLoggingConfig.defaults()), new ProblemDefaultsProvider()));
    ZalandoProblemMapper mapper = new ZalandoProblemMapper(registry);

    @Test
    void responseShouldUseProblemStatus() {
        ThrowableProblem problem = Problem.builder()
                .withTitle("There's something wrong with your request")
                .withStatus(BAD_REQUEST)
                .build();

        Response response = mapper.toResponse(problem);

        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.getStatusCode());
    }

    @Test
    void problemWithoutStatusShouldDefaultTo500() {
        ThrowableProblem exception = Problem.builder().build();

        Response response = mapper.toResponse(exception);

        assertThat(response.getStatus()).isEqualTo(500);
    }

}
