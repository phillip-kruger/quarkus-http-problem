package io.quarkiverse.httpproblem.postprocessing;

import java.util.List;
import java.util.Map;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ProblemRecorder {

    public RuntimeValue<ProblemLogger> createProblemLogger(Map<String, ProblemLogLevel> levels,
            List<String> stackTracePatterns) {
        ProblemLoggingConfig config = new ProblemLoggingConfig(levels, stackTracePatterns);
        return new RuntimeValue<>(new ProblemLogger(config));
    }

}
