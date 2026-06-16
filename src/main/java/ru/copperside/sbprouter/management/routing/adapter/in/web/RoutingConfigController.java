package ru.copperside.sbprouter.management.routing.adapter.in.web;

import org.springframework.web.bind.annotation.*;
import ru.copperside.sbprouter.management.common.web.ApiResponse;
import ru.copperside.sbprouter.management.routing.application.RoutingConfigService;
import ru.copperside.sbprouter.management.routing.domain.RoutingConfig;
import ru.copperside.sbprouter.management.routing.domain.RoutingConfigProblemException;

import java.time.Clock;

@RestController
@RequestMapping("/internal/v1/sbp-router-management/routing-config")
public class RoutingConfigController {

    private final RoutingConfigService service;
    private final Clock clock;

    public RoutingConfigController(RoutingConfigService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @GetMapping
    public ApiResponse<RoutingConfig> get() {
        RoutingConfig config = service.latest()
                .orElseThrow(() -> new RoutingConfigProblemException("ROUTING_CONFIG_NOT_FOUND", "routing config is not set"));
        return ApiResponse.success(config, clock);
    }

    @PutMapping
    public ApiResponse<RoutingConfig> put(@RequestBody RoutingConfig config) {
        return ApiResponse.success(service.replace(config), clock);
    }
}
