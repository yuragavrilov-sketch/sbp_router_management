package ru.copperside.sbprouter.management.fleet.adapter.in.web;

import ru.copperside.sbprouter.management.fleet.domain.RouterInstance;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public record RouterInstanceResponse(
        String instanceId,
        String status,
        Instant startedAt,
        Instant lastHeartbeat,
        String activeGroup,
        List<String> groups,
        List<RouterInstance.RouterBackend> backends,
        RouterInstance.RouterMetrics metrics
) {
    public static RouterInstanceResponse from(RouterInstance i, Instant now, Duration ttl) {
        boolean up = i.lastHeartbeat() != null && i.lastHeartbeat().isAfter(now.minus(ttl));
        return new RouterInstanceResponse(
                i.instanceId(), up ? "UP" : "STALE", i.startedAt(), i.lastHeartbeat(),
                i.activeGroup(), i.groups(), i.backends(), i.metrics());
    }
}
