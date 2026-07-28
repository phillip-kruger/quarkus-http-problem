package io.quarkiverse.httpproblem;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(DisabledMapperIT.WebApplicationExceptionMapperDisabled.class)
class DisabledMapperIT {

    static {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    void disabledMapperShouldFallThroughToDefaultExceptionMapper() {
        // WebApplicationException(400) would normally be handled by WebApplicationExceptionMapper
        // and return 400. With it disabled, DefaultExceptionMapper catches it as Exception -> 500.
        given()
                .queryParam("status", 400)
                .get("/throw/jax-rs/web-application-exception")
                .then()
                .statusCode(500)
                .body("title", equalTo("Internal Server Error"))
                .body("status", equalTo(500));
    }

    @Test
    void notFoundExceptionMapperShouldStillWork() {
        given()
                .queryParam("message", "not here")
                .get("/throw/jax-rs/not-found-exception")
                .then()
                .statusCode(404)
                .body("title", equalTo("Not Found"))
                .body("detail", equalTo("not here"));
    }

    public static class WebApplicationExceptionMapperDisabled implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.http-problem.mapper.web-application-exception.enabled", "false");
        }
    }
}
