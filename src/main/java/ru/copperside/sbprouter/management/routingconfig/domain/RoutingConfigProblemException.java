package ru.copperside.sbprouter.management.routingconfig.domain;

public class RoutingConfigProblemException extends RuntimeException {

    private final String code;

    public RoutingConfigProblemException(String code, String message) {
        super(code + ": " + message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
