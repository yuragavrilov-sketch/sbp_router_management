package ru.copperside.sbprouter.management.traffic.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

public class TrafficRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(TrafficRetentionJob.class);

    private final TrafficRetentionService service;

    public TrafficRetentionJob(TrafficRetentionService service) {
        this.service = service;
    }

    @Scheduled(
            initialDelayString = "${traffic.retention.initial-delay-ms:3600000}",
            fixedDelayString = "${traffic.retention.fixed-delay-ms:3600000}")
    public void purge() {
        int deleted = service.purge();
        if (deleted > 0) {
            log.info("Traffic retention purged {} transactions", deleted);
        }
    }
}
