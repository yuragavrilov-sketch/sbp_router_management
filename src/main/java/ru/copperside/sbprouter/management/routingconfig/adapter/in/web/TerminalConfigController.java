package ru.copperside.sbprouter.management.routingconfig.adapter.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.copperside.sbprouter.management.common.web.ApiResponse;
import ru.copperside.sbprouter.management.routingconfig.application.SaveTerminalConfigCommand;
import ru.copperside.sbprouter.management.routingconfig.application.TerminalRoutingConfigService;

import java.time.Clock;

@RestController
@RequestMapping("/internal/v1/sbp-router-management/terminal-config")
public class TerminalConfigController {

    private final TerminalRoutingConfigService service;
    private final Clock clock;

    public TerminalConfigController(TerminalRoutingConfigService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @GetMapping
    public ApiResponse<TerminalConfigResponse> get() {
        return ApiResponse.success(service.get().map(TerminalConfigResponse::from).orElse(null), clock);
    }

    @PutMapping
    public ApiResponse<TerminalConfigResponse> save(@RequestBody TerminalConfigRequest request) {
        return ApiResponse.success(TerminalConfigResponse.from(service.save(
                new SaveTerminalConfigCommand(request.c2bFieldName(), request.b2cFieldName(), request.tkbPayPrefix()))), clock);
    }
}
