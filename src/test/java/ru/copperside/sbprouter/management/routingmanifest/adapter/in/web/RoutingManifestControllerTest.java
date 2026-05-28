package ru.copperside.sbprouter.management.routingmanifest.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import ru.copperside.sbprouter.management.common.config.TimeConfig;
import ru.copperside.sbprouter.management.common.web.GlobalExceptionHandler;
import ru.copperside.sbprouter.management.routingmanifest.application.RoutingManifestService;
import ru.copperside.sbprouter.management.routingmanifest.domain.RoutingManifest;
import ru.copperside.sbprouter.management.routingmanifest.domain.RoutingManifestContent;
import ru.copperside.sbprouter.management.routingmanifest.domain.RoutingManifestPayload;
import ru.copperside.sbprouter.management.routingmanifest.domain.RoutingManifestProblemException;
import ru.copperside.sbprouter.management.routingmanifest.domain.RoutingManifestStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoutingManifestController.class)
@Import({TimeConfig.class, GlobalExceptionHandler.class, RoutingManifestControllerTest.MockConfig.class})
class RoutingManifestControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    RoutingManifestService service;

    private RoutingManifest sampleManifest() {
        Instant now = Instant.parse("2026-05-28T09:00:00Z");
        RoutingManifestContent content = new RoutingManifestContent(
                Map.of(), new RoutingManifestContent.TerminalsPayload("rcvTspId", "terminalName", "Pay", List.of()),
                Map.of(), Map.of());
        RoutingManifestPayload payload = new RoutingManifestPayload(1, RoutingManifestStatus.VALID, now, content, List.of());
        return new RoutingManifest(UUID.randomUUID(), 1, RoutingManifestStatus.VALID, "sha256:abc", now, content, payload);
    }

    @Test
    void publishReturnsManifestEnvelope() throws Exception {
        when(service.publish()).thenReturn(sampleManifest());

        mockMvc.perform(post("/internal/v1/sbp-router-management/routing-manifests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.status").value("VALID"))
                .andExpect(jsonPath("$.data.checksum").value("sha256:abc"));
    }

    @Test
    void latestReturns404ProblemWhenMissing() throws Exception {
        when(service.latest()).thenThrow(
                new RoutingManifestProblemException("ROUTING_MANIFEST_NOT_FOUND", "Routing manifest not found"));

        mockMvc.perform(get("/internal/v1/sbp-router-management/routing-manifests/latest"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ROUTING_MANIFEST_NOT_FOUND"));
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        RoutingManifestService routingManifestService() {
            return mock(RoutingManifestService.class);
        }
    }
}
