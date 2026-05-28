package ru.copperside.sbprouter.management.routingconfig.adapter.in.web;

import ru.copperside.sbprouter.management.routingconfig.domain.TerminalRoutingConfig;

import java.util.UUID;

public record TerminalConfigResponse(
        UUID id,
        String c2bFieldName,
        String b2cFieldName,
        String tkbPayPrefix,
        String status,
        int version
) {
    public static TerminalConfigResponse from(TerminalRoutingConfig c) {
        return new TerminalConfigResponse(c.id(), c.c2bFieldName(), c.b2cFieldName(),
                c.tkbPayPrefix(), c.status().name(), c.version());
    }
}
