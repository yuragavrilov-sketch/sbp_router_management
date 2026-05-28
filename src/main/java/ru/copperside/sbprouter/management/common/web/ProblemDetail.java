package ru.copperside.sbprouter.management.common.web;

public record ProblemDetail(
        String type,
        String title,
        int status,
        String code,
        String message,
        Object details,
        String traceId
) {
}
