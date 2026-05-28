package ru.copperside.sbprouter.management.routingconfig.adapter.in.web;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.copperside.sbprouter.management.common.web.ApiResponse;
import ru.copperside.sbprouter.management.routingconfig.application.TkbPayListService;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/sbp-router-management/tkb-pay-list")
public class TkbPayListController {

    private final TkbPayListService service;
    private final Clock clock;

    public TkbPayListController(TkbPayListService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @GetMapping
    public ApiResponse<List<TkbPayEntryResponse>> list() {
        return ApiResponse.success(service.list().stream().map(TkbPayEntryResponse::from).toList(), clock);
    }

    @PostMapping
    public ApiResponse<TkbPayEntryResponse> add(@RequestBody TkbPayEntryRequest request) {
        return ApiResponse.success(TkbPayEntryResponse.from(service.add(request.rcvTspId())), clock);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<TkbPayEntryResponse> remove(@PathVariable UUID id) {
        return ApiResponse.success(TkbPayEntryResponse.from(service.markRemoval(id)), clock);
    }
}
