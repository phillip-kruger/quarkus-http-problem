package io.quarkiverse.httpproblem.postprocessing;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.logging.Logger;

import io.quarkiverse.httpproblem.HttpProblem;

public class ProblemLogger implements ProblemPostProcessor {

    private final Logger logger;
    private final ProblemLoggingConfig config;

    public ProblemLogger(ProblemLoggingConfig config) {
        this(Logger.getLogger("http-problem"), config);
    }

    ProblemLogger(Logger logger, ProblemLoggingConfig config) {
        this.logger = logger;
        this.config = config;
    }

    @Override
    public HttpProblem apply(HttpProblem problem, ProblemContext context) {
        int statusCode = problem.getStatusCode();
        ProblemLogLevel level = config.resolveLevel(statusCode);
        String message = serialize(problem);
        if (config.includeStackTrace(statusCode)) {
            level.log(logger, message, context.cause);
        } else {
            level.log(logger, message);
        }
        return problem;
    }

    private String serialize(HttpProblem problem) {
        Stream<String> basicFields = Stream.of(
                "status=" + problem.getStatusCode(),
                problem.getTitle() == null ? null : "title=\"" + problem.getTitle() + "\"",
                problem.getDetail() == null ? null : "detail=\"" + problem.getDetail() + "\"",
                problem.getInstance() == null ? null : "instance=\"" + problem.getInstance() + "\"",
                problem.getType() == null ? null : "type=" + problem.getType());

        Stream<String> parameters = problem.getParameters().entrySet().stream().map(this::serializeParameter);

        return Stream.concat(basicFields, parameters)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
    }

    private String serializeParameter(Map.Entry<String, Object> param) {
        Object value = param.getValue();
        String serializedValue;
        if (value == null) {
            serializedValue = "null";
        } else if (value instanceof String) {
            serializedValue = "\"" + value + "\"";
        } else {
            serializedValue = value.toString();
        }
        return param.getKey() + "=" + serializedValue;
    }

    @Override
    public int priority() {
        return LOG_PRIORITY;
    }
}
