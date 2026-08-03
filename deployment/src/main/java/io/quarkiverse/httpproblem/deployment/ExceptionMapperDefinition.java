package io.quarkiverse.httpproblem.deployment;

import java.util.Objects;

import io.quarkus.deployment.Capabilities;

final class ExceptionMapperDefinition {

    static ExceptionClassSupplier mapper(String mapper) {
        Objects.requireNonNull(mapper);
        return new ExceptionClassSupplier(mapper);
    }

    static class ExceptionClassSupplier {
        private final String mapper;

        private ExceptionClassSupplier(String mapper) {
            this.mapper = mapper;
        }

        ExceptionMapperDefinition thatHandles(String exception) {
            Objects.requireNonNull(exception);
            return new ExceptionMapperDefinition(exception, this.mapper, null);
        }
    }

    final String exceptionClassName;
    final String mapperClassName;
    private final String requiredCapability;

    private ExceptionMapperDefinition(String exceptionClassName, String mapperClassName, String requiredCapability) {
        this.exceptionClassName = exceptionClassName;
        this.mapperClassName = mapperClassName;
        this.requiredCapability = requiredCapability;
    }

    ExceptionMapperDefinition onlyIf(String capability) {
        return new ExceptionMapperDefinition(this.exceptionClassName, this.mapperClassName, capability);
    }

    boolean isNeeded(Capabilities capabilities) {
        if (requiredCapability != null && !capabilities.isPresent(requiredCapability)) {
            return false;
        }
        return new ClasspathDetector(exceptionClassName).getAsBoolean();
    }
}
