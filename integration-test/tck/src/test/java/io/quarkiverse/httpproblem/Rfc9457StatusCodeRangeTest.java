package io.quarkiverse.httpproblem;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@QuarkusTest
@TestProfile(Rfc9457StatusCodeRangeTest.ExtendedRange.class)
class Rfc9457StatusCodeRangeTest {

    static {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @ParameterizedTest(name = "Extended range - non-standard status code {0} should be accepted when max=999")
    @ValueSource(ints = { 600, 783, 999 })
    @DisplayName("Non-standard status codes should be accepted with extended range configuration")
    void nonStandardStatusCodeShouldBeAcceptedWithExtendedRange(int status) {
        given()
                .queryParam("status", status)
                .get("/throw/rfc9457/with-status")
                .then()
                .statusCode(status)
                .body("status", equalTo(status));
    }

    public static class ExtendedRange implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.http-problem.status-code.max", "999");
        }
    }
}
