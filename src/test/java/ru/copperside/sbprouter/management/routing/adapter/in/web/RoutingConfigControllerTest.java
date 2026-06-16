package ru.copperside.sbprouter.management.routing.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import ru.copperside.sbprouter.management.routing.application.RoutingConfigService;
import ru.copperside.sbprouter.management.routing.domain.RoutingConfig;
import ru.copperside.sbprouter.management.routing.domain.RoutingConfigProblemException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoutingConfigController.class)
@Import({TimeConfig.class, GlobalExceptionHandler.class, RoutingConfigControllerTest.MockConfig.class})
class RoutingConfigControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    RoutingConfigService service;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static RoutingConfig validConfig(Long version) {
        return new RoutingConfig(version, "default",
                Map.of("default", new RoutingConfig.Group(List.of("http://a/api"))));
    }

    @Test
    void getReturns200WithConfig() throws Exception {
        when(service.latest()).thenReturn(Optional.of(validConfig(1L)));

        mockMvc.perform(get("/internal/v1/sbp-router-management/routing-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.activeGroup").value("default"))
                .andExpect(jsonPath("$.data.groups.default.backends[0]").value("http://a/api"));
    }

    @Test
    void getReturns404WhenEmpty() throws Exception {
        when(service.latest()).thenReturn(Optional.empty());

        mockMvc.perform(get("/internal/v1/sbp-router-management/routing-config"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ROUTING_CONFIG_NOT_FOUND"));
    }

    @Test
    void putReturns200WithSavedConfig() throws Exception {
        RoutingConfig saved = validConfig(2L);
        when(service.replace(any())).thenReturn(saved);

        String body = MAPPER.writeValueAsString(validConfig(null));

        mockMvc.perform(put("/internal/v1/sbp-router-management/routing-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(2))
                .andExpect(jsonPath("$.data.activeGroup").value("default"));
    }

    @Test
    void putReturns400WhenServiceThrowsInvalidConfig() throws Exception {
        when(service.replace(any()))
                .thenThrow(new RoutingConfigProblemException("ROUTING_CONFIG_INVALID", "activeGroup 'nope' must be one of [default]"));

        String body = MAPPER.writeValueAsString(validConfig(null));

        mockMvc.perform(put("/internal/v1/sbp-router-management/routing-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("ROUTING_CONFIG_INVALID"));
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        RoutingConfigService routingConfigService() {
            return mock(RoutingConfigService.class);
        }
    }
}
