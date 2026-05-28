package ru.copperside.sbprouter.management.routingconfig.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.copperside.sbprouter.management.common.config.TimeConfig;
import ru.copperside.sbprouter.management.common.web.GlobalExceptionHandler;
import ru.copperside.sbprouter.management.routingconfig.application.CreateUpstreamCommand;
import ru.copperside.sbprouter.management.routingconfig.application.UpstreamService;
import ru.copperside.sbprouter.management.routingconfig.domain.ConfigStatus;
import ru.copperside.sbprouter.management.routingconfig.domain.Upstream;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UpstreamController.class)
@Import({TimeConfig.class, GlobalExceptionHandler.class, UpstreamControllerTest.MockConfig.class})
class UpstreamControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UpstreamService service;

    @Test
    void createReturnsDraftEnvelope() throws Exception {
        Instant now = Instant.parse("2026-05-28T09:00:00Z");
        when(service.create(any(CreateUpstreamCommand.class))).thenReturn(
                new Upstream(UUID.randomUUID(), "infosrv", "http://infosrv/api", 30000, 2, 500,
                        ConfigStatus.DRAFT, false, 1, now, now));

        mockMvc.perform(post("/internal/v1/sbp-router-management/upstreams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"infosrv","url":"http://infosrv/api","timeoutMs":30000,"retryMaxAttempts":2,"retryBackoffMs":500}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("infosrv"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        UpstreamService upstreamService() {
            return org.mockito.Mockito.mock(UpstreamService.class);
        }
    }
}
