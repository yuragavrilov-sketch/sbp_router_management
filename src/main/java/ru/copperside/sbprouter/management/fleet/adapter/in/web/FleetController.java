package ru.copperside.sbprouter.management.fleet.adapter.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.copperside.sbprouter.management.common.web.ApiResponse;
import ru.copperside.sbprouter.management.fleet.application.FleetRegistry;
import ru.copperside.sbprouter.management.fleet.config.FleetProperties;

import java.time.Clock;

/**
 * Read-only fleet view: the running sbp-router instances assembled from their heartbeats, each with
 * a UP/STALE status derived from the configured TTL. Guarded by the internal-admin-key like all
 * {@code /internal/**} endpoints.
 */
@RestController
@RequestMapping("/internal/v1/sbp-router-management/routers")
public class FleetController {

    private final FleetRegistry registry;
    private final FleetProperties properties;
    private final Clock clock;

    public FleetController(FleetRegistry registry, FleetProperties properties, Clock clock) {
        this.registry = registry;
        this.properties = properties;
        this.clock = clock;
    }

    @GetMapping
    public ApiResponse<FleetResponse> list() {
        FleetResponse body = FleetResponse.from(registry.all(), clock.instant(), properties.ttl());
        return ApiResponse.success(body, clock);
    }
}
