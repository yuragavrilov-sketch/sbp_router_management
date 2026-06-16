package ru.copperside.sbprouter.management.fleet.adapter.in.web;

import ru.copperside.sbprouter.management.fleet.domain.RouterInstance;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public record FleetResponse(int total, int up, List<RouterInstanceResponse> routers) {

    public static FleetResponse from(List<RouterInstance> instances, Instant now, Duration ttl) {
        List<RouterInstanceResponse> routers = instances.stream()
                .sorted(Comparator.comparing(RouterInstance::instanceId))
                .map(i -> RouterInstanceResponse.from(i, now, ttl))
                .toList();
        int up = (int) routers.stream().filter(r -> "UP".equals(r.status())).count();
        return new FleetResponse(routers.size(), up, routers);
    }
}
