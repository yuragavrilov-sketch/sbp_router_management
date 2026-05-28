package ru.copperside.sbprouter.management.routingmanifest.adapter.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.copperside.sbprouter.management.common.web.ApiResponse;
import ru.copperside.sbprouter.management.routingmanifest.application.RoutingManifestService;

import java.time.Clock;

@RestController
@RequestMapping("/internal/v1/sbp-router-management/pending-changes")
public class PendingChangesController {

    private final RoutingManifestService service;
    private final Clock clock;

    public PendingChangesController(RoutingManifestService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @GetMapping
    public ApiResponse<PendingChangesResponse> pendingChanges() {
        return ApiResponse.success(PendingChangesResponse.from(service.pendingChanges()), clock);
    }
}
