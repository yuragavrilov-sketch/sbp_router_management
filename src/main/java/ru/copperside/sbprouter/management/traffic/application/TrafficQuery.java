package ru.copperside.sbprouter.management.traffic.application;

import java.time.Instant;

public record TrafficQuery(
        String requestType,
        String terminalOwner,
        String upstream,
        String outcome,
        String status,
        Instant from,
        Instant to,
        String q,
        int page,
        int size
) {
    public int safeSize() {
        return size <= 0 || size > 200 ? 50 : size;
    }

    public int safePage() {
        return Math.max(page, 0);
    }
}
