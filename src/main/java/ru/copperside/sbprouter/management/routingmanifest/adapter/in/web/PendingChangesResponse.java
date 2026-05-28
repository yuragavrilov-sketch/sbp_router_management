package ru.copperside.sbprouter.management.routingmanifest.adapter.in.web;

import ru.copperside.sbprouter.management.routingmanifest.domain.PendingChanges;

import java.util.List;

public record PendingChangesResponse(
        int count,
        Integer currentVersion,
        int nextVersion,
        List<PendingChanges.Entry> entries
) {
    public static PendingChangesResponse from(PendingChanges p) {
        return new PendingChangesResponse(p.count(), p.currentVersion(), p.nextVersion(), p.entries());
    }
}
