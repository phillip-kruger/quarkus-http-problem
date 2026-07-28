package io.quarkiverse.httpproblem;

import static io.restassured.RestAssured.when;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.CONFLICT;
import static jakarta.ws.rs.core.Response.Status.TOO_MANY_REQUESTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.nullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
class Rfc9457ExtensionMembersTest {

    static {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    @DisplayName("RFC 9457 Section 3.2 - extension members should appear in response body")
    void extensionMembersShouldAppearInBody() {
        when()
                .get("/throw/rfc9457/with-extensions")
                .then()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("invalid_params", hasItems("name", "age"))
                .body("retry_after", equalTo(30))
                .body("documentation_url", equalTo("https://api.example.com/docs/errors/validation"));
    }

    @Test
    @DisplayName("Implementation quality - extension members should appear after standard members in JSON")
    void extensionMembersShouldAppearAfterStandardMembers() {
        String body = when()
                .get("/throw/rfc9457/with-extensions")
                .then()
                .extract().body().asString();

        assertThat(body.indexOf("\"status\"")).isLessThan(body.indexOf("\"invalid_params\""));
        assertThat(body.indexOf("\"title\"")).isLessThan(body.indexOf("\"retry_after\""));
    }

    @Test
    @DisplayName("RFC 9457 Section 3.2 - single extension member should appear in body")
    void singleExtensionMemberShouldAppearInBody() {
        when()
                .get("/throw/rfc9457/with-single-extension")
                .then()
                .statusCode(CONFLICT.getStatusCode())
                .body("current_version", equalTo(42));
    }

    @Test
    @DisplayName("RFC 9457 Section 3.2 - extension members with nested objects and booleans")
    void nestedObjectAndBooleanExtensionMembers() {
        when()
                .get("/throw/rfc9457/with-nested-extension")
                .then()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("nested_object.street", equalTo("123 Main St"))
                .body("nested_object.city", equalTo("Springfield"))
                .body("is_retryable", equalTo(true));
    }

    @Test
    @DisplayName("RFC 9457 Section 3.2 - extension members should coexist with all standard members")
    void extensionMembersCoexistWithStandardMembers() {
        when()
                .get("/throw/rfc9457/with-extensions")
                .then()
                .body("type", equalTo("https://example.com/probs/validation-error"))
                .body("status", equalTo(BAD_REQUEST.getStatusCode()))
                .body("title", equalTo("Validation Error"))
                .body("invalid_params", hasItems("name", "age"))
                .body("retry_after", equalTo(30));
    }

    @Test
    @DisplayName("Headers should be HTTP response headers, not serialized in JSON body")
    void headersShouldNotAppearInResponseBody() {
        when()
                .get("/throw/rfc9457/with-headers")
                .then()
                .statusCode(TOO_MANY_REQUESTS.getStatusCode())
                .header("Retry-After", equalTo("60"))
                .header("X-RateLimit-Remaining", equalTo("0"))
                .body("Retry-After", nullValue())
                .body("X-RateLimit-Remaining", nullValue());
    }
}
