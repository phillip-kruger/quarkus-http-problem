package io.quarkiverse.httpproblem.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkus.deployment.Capabilities;
import io.quarkus.runtime.configuration.ConfigurationException;

class ProblemProcessorTest {

    static final Capabilities CAPABILITIES_WITH_JSON = new Capabilities(Collections.singleton("io.quarkus.jackson"));
    static final Capabilities CAPABILITIES_WITHOUT_JSON = new Capabilities(Collections.singleton("io.quarkus.resteasy"));

    final ProblemProcessor problemProcessor = new ProblemProcessor();

    @Test
    void featureNameShouldBeValid() {
        assertThat(problemProcessor.createFeature(CAPABILITIES_WITH_JSON).getName())
                .isEqualTo("http-problem");
    }

    @Test
    void shouldFailBuildIfMissingJsonCapability() {
        assertThatThrownBy(() -> problemProcessor.createFeature(CAPABILITIES_WITHOUT_JSON))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("quarkus-rest-jackson");
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
            "MismatchedInputException, mismatched-input-exception",
            "InvalidDefinitionException, invalid-definition-exception",
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
