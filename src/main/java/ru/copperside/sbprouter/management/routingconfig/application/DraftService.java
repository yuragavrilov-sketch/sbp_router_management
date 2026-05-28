package ru.copperside.sbprouter.management.routingconfig.application;

import ru.copperside.sbprouter.management.routingconfig.application.port.out.DraftRepository;

public class DraftService {

    private final DraftRepository repository;

    public DraftService(DraftRepository repository) {
        this.repository = repository;
    }

    public void discardAll() {
        repository.discardAll();
    }
}
