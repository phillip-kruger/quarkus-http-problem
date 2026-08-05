package io.quarkiverse.httpproblem.deployment.devui;

public final class ExceptionMapperInfo {

    public final String exception;
    public final String mapper;
    public final String status;

    public ExceptionMapperInfo(String exception, String mapper, String status) {
        this.exception = exception;
        this.mapper = mapper;
        this.status = status;
    }

    public String getException() {
        return exception;
    }

    public String getMapper() {
        return mapper;
    }

    public String getStatus() {
        return status;
    }

}
