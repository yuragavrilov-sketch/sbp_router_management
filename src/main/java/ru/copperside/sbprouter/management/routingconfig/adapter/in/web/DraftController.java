package ru.copperside.sbprouter.management.routingconfig.adapter.in.web;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.copperside.sbprouter.management.common.web.ApiResponse;
import ru.copperside.sbprouter.management.routingconfig.application.DraftService;

import java.time.Clock;
import java.util.Map;

@RestController
@RequestMapping("/internal/v1/sbp-router-management/drafts")
public class DraftController {

    private final DraftService service;
    private final Clock clock;

    public DraftController(DraftService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @DeleteMapping
    public ApiResponse<Map<String, String>> discard() {
        service.discardAll();
        return ApiResponse.success(Map.of("status", "discarded"), clock);
    }
}
