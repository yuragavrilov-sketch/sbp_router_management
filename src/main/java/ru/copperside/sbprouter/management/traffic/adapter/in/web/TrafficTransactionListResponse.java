package ru.copperside.sbprouter.management.traffic.adapter.in.web;

import ru.copperside.sbprouter.management.traffic.application.TrafficListResult;

import java.util.List;

public record TrafficTransactionListResponse(
        List<TrafficTransactionSummaryResponse> items,
        long total,
        int page,
        int size
) {
    public static TrafficTransactionListResponse from(TrafficListResult r) {
        return new TrafficTransactionListResponse(
                r.items().stream().map(TrafficTransactionSummaryResponse::from).toList(),
                r.total(), r.page(), r.size());
    }
}
