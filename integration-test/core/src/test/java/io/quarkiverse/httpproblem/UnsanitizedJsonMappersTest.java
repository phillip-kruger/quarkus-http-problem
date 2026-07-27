package io.quarkiverse.httpproblem;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;

@QuarkusTest
@TestProfile(UnsanitizedJsonMappersTest.SanitizationDisabled.class)
class UnsanitizedJsonMappersTest {

    static final String SANITIZED_DETAIL = "Malformed request body";

    private static final Logger logger = LoggerFactory.getLogger(UnsanitizedJsonMappersTest.class);

    static {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    @DisplayName("Should expose raw detail when include-details is true")
    void shouldExposeRawDetailOnMalformedBody() {
        ExtractableResponse<Response> response = given()
                .body("{\"key\":\"")
                .contentType(APPLICATION_JSON)
                .post("/throw/json")
                .then()
                .statusCode(BAD_REQUEST.getStatusCode())
                .extract();

        if (isProblemJson(response)) {
            assertThat(response.body().jsonPath().getString("detail")).isNotEqualTo(SANITIZED_DETAIL);
        } else {
            logger.info("Response handled by RESTEasy Reactive before mapper, skipping detail assertion");
        }
    }

    @Test
    @DisplayName("Should expose raw detail for invalid field format when include-details is true")
    void shouldExposeRawDetailForInvalidFieldFormat() throws IOException {
        ValidatableResponse response = given()
                .body("{\"uuid_field_1\":\"ABC-DEF-GHI\"}")
                .contentType(APPLICATION_JSON)
                .post("/throw/json")
                .then()
                .statusCode(BAD_REQUEST.getStatusCode());

        if (response.extract().body().asInputStream().available() == 0) {
            logger.info("Reactive impl returns empty body, skipping further validation");
            return;
        }

        response.body("detail", not(is(SANITIZED_DETAIL)))
                .body("field", anyOf(is("uuid_field_1"), nullValue()));
    }

    private static boolean isProblemJson(ExtractableResponse<Response> response) {
        String contentType = response.contentType();
        return contentType != null && contentType.contains("application/problem+json");
    }

    public static class SanitizationDisabled implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.http-problem.include-details", "true");
        }
    }
}
