package io.quarkiverse.httpproblem.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.Map;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkus.deployment.Capabilities;

class ProblemProcessorTest {

    static final Capabilities CAPABILITIES_WITH_JSON = new Capabilities(Collections.singleton("io.quarkus.jackson"));
    static final Capabilities CAPABILITIES_WITHOUT_JSON = new Capabilities(Collections.singleton("io.quarkus.resteasy"));

    final Logger logger = mock(Logger.class);
    final ProblemProcessor problemProcessor = createProcessorWith(logger);

    /**
     * That's the only way I found to inject mock logger into ProblemProcessor. Quarkus forbids having multiple constructors in
     * deployment processors, or have fields that are not @BuildItem, which makes it impossible to inject anything via
     * constructor or even setter.
     */
    private ProblemProcessor createProcessorWith(Logger logger) {
        return new ProblemProcessor() {
            @Override
            protected Logger logger() {
                return logger;
            }
        };
    }

    @Test
    void featureNameShouldBeValid() {
        assertThat(problemProcessor.createFeature(CAPABILITIES_WITH_JSON).getName())
                .isEqualTo("http-problem");

        verify(logger, times(0)).error(anyString());
    }

    @Test
    void shouldLogErrorIfMissingJsonCapability() {
        problemProcessor.createFeature(CAPABILITIES_WITHOUT_JSON);

        verify(logger).error("`quarkus-http-problem` extension is useless without json provider. "
                + "Please add `quarkus-rest-jackson` or `quarkus-rest-jsonb` (or classic `resteasy` equivalent) extension to your project.");
    }

    @ParameterizedTest
    @CsvSource({
            "WebApplicationException, web-application-exception",
            "NotFoundException, not-found-exception",
            "ForbiddenException, forbidden-exception",
            "HttpProblem, http-problem",
            "UnauthorizedException, unauthorized-exception",
            "AuthenticationFailedException, authentication-failed-exception",
            "ConstraintViolationException, constraint-violation-exception",
            "JsonProcessingException, json-processing-exception",
            "Exception, exception"
    })
    void toKebabCaseShouldConvertClassNamesCorrectly(String input, String expected) {
        assertThat(ProblemProcessor.toKebabCase(input)).isEqualTo(expected);
    }

    @Test
    void isMapperEnabledShouldReturnTrueWhenNoConfigPresent() {
        assertThat(ProblemProcessor.isMapperEnabled("jakarta.ws.rs.NotFoundException", Map.of()))
                .isTrue();
    }

    @Test
    void isMapperEnabledShouldReturnFalseWhenDisabled() {
        ProblemBuildConfig.MapperConfig disabled = () -> false;
        Map<String, ProblemBuildConfig.MapperConfig> config = Map.of("not-found-exception", disabled);

        assertThat(ProblemProcessor.isMapperEnabled("jakarta.ws.rs.NotFoundException", config))
                .isFalse();
    }

    @Test
    void isMapperEnabledShouldReturnTrueWhenExplicitlyEnabled() {
        ProblemBuildConfig.MapperConfig enabled = () -> true;
        Map<String, ProblemBuildConfig.MapperConfig> config = Map.of("not-found-exception", enabled);

        assertThat(ProblemProcessor.isMapperEnabled("jakarta.ws.rs.NotFoundException", config))
                .isTrue();
    }

}
