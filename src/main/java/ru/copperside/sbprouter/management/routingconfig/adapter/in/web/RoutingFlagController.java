package ru.copperside.sbprouter.management.routingconfig.adapter.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.copperside.sbprouter.management.common.web.ApiResponse;
import ru.copperside.sbprouter.management.routingconfig.application.RoutingFlagService;

import java.time.Clock;
import java.util.List;

@RestController
@RequestMapping("/internal/v1/sbp-router-management/routing-flags")
public class RoutingFlagController {

    private final RoutingFlagService service;
    private final Clock clock;

    public RoutingFlagController(RoutingFlagService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @GetMapping
    public ApiResponse<List<RoutingFlagResponse>> list() {
        return ApiResponse.success(service.list().stream().map(RoutingFlagResponse::from).toList(), clock);
    }

    @PutMapping("/{key}")
    public ApiResponse<RoutingFlagResponse> set(@PathVariable String key, @RequestBody RoutingFlagRequest request) {
        return ApiResponse.success(RoutingFlagResponse.from(service.set(key, request.value())), clock);
    }
}
