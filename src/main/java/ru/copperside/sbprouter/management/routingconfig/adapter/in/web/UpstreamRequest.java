package ru.copperside.sbprouter.management.routingconfig.adapter.in.web;

public record UpstreamRequest(
        String name,
        String url,
        Integer timeoutMs,
        Integer retryMaxAttempts,
        Integer retryBackoffMs
) {
}
