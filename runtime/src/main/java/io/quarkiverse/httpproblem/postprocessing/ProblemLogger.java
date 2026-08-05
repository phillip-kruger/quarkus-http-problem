package io.quarkiverse.httpproblem.postprocessing;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.logging.Logger;

import io.quarkiverse.httpproblem.HttpProblem;

public class ProblemLogger implements ProblemPostProcessor {

    private static final Pattern CONTROL_CHARS = Pattern.compile("\\e\\[[0-9;]*[a-zA-Z]|[\\x00-\\x1f\\x7f]");

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
                problem.getTitle() == null ? null : "title=\"" + sanitize(problem.getTitle()) + "\"",
                problem.getDetail() == null ? null : "detail=\"" + sanitize(problem.getDetail()) + "\"",
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
            serializedValue = "\"" + sanitize((String) value) + "\"";
        } else {
            serializedValue = sanitize(value.toString());
        }
        return sanitize(param.getKey()) + "=" + serializedValue;
    }

    static String sanitize(String value) {
        if (value == null) {
            return null;
        }
        return CONTROL_CHARS.matcher(value).replaceAll("_");
    }

    @Override
    public int priority() {
        return LOG_PRIORITY;
    }
}
