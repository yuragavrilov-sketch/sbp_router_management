package ru.copperside.sbprouter.management.routingmanifest.adapter.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.copperside.sbprouter.management.common.web.ApiResponse;
import ru.copperside.sbprouter.management.routingmanifest.application.RoutingManifestService;

import java.time.Clock;
import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/sbp-router-management/routing-manifests")
public class RoutingManifestController {

    private final RoutingManifestService service;
    private final Clock clock;

    public RoutingManifestController(RoutingManifestService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @PostMapping
    public ApiResponse<RoutingManifestResponse> publish() {
        return ApiResponse.success(RoutingManifestResponse.from(service.publish()), clock);
    }

    @GetMapping("/latest")
    public ApiResponse<RoutingManifestResponse> latest() {
        return ApiResponse.success(RoutingManifestResponse.from(service.latest()), clock);
    }

    @GetMapping("/{manifestId}")
    public ApiResponse<RoutingManifestResponse> get(@PathVariable UUID manifestId) {
        return ApiResponse.success(RoutingManifestResponse.from(service.get(manifestId)), clock);
    }
}
