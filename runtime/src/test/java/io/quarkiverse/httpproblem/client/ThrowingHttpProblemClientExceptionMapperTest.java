package io.quarkiverse.httpproblem.client;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.httpproblem.HttpProblem;

class ThrowingHttpProblemClientExceptionMapperTest {

    ThrowingHttpProblemClientExceptionMapper mapper = new ThrowingHttpProblemClientExceptionMapper();

    @ParameterizedTest
    @ValueSource(strings = {
            "application/problem+json",
            "application/problem+json;charset=UTF-8"
    })
    void shouldMapProblemCompatibleMimeTypesToHttpProblem(String contentType) {

        // given
        Response nonProblemResponse = Response.status(400)
                .type(contentType)
                .entity(
                        // serializers are not available/registered here in unit tests
                        HttpProblem.builder()
                                .withStatus(400)
                                .withTitle("Bad Request")
                                .build())
                .build();

        // when
        Throwable mappedThrowable = mapper.toThrowable(nonProblemResponse);

        // then
        assertThat(mappedThrowable)
                .isInstanceOf(HttpProblem.class)
                .usingRecursiveComparison()
                .isEqualTo(
                        HttpProblem.builder()
                                .withStatus(400)
                                .withTitle("Bad Request")
                                .build());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "application/json",
            "application/json;charset=UTF-8",
            "application/xml",
            "text/plain;charset=UTF-8"
    })
    void shouldSkipNonProblemResponses(String contentType) {
        // given
        Response nonProblemResponse = Response.status(400)
                .type(contentType)
                .build();

        // when
        Throwable mappedThrowable = mapper.toThrowable(nonProblemResponse);

        // then
        assertThat(mappedThrowable).isNull();
    }

    @Test
    void shouldReturnNullWhenDeserializationFails() {
        // given - a response that claims to be problem+json but has an unreadable entity
        Response badProblemResponse = Response.status(500)
                .type(HttpProblem.MEDIA_TYPE)
                .entity("not valid json")
                .build();

        // when
        Throwable mappedThrowable = mapper.toThrowable(badProblemResponse);

        // then - returns null so other mappers can handle it
        assertThat(mappedThrowable).isNull();
    }

    @Test
    void shouldReturnNullForNullMediaType() {
        // given
        Response noMediaType = Response.status(400).build();

        // when
        Throwable mappedThrowable = mapper.toThrowable(noMediaType);

        // then
        assertThat(mappedThrowable).isNull();
    }

}
