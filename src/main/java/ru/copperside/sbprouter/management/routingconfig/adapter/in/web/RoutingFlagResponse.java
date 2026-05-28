package ru.copperside.sbprouter.management.routingconfig.adapter.in.web;

import ru.copperside.sbprouter.management.routingconfig.domain.RoutingFlag;

import java.util.UUID;

public record RoutingFlagResponse(UUID id, String key, String value, String status) {
    public static RoutingFlagResponse from(RoutingFlag f) {
        return new RoutingFlagResponse(f.id(), f.key(), f.value(), f.status().name());
    }
}
