package ru.copperside.sbprouter.management.traffic.adapter.in.web;

import ru.copperside.sbprouter.management.traffic.domain.TrafficStats;

public record TrafficStatsResponse(TrafficStats stats) {
    public static TrafficStatsResponse from(TrafficStats s) {
        return new TrafficStatsResponse(s);
    }
}
