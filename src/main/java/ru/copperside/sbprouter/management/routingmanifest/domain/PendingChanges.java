package ru.copperside.sbprouter.management.routingmanifest.domain;

import java.util.List;

public record PendingChanges(
        int count,
        Integer currentVersion,
        int nextVersion,
        List<Entry> entries
) {
    public record Entry(String entityKind, String change, String label) {
    }
}
