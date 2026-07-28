package io.quarkiverse.httpproblem;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@QuarkusTest
class Rfc9457StandardMembersTest {

    static {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    @DisplayName("RFC 9457 Section 3.1.1 - type should be an absolute URI when explicitly set")
    void typeShouldBeAbsoluteUri() {
        when()
                .get("/throw/rfc9457/with-type")
                .then()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("type", equalTo("https://example.com/probs/invalid-parameter"));
    }

    @Test
    @DisplayName("RFC 9457 Section 3.1.1 - type should be absent from JSON when not set")
    void typeShouldBeAbsentWhenNotSet() {
        String body = when()
                .get("/throw/rfc9457/without-type")
                .then()
                .statusCode(NOT_FOUND.getStatusCode())
                .body("type", nullValue())
                .extract().body().asString();

        assertThat(body).doesNotContain("\"type\"");
    }

    @Test
    @DisplayName("RFC 9457 Section 3.1.1 - type should accept non-HTTP URI schemes such as URNs")
    void typeShouldAcceptUrnScheme() {
        when()
                .get("/throw/rfc9457/with-type-urn")
                .then()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("type", equalTo("urn:example:problem:validation-error"));
    }

    @ParameterizedTest(name = "RFC 9457 Section 3.1.2 - status {0} in body should match HTTP status")
    @ValueSource(ints = { 400, 403, 404, 500, 502 })
    void statusInBodyShouldMatchHttpStatus(int status) {
        given()
                .queryParam("status", status)
                .get("/throw/rfc9457/with-status")
                .then()
                .statusCode(status)
                .body("status", equalTo(status));
    }

    @Test
    @DisplayName("RFC 9457 Section 3.1.2 - status should always be present even in minimal problem")
    void statusShouldAlwaysBePresent() {
        when()
                .get("/throw/rfc9457/minimal")
                .then()
                .statusCode(INTERNAL_SERVER_ERROR.getStatusCode())
                .body("status", equalTo(INTERNAL_SERVER_ERROR.getStatusCode()));
    }

    @Test
    @DisplayName("RFC 9457 Section 3.1.2 - status should be a JSON number")
    void statusShouldBeNumeric() {
        Object status = given()
                .queryParam("status", BAD_REQUEST.getStatusCode())
                .get("/throw/rfc9457/with-status")
                .then()
                .extract().body().jsonPath().get("status");

        assertThat(status).isInstanceOf(Integer.class);
    }

    @Test
    @DisplayName("RFC 9457 Section 3.1.3 - title should be present when set")
    void titleShouldBePresent() {
        when()
                .get("/throw/rfc9457/with-type")
                .then()
                .body("title", equalTo("Invalid Parameter"));
    }

    @Test
    @DisplayName("RFC 9457 Section 3.1.3 - title should be absent when not set")
    void titleShouldBeAbsentWhenNotSet() {
        when()
                .get("/throw/rfc9457/minimal")
                .then()
                .body("title", nullValue());
    }

    @Test
    @DisplayName("RFC 9457 Section 3.1.4 - detail should be present when set")
    void detailShouldBePresent() {
        when()
                .get("/throw/rfc9457/with-detail")
                .then()
                .body("detail", equalTo("Field 'email' is not a valid email address."));
    }

    @Test
    @DisplayName("RFC 9457 Section 3.1.4 - detail should be absent when not set")
    void detailShouldBeAbsentWhenNotSet() {
        when()
                .get("/throw/rfc9457/minimal")
                .then()
                .body("detail", nullValue());
    }

    @Test
    @DisplayName("RFC 9457 Section 3.1.5 - instance should be the explicit URI when set")
    void instanceShouldBeExplicitUriWhenSet() {
        when()
                .get("/throw/rfc9457/with-instance")
                .then()
                .body("instance", equalTo("https://example.com/errors/404/abc123"));
    }

    @Test
    @DisplayName("RFC 9457 Section 3.1.2 - non-standard status code should still be returned correctly")
    void nonStandardStatusCodeShouldBeReturned() {
        given()
                .queryParam("status", 418)
                .get("/throw/rfc9457/with-status")
                .then()
                .statusCode(418)
                .body("status", equalTo(418))
                .body("title", equalTo("Unknown"));
    }

    @Test
    @DisplayName("RFC 9457 Section 3.1.5 - instance defaults to request path via ProblemDefaultsProvider")
    void instanceShouldDefaultToRequestPath() {
        when()
                .get("/throw/rfc9457/without-instance")
                .then()
                .body("instance", equalTo("/throw/rfc9457/without-instance"));
    }
}
