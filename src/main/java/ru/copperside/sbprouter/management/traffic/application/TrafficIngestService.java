package ru.copperside.sbprouter.management.traffic.application;

import ru.copperside.sbprouter.management.traffic.application.port.out.TrafficWriteRepository;
import ru.copperside.sbprouter.management.traffic.domain.TrafficDirection;
import ru.copperside.sbprouter.management.traffic.domain.TrafficEvent;
import ru.copperside.sbprouter.management.traffic.domain.TrafficStatus;
import ru.copperside.sbprouter.management.traffic.domain.TrafficTransaction;

import java.time.Clock;
import java.time.Instant;

public class TrafficIngestService {

    private final TrafficWriteRepository repository;
    private final Clock clock;

    public TrafficIngestService(TrafficWriteRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void record(TrafficEvent event) {
        Instant now = Instant.now(clock);
        boolean isRequest = event.direction() == TrafficDirection.REQUEST;
        TrafficTransaction partial = new TrafficTransaction(
                event.correlationKey(),
                event.txId(),
                event.requestType(),
                isRequest ? event.operationId() : null,   // operationId: set from request; response may be null
                isRequest ? event.operationType() : null, // operationType: set from request; response may be null
                isRequest ? event.terminalOwner() : null,
                isRequest ? event.route() : null,
                isRequest ? null : event.upstream(),
                isRequest ? null : event.outcome(),
                TrafficStatus.PENDING,                 // recomputed by the upsert
                isRequest ? event.timestamp() : null,  // requestAt
                isRequest ? null : event.timestamp(),  // responseAt
                null,                                   // latency recomputed by the upsert
                event.env(),
                isRequest ? event.body() : null,        // requestXml
                isRequest ? null : event.body(),        // responseXml
                now,
                now);
        repository.upsert(partial);
    }
}
