package ru.copperside.sbprouter.management.routingconfig.domain;

import java.time.Instant;
import java.util.UUID;

public record Upstream(
        UUID id,
        String name,
        String url,
        Integer timeoutMs,
        Integer retryMaxAttempts,
        Integer retryBackoffMs,
        ConfigStatus status,
        boolean removal,
        int version,
        Instant createdAt,
        Instant updatedAt
) {
    public Upstream withPatch(String url, Integer timeoutMs, Integer retryMaxAttempts, Integer retryBackoffMs, Instant now) {
        return new Upstream(
                id, name,
                url != null ? url : this.url,
                timeoutMs != null ? timeoutMs : this.timeoutMs,
                retryMaxAttempts != null ? retryMaxAttempts : this.retryMaxAttempts,
                retryBackoffMs != null ? retryBackoffMs : this.retryBackoffMs,
                status, removal, version, createdAt, now);
    }
}
