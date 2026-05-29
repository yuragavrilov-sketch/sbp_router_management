package ru.copperside.sbprouter.management.traffic.domain;

public class TrafficTransactionProblemException extends RuntimeException {

    private final String code;

    public TrafficTransactionProblemException(String code, String message) {
        super(code + ": " + message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
