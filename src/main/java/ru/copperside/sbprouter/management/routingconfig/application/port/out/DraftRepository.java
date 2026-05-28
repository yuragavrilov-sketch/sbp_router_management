package ru.copperside.sbprouter.management.routingconfig.application.port.out;

public interface DraftRepository {
    /** Delete all DRAFT rows across config tables and clear removal flags on ACTIVE rows. */
    void discardAll();
}
