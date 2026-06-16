package ru.copperside.sbprouter.management.fleet.application;

import org.springframework.stereotype.Component;
import ru.copperside.sbprouter.management.fleet.domain.RouterInstance;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry of router instances keyed by instanceId. Keeps the newest heartbeat per
 * instance (by heartbeat timestamp, so out-of-order delivery can't regress state) and supports
 * purging instances that went silent.
 */
@Component
public class FleetRegistry {

    private final Map<String, RouterInstance> instances = new ConcurrentHashMap<>();

    public void record(RouterInstance instance) {
        if (instance == null || instance.instanceId() == null) {
            return;
        }
        instances.merge(instance.instanceId(), instance, FleetRegistry::newer);
    }

    private static RouterInstance newer(RouterInstance current, RouterInstance incoming) {
        if (incoming.lastHeartbeat() == null) {
            return current;
        }
        if (current.lastHeartbeat() == null || !incoming.lastHeartbeat().isBefore(current.lastHeartbeat())) {
            return incoming;
        }
        return current;
    }

    public List<RouterInstance> all() {
        return List.copyOf(instances.values());
    }

    /** Drops instances whose last heartbeat is older than {@code cutoff} (bounds memory). */
    public void purgeOlderThan(Instant cutoff) {
        instances.values().removeIf(i -> i.lastHeartbeat() == null || i.lastHeartbeat().isBefore(cutoff));
    }
}
