package ru.copperside.sbprouter.management.fleet.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import ru.copperside.sbprouter.management.common.config.TimeConfig;
import ru.copperside.sbprouter.management.common.web.GlobalExceptionHandler;
import ru.copperside.sbprouter.management.fleet.application.FleetRegistry;
import ru.copperside.sbprouter.management.fleet.config.FleetProperties;
import ru.copperside.sbprouter.management.fleet.domain.RouterInstance;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FleetController.class)
@Import({TimeConfig.class, GlobalExceptionHandler.class, FleetControllerTest.MockConfig.class})
class FleetControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void listReturnsFleetWithUpAndStaleStatus() throws Exception {
        mockMvc.perform(get("/internal/v1/sbp-router-management/routers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.up").value(1))
                // sorted by instanceId: "stale-1" before "up-1"
                .andExpect(jsonPath("$.data.routers[0].instanceId").value("stale-1"))
                .andExpect(jsonPath("$.data.routers[0].status").value("STALE"))
                .andExpect(jsonPath("$.data.routers[1].instanceId").value("up-1"))
                .andExpect(jsonPath("$.data.routers[1].status").value("UP"))
                .andExpect(jsonPath("$.data.routers[1].activeGroup").value("default"))
                .andExpect(jsonPath("$.data.routers[1].backends[0].url").value("http://a/api"));
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        FleetRegistry fleetRegistry() {
            FleetRegistry r = new FleetRegistry();
            Instant now = Instant.now();
            r.record(new RouterInstance("up-1", now.minusSeconds(120), now.minusSeconds(5), "default",
                    List.of("default"),
                    List.of(new RouterInstance.RouterBackend("http://a/api", "default", false)),
                    new RouterInstance.RouterMetrics(1, 5, 0, 10, 5, 40, 100)));
            r.record(new RouterInstance("stale-1", now.minusSeconds(600), now.minusSeconds(300), "default",
                    List.of("default"), List.of(),
                    new RouterInstance.RouterMetrics(0, 0, 0, 0, 0, 0, 0)));
            return r;
        }

        @Bean
        FleetProperties fleetProperties() {
            return new FleetProperties("sbp-router-heartbeat", "g", Duration.ofSeconds(45));
        }
    }
}
