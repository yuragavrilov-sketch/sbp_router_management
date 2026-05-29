package ru.copperside.sbprouter.management.traffic.application;

import ru.copperside.sbprouter.management.traffic.domain.TrafficTransaction;

import java.util.List;

public record TrafficListResult(List<TrafficTransaction> items, long total, int page, int size) {
}
