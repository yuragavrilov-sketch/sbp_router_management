package ru.copperside.sbprouter.management.routingconfig.application;

public record PatchUpstreamCommand(
        String url,
        Integer timeoutMs,
        Integer retryMaxAttempts,
        Integer retryBackoffMs
) {
}
