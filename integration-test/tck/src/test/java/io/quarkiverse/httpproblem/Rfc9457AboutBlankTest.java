package io.quarkiverse.httpproblem;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@QuarkusTest
class Rfc9457AboutBlankTest {

    static {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @ParameterizedTest(name = "RFC 9457 Section 4.2.1 - about:blank title for status {0} should match HTTP reason phrase")
    @ValueSource(ints = { 400, 401, 403, 404, 405, 409, 500, 502, 503 })
    void aboutBlankTitleShouldMatchStatusPhrase(int status) {
        String expectedTitle = Response.Status.fromStatusCode(status).getReasonPhrase();

        given()
                .queryParam("status", status)
                .get("/throw/rfc9457/about-blank")
                .then()
                .statusCode(status)
                .body("status", equalTo(status))
                .body("title", equalTo(expectedTitle));
    }

    @Test
    @DisplayName("RFC 9457 Section 4.2.1 - explicit about:blank type should appear in JSON")
    void aboutBlankExplicitTypeShouldBeInResponse() {
        when()
                .get("/throw/rfc9457/about-blank-explicit")
                .then()
                .statusCode(NOT_FOUND.getStatusCode())
                .body("type", equalTo("about:blank"))
                .body("title", equalTo(NOT_FOUND.getReasonPhrase()));
    }

    @Test
    @DisplayName("RFC 9457 Section 4.2.1 - implicit about:blank (valueOf) should omit type from JSON")
    void implicitAboutBlankShouldOmitType() {
        given()
                .queryParam("status", NOT_FOUND.getStatusCode())
                .get("/throw/rfc9457/about-blank")
                .then()
                .statusCode(NOT_FOUND.getStatusCode())
                .body("type", nullValue())
                .body("title", equalTo(NOT_FOUND.getReasonPhrase()));
    }

    @Test
    @DisplayName("RFC 9457 Section 4.2.1 - about:blank with non-standard status code should fall back to builder")
    void aboutBlankWithNonStandardStatusShouldFallBackToBuilder() {
        given()
                .queryParam("status", 418)
                .get("/throw/rfc9457/about-blank")
                .then()
                .statusCode(418)
                .body("status", equalTo(418));
    }

    @Test
    @DisplayName("RFC 9457 Section 4.2.1 - about:blank problem can include detail")
    void aboutBlankWithDetailShouldIncludeDetail() {
        when()
                .get("/throw/rfc9457/about-blank-with-detail")
                .then()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("type", equalTo("about:blank"))
                .body("detail", equalTo("The 'name' query parameter is required."));
    }
}
