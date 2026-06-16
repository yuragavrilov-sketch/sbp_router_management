package ru.copperside.sbprouter.management.fleet.adapter.in.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.copperside.sbprouter.management.fleet.domain.RouterInstance;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Parses a router heartbeat JSON payload into a {@link RouterInstance}. Returns null on bad input. */
public class HeartbeatMapper {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatMapper.class);

    private final ObjectMapper mapper;

    public HeartbeatMapper(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public RouterInstance map(byte[] json) {
        if (json == null || json.length == 0) {
            return null;
        }
        try {
            JsonNode n = mapper.readTree(json);
            String instanceId = text(n, "instanceId");
            if (instanceId == null) {
                return null;
            }
            return new RouterInstance(
                    instanceId,
                    instant(n, "startedAt"),
                    instant(n, "timestamp"),
                    text(n, "activeGroup"),
                    stringList(n.path("groups")),
                    backends(n.path("backends")),
                    metrics(n.path("metrics")));
        } catch (Exception e) {
            log.warn("fleet: dropping unparseable heartbeat: {}", e.toString());
            return null;
        }
    }

    private static List<RouterInstance.RouterBackend> backends(JsonNode arr) {
        List<RouterInstance.RouterBackend> out = new ArrayList<>();
        if (arr.isArray()) {
            for (JsonNode b : arr) {
                out.add(new RouterInstance.RouterBackend(
                        text(b, "url"), text(b, "group"), b.path("banned").asBoolean(false)));
            }
        }
        return out;
    }

    private static RouterInstance.RouterMetrics metrics(JsonNode m) {
        return new RouterInstance.RouterMetrics(
                m.path("activeRequests").asInt(0),
                m.path("requestsTotal").asDouble(0),
                m.path("upstreamErrorsTotal").asDouble(0),
                m.path("kafkaPublishedTotal").asDouble(0),
                m.path("requestCount").asLong(0),
                m.path("avgLatencyMs").asDouble(0),
                m.path("maxLatencyMs").asDouble(0));
    }

    private static List<String> stringList(JsonNode arr) {
        List<String> out = new ArrayList<>();
        if (arr.isArray()) {
            arr.forEach(e -> out.add(e.asText()));
        }
        return out;
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.path(field);
        return v.isMissingNode() || v.isNull() ? null : v.asText();
    }

    private static Instant instant(JsonNode n, String field) {
        String v = text(n, field);
        if (v == null) {
            return null;
        }
        try {
            return Instant.parse(v);
        } catch (Exception e) {
            return null;
        }
    }
}
