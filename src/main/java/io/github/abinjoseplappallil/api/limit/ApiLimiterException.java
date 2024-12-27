package io.github.abinjoseplappallil.api.limit;

public class ApiLimiterException extends RuntimeException {

    public ApiLimiterException(String message) {
        super(message);
    }
}
