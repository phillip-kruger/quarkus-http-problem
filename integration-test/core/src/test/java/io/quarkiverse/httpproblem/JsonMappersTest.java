package io.quarkiverse.httpproblem;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;

@QuarkusTest
class JsonMappersTest {

    static final String SANITIZED_DETAIL = "Malformed request body";

    private static final Logger logger = LoggerFactory.getLogger(JsonMappersTest.class);

    static {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    @DisplayName("Should return Bad Request(400) without leaking internals when request payload is malformed")
    void shouldThrowBadRequestOnMalformedBody() {
        ExtractableResponse<Response> response = given()
                .body("{\"key\":\"")
                .contentType(APPLICATION_JSON)
                .post("/throw/json")
                .then()
                .statusCode(BAD_REQUEST.getStatusCode())
                .extract();

        assertDetailDoesNotLeakInternals(response);
    }

    @Test
    @DisplayName("Should return Bad Request(400) without leaking internals for differently malformed body")
    void shouldThrowBadRequestOnDifferentlyMalformedBody() {
        ExtractableResponse<Response> response = given()
                .body("{\"key\":")
                .contentType(APPLICATION_JSON)
                .post("/throw/json")
                .then()
                .statusCode(BAD_REQUEST.getStatusCode())
                .extract();

        assertDetailDoesNotLeakInternals(response);
    }

    @Test
    @DisplayName("Should return Bad Request(400) with sanitized detail when field cannot be deserialized")
    void shouldThrowBadRequestForInvalidFieldFormat() throws IOException {
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

        response.body("detail", is(SANITIZED_DETAIL))
                .body("field", anyOf(is("uuid_field_1"), nullValue()));
    }

    @Test
    @DisplayName("Should return Bad Request(400) with sanitized detail for nested field deserialization error")
    void shouldThrowBadRequestForInvalidFieldFormatInNestedObject() throws IOException {
        ValidatableResponse response = given()
                .body("{\"nested\": {\"uuid_field_2\":\"ABC-DEF-GHI\"}}")
                .contentType(APPLICATION_JSON)
                .post("/throw/json")
                .then()
                .statusCode(BAD_REQUEST.getStatusCode());

        if (response.extract().body().asInputStream().available() == 0) {
            logger.info("Reactive impl returns empty body, skipping further validation");
            return;
        }

        response.body("detail", is(SANITIZED_DETAIL))
                .body("field", anyOf(is("nested.uuid_field_2"), nullValue()));
    }

    @Test
    @DisplayName("Should return Bad Request(400) with sanitized detail for collection item deserialization error")
    void shouldThrowBadRequestForInvalidFieldFormatInCollectionItem() throws IOException {
        ValidatableResponse response = given()
                .body("{\"collection\": [{\"uuid_field_2\":\"ABC-DEF-GHI\"}]}")
                .contentType(APPLICATION_JSON)
                .post("/throw/json")
                .then()
                .statusCode(BAD_REQUEST.getStatusCode());

        if (response.extract().body().asInputStream().available() == 0) {
            logger.info("Reactive impl returns empty body, skipping further validation");
            return;
        }

        response.body("detail", is(SANITIZED_DETAIL))
                .body("field", anyOf(is("collection[0].uuid_field_2"), nullValue()));
    }

    private static void assertDetailDoesNotLeakInternals(ExtractableResponse<Response> response) {
        String contentType = response.contentType();
        if (contentType == null || !contentType.contains("json")) {
            return;
        }
        String detail = response.body().jsonPath().getString("detail");
        if (detail != null) {
            assertThat(detail)
                    .as("detail must not leak internal class names or stack traces")
                    .doesNotContain("com.", "org.", "java.", "jakarta.")
                    .doesNotContain("Exception", "adapting object");
        }
    }
}
