package ru.copperside.sbprouter.management.traffic.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record TrafficStats(
        long total,
        long responded,
        long pending,
        Map<String, Long> byOutcome,
        Map<String, Long> byRequestType,
        Map<String, Long> byUpstream,
        Long latencyP95,
        Long latencyP99,
        Long latencyAvg,
        List<Bucket> throughputPerMinute
) {
    public record Bucket(Instant minute, long count) {
    }
}
