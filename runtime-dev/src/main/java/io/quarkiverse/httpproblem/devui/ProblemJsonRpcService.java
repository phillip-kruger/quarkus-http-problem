package io.quarkiverse.httpproblem.devui;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import io.quarkiverse.httpproblem.HttpProblem;
import io.quarkiverse.httpproblem.postprocessing.PostProcessorsRegistry;
import io.quarkiverse.httpproblem.postprocessing.ProblemContext;
import io.quarkiverse.httpproblem.postprocessing.ProblemPostProcessor;
import io.quarkus.runtime.annotations.JsonRpcDescription;

public class ProblemJsonRpcService {

    @Inject
    PostProcessorsRegistry postProcessorsRegistry;

    @JsonRpcDescription("List active post-processors in the HTTP Problem pipeline with their execution order and priority")
    public List<Map<String, Object>> getPostProcessors() {
        return postProcessorsRegistry.getProcessors().stream()
                .map(this::processorToMap)
                .collect(Collectors.toList());
    }

    @JsonRpcDescription("Generate a test HTTP Problem response for a given status code, running it through the full post-processor pipeline")
    public Map<String, Object> testProblem(
            @JsonRpcDescription("HTTP status code (e.g. 404, 500)") int statusCode,
            @JsonRpcDescription("Optional detail message for the problem") String detail) {

        HttpProblem.Builder builder = HttpProblem.builder().withStatus(statusCode);

        Response.Status status = Response.Status.fromStatusCode(statusCode);
        if (status != null) {
            builder.withTitle(status.getReasonPhrase());
        }

        if (detail != null && !detail.isBlank()) {
            builder.withDetail(detail);
        }

        HttpProblem problem = builder.build();
        ProblemContext context = ProblemContext.of(new RuntimeException("Dev UI test"), "/dev-ui/test");
        HttpProblem processed = postProcessorsRegistry.applyPostProcessing(problem, context);

        return problemToMap(processed);
    }

    private Map<String, Object> processorToMap(ProblemPostProcessor processor) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", processor.getClass().getSimpleName());
        map.put("className", processor.getClass().getName());
        map.put("priority", processor.priority());
        return map;
    }

    private Map<String, Object> problemToMap(HttpProblem problem) {
        Map<String, Object> map = new LinkedHashMap<>();
        URI type = problem.getType();
        if (type != null) {
            map.put("type", type.toString());
        }
        map.put("status", problem.getStatusCode());
        String title = problem.getTitle();
        if (title != null) {
            map.put("title", title);
        }
        String detail = problem.getDetail();
        if (detail != null) {
            map.put("detail", detail);
        }
        URI instance = problem.getInstance();
        if (instance != null) {
            map.put("instance", instance.toString());
        }
        if (!problem.getParameters().isEmpty()) {
            map.putAll(problem.getParameters());
        }
        return map;
    }

}
