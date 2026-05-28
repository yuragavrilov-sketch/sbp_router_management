package ru.copperside.sbprouter.management.routingconfig.adapter.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.copperside.sbprouter.management.common.web.ApiResponse;
import ru.copperside.sbprouter.management.routingconfig.application.ExtractionRuleService;
import ru.copperside.sbprouter.management.routingconfig.application.SaveExtractionRuleCommand;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/sbp-router-management/extraction-rules")
public class ExtractionRuleController {

    private final ExtractionRuleService service;
    private final Clock clock;

    public ExtractionRuleController(ExtractionRuleService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @GetMapping
    public ApiResponse<List<ExtractionRuleResponse>> list() {
        return ApiResponse.success(service.list().stream().map(ExtractionRuleResponse::from).toList(), clock);
    }

    @PostMapping
    public ApiResponse<ExtractionRuleResponse> create(@RequestBody ExtractionRuleRequest request) {
        return ApiResponse.success(ExtractionRuleResponse.from(service.create(
                new SaveExtractionRuleCommand(request.messageType(), request.routingFields(), request.extraFields()))), clock);
    }

    @PatchMapping("/{id}")
    public ApiResponse<ExtractionRuleResponse> patch(@PathVariable UUID id, @RequestBody ExtractionRuleRequest request) {
        return ApiResponse.success(ExtractionRuleResponse.from(service.patch(id,
                new SaveExtractionRuleCommand(request.messageType(), request.routingFields(), request.extraFields()))), clock);
    }
}
