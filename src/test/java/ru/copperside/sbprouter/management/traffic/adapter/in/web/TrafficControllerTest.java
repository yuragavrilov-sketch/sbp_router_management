package ru.copperside.sbprouter.management.traffic.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import ru.copperside.sbprouter.management.common.config.TimeConfig;
import ru.copperside.sbprouter.management.common.web.GlobalExceptionHandler;
import ru.copperside.sbprouter.management.traffic.application.TrafficListResult;
import ru.copperside.sbprouter.management.traffic.application.TrafficQueryService;
import ru.copperside.sbprouter.management.traffic.domain.TrafficStatus;
import ru.copperside.sbprouter.management.traffic.domain.TrafficTransaction;
import ru.copperside.sbprouter.management.traffic.domain.TrafficTransactionProblemException;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TrafficController.class)
@Import({TimeConfig.class, GlobalExceptionHandler.class, TrafficControllerTest.MockConfig.class})
class TrafficControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TrafficQueryService service;

    @Test
    void listReturnsEnvelope() throws Exception {
        Instant now = Instant.parse("2026-05-29T09:00:00Z");
        TrafficTransaction t = new TrafficTransaction("corr-1", "tx-1", "ReqAuthPay", "owner-A", "route-x",
                "infosrv", "Code=0", TrafficStatus.RESPONDED, now, now.plusMillis(40), 40L, "local", null, null, now, now);
        when(service.list(any())).thenReturn(new TrafficListResult(List.of(t), 1, 0, 50));

        mockMvc.perform(get("/internal/v1/sbp-router-management/traffic/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].correlationId").value("corr-1"))
                .andExpect(jsonPath("$.data.items[0].latencyMs").value(40))
                .andExpect(jsonPath("$.data.items[0].requestXml").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].responseXml").doesNotExist());
    }

    @Test
    void detailNotFoundReturns404Problem() throws Exception {
        when(service.get(any())).thenThrow(
                new TrafficTransactionProblemException("TRAFFIC_TRANSACTION_NOT_FOUND", "Traffic transaction not found"));

        mockMvc.perform(get("/internal/v1/sbp-router-management/traffic/transactions/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("TRAFFIC_TRANSACTION_NOT_FOUND"));
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        TrafficQueryService trafficQueryService() {
            return mock(TrafficQueryService.class);
        }
    }
}
