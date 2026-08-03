package io.quarkiverse.httpproblem.client;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import org.jboss.logging.Logger;

import io.quarkiverse.httpproblem.HttpProblem;

/**
 * Parses application/problem+json response types and rethrows them as `HttpProblem`.
 * <p>
 * Intended to be used as @Provider for RestClients via @RegisterRestClient.
 */
public class ThrowingHttpProblemClientExceptionMapper implements ResponseExceptionMapper<RuntimeException> {

    private static final Logger log = Logger.getLogger(ThrowingHttpProblemClientExceptionMapper.class);

    @Override
    public RuntimeException toThrowable(Response response) {
        if (!isProblemMediaType(response.getMediaType())) {
            return null; // Let others handle non-problem formats
        }

        try {
            HttpProblem returnedProblem = response.readEntity(HttpProblem.class);
            // instance must be nullified, otherwise it will be propagated as-is
            return HttpProblem.builder(returnedProblem)
                    .withInstance(null)
                    .build();
        } catch (RuntimeException e) {
            log.warn("Failed to deserialize application/problem+json response body", e);
            return null; // Let others handle unreadable responses
        }
    }

    /**
     * Checks type and subtype only, ignoring parameters such as {@code charset=UTF-8}.
     * {@link MediaType#isCompatible(MediaType)} also checks parameters, which causes it to reject
     * {@code application/problem+json; charset=UTF-8} even though it is the same media type.
     */
    private static boolean isProblemMediaType(MediaType mediaType) {
        if (mediaType == null) {
            return false;
        }
        return HttpProblem.MEDIA_TYPE.getType().equalsIgnoreCase(mediaType.getType())
                && HttpProblem.MEDIA_TYPE.getSubtype().equalsIgnoreCase(mediaType.getSubtype());
    }

}
