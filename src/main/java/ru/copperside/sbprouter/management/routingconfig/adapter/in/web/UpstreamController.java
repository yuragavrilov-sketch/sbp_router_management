package ru.copperside.sbprouter.management.routingconfig.adapter.in.web;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.copperside.sbprouter.management.common.web.ApiResponse;
import ru.copperside.sbprouter.management.routingconfig.application.CreateUpstreamCommand;
import ru.copperside.sbprouter.management.routingconfig.application.PatchUpstreamCommand;
import ru.copperside.sbprouter.management.routingconfig.application.UpstreamService;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/sbp-router-management/upstreams")
public class UpstreamController {

    private final UpstreamService service;
    private final Clock clock;

    public UpstreamController(UpstreamService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @GetMapping
    public ApiResponse<List<UpstreamResponse>> list() {
        return ApiResponse.success(service.list().stream().map(UpstreamResponse::from).toList(), clock);
    }

    @PostMapping
    public ApiResponse<UpstreamResponse> create(@RequestBody UpstreamRequest request) {
        return ApiResponse.success(UpstreamResponse.from(service.create(new CreateUpstreamCommand(
                request.name(), request.url(), request.timeoutMs(),
                request.retryMaxAttempts(), request.retryBackoffMs()))), clock);
    }

    @PatchMapping("/{id}")
    public ApiResponse<UpstreamResponse> patch(@PathVariable UUID id, @RequestBody UpstreamRequest request) {
        return ApiResponse.success(UpstreamResponse.from(service.patch(id, new PatchUpstreamCommand(
                request.url(), request.timeoutMs(),
                request.retryMaxAttempts(), request.retryBackoffMs()))), clock);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<UpstreamResponse> remove(@PathVariable UUID id) {
        return ApiResponse.success(UpstreamResponse.from(service.markRemoval(id)), clock);
    }
}
