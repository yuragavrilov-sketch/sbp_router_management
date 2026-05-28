package ru.copperside.sbprouter.management.routingconfig.application;

public record CreateUpstreamCommand(
        String name,
        String url,
        Integer timeoutMs,
        Integer retryMaxAttempts,
        Integer retryBackoffMs
) {
}
