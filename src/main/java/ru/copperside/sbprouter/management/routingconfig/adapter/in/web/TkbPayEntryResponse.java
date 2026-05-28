package ru.copperside.sbprouter.management.routingconfig.adapter.in.web;

import ru.copperside.sbprouter.management.routingconfig.domain.TkbPayListEntry;

import java.util.UUID;

public record TkbPayEntryResponse(UUID id, String rcvTspId, String status, boolean removal) {
    public static TkbPayEntryResponse from(TkbPayListEntry e) {
        return new TkbPayEntryResponse(e.id(), e.rcvTspId(), e.status().name(), e.removal());
    }
}
