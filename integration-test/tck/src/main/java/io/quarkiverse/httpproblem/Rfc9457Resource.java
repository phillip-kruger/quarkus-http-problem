package io.quarkiverse.httpproblem;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/throw/rfc9457/")
@Produces(MediaType.APPLICATION_JSON)
public class Rfc9457Resource {

    @GET
    @Path("/complete")
    public void throwComplete() {
        throw HttpProblem.builder()
                .withType(URI.create("https://example.com/probs/out-of-credit"))
                .withTitle("You do not have enough credit.")
                .withStatus(Response.Status.FORBIDDEN)
                .withDetail("Your current balance is 30, but that costs 50.")
                .withInstance(URI.create("https://example.net/account/12345/msgs/abc"))
                .with("balance", 30)
                .with("accounts", List.of("/account/12345", "/account/67890"))
                .build();
    }

    @GET
    @Path("/minimal")
    public void throwMinimal() {
        throw HttpProblem.builder()
                .withStatus(Response.Status.INTERNAL_SERVER_ERROR)
                .build();
    }

    @GET
    @Path("/with-type")
    public void throwWithType() {
        throw HttpProblem.builder()
                .withType(URI.create("https://example.com/probs/invalid-parameter"))
                .withTitle("Invalid Parameter")
                .withStatus(Response.Status.BAD_REQUEST)
                .withDetail("The 'age' parameter must be a positive integer.")
                .build();
    }

    @GET
    @Path("/without-type")
    public void throwWithoutType() {
        throw HttpProblem.builder()
                .withTitle("Not Found")
                .withStatus(Response.Status.NOT_FOUND)
                .withDetail("The requested resource was not found.")
                .build();
    }

    @GET
    @Path("/with-status")
    public void throwWithStatus(@QueryParam("status") int status) {
        Response.Status responseStatus = Response.Status.fromStatusCode(status);
        String title = responseStatus != null ? responseStatus.getReasonPhrase() : "Unknown";
        throw HttpProblem.builder()
                .withStatus(status)
                .withTitle(title)
                .build();
    }

    @GET
    @Path("/with-detail")
    public void throwWithDetail() {
        throw HttpProblem.builder()
                .withStatus(Response.Status.BAD_REQUEST)
                .withTitle("Validation Error")
                .withDetail("Field 'email' is not a valid email address.")
                .build();
    }

    @GET
    @Path("/with-instance")
    public void throwWithInstance() {
        throw HttpProblem.builder()
                .withStatus(Response.Status.NOT_FOUND)
                .withTitle("Not Found")
                .withInstance(URI.create("https://example.com/errors/404/abc123"))
                .build();
    }

    @GET
    @Path("/without-instance")
    public void throwWithoutInstance() {
        throw HttpProblem.builder()
                .withStatus(Response.Status.NOT_FOUND)
                .withTitle("Not Found")
                .build();
    }

    @GET
    @Path("/with-type-urn")
    public void throwWithTypeUrn() {
        throw HttpProblem.builder()
                .withType(URI.create("urn:example:problem:validation-error"))
                .withTitle("Validation Error")
                .withStatus(Response.Status.BAD_REQUEST)
                .withDetail("Non-HTTP URI scheme for type member.")
                .build();
    }

    @GET
    @Path("/with-extensions")
    public void throwWithExtensions() {
        throw HttpProblem.builder()
                .withType(URI.create("https://example.com/probs/validation-error"))
                .withTitle("Validation Error")
                .withStatus(Response.Status.BAD_REQUEST)
                .with("invalid_params", List.of("name", "age"))
                .with("retry_after", 30)
                .with("documentation_url", "https://api.example.com/docs/errors/validation")
                .build();
    }

    @GET
    @Path("/with-single-extension")
    public void throwWithSingleExtension() {
        throw HttpProblem.builder()
                .withStatus(Response.Status.CONFLICT)
                .withTitle("Conflict")
                .withDetail("Resource version conflict.")
                .with("current_version", 42)
                .build();
    }

    @GET
    @Path("/with-nested-extension")
    public void throwWithNestedExtension() {
        Map<String, Object> address = new LinkedHashMap<>();
        address.put("street", "123 Main St");
        address.put("city", "Springfield");
        throw HttpProblem.builder()
                .withStatus(Response.Status.BAD_REQUEST)
                .withTitle("Validation Error")
                .with("nested_object", address)
                .with("is_retryable", true)
                .build();
    }

    @GET
    @Path("/about-blank")
    public void throwAboutBlank(@QueryParam("status") int status) {
        Response.Status responseStatus = Response.Status.fromStatusCode(status);
        if (responseStatus == null) {
            throw HttpProblem.builder()
                    .withStatus(status)
                    .build();
        }
        throw HttpProblem.valueOf(responseStatus);
    }

    @GET
    @Path("/about-blank-explicit")
    public void throwAboutBlankExplicit() {
        throw HttpProblem.builder()
                .withType(URI.create("about:blank"))
                .withTitle("Not Found")
                .withStatus(Response.Status.NOT_FOUND)
                .build();
    }

    @GET
    @Path("/about-blank-with-detail")
    public void throwAboutBlankWithDetail() {
        throw HttpProblem.builder()
                .withType(URI.create("about:blank"))
                .withTitle("Bad Request")
                .withStatus(Response.Status.BAD_REQUEST)
                .withDetail("The 'name' query parameter is required.")
                .build();
    }

    @GET
    @Path("/with-headers")
    public void throwWithHeaders() {
        throw HttpProblem.builder()
                .withStatus(Response.Status.TOO_MANY_REQUESTS)
                .withTitle("Too Many Requests")
                .withDetail("Rate limit exceeded.")
                .withHeader("Retry-After", "60")
                .withHeader("X-RateLimit-Remaining", "0")
                .build();
    }
}
