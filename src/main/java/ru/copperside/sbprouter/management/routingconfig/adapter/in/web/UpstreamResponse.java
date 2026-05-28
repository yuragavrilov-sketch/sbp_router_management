package ru.copperside.sbprouter.management.routingconfig.adapter.in.web;

import ru.copperside.sbprouter.management.routingconfig.domain.Upstream;

import java.time.Instant;
import java.util.UUID;

public record UpstreamResponse(
        UUID id,
        String name,
        String url,
        Integer timeoutMs,
        Integer retryMaxAttempts,
        Integer retryBackoffMs,
        String status,
        boolean removal,
        int version,
        Instant updatedAt
) {
    public static UpstreamResponse from(Upstream u) {
        return new UpstreamResponse(
                u.id(), u.name(), u.url(), u.timeoutMs(), u.retryMaxAttempts(), u.retryBackoffMs(),
                u.status().name(), u.removal(), u.version(), u.updatedAt());
    }
}
