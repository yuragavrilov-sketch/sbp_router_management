package ru.copperside.sbprouter.management.traffic.adapter.in.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.copperside.sbprouter.management.common.web.ApiResponse;
import ru.copperside.sbprouter.management.traffic.application.TrafficQuery;
import ru.copperside.sbprouter.management.traffic.application.TrafficQueryService;

import java.time.Clock;
import java.time.Instant;

@RestController
@RequestMapping("/internal/v1/sbp-router-management/traffic")
public class TrafficController {

    private final TrafficQueryService service;
    private final Clock clock;

    public TrafficController(TrafficQueryService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @GetMapping("/transactions")
    public ApiResponse<TrafficTransactionListResponse> list(
            @RequestParam(required = false) String requestType,
            @RequestParam(required = false) String terminalOwner,
            @RequestParam(required = false) String upstream,
            @RequestParam(required = false) String outcome,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        TrafficQuery query = new TrafficQuery(requestType, terminalOwner, upstream, outcome, status, from, to, q, page, size);
        return ApiResponse.success(TrafficTransactionListResponse.from(service.list(query)), clock);
    }

    @GetMapping("/transactions/{correlationId}")
    public ApiResponse<TrafficTransactionResponse> get(@PathVariable String correlationId) {
        return ApiResponse.success(TrafficTransactionResponse.from(service.get(correlationId)), clock);
    }

    @GetMapping("/stats")
    public ApiResponse<TrafficStatsResponse> stats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ApiResponse.success(TrafficStatsResponse.from(service.stats(from, to)), clock);
    }
}
