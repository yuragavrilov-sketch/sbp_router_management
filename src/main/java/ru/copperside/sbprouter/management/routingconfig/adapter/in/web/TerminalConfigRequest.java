package ru.copperside.sbprouter.management.routingconfig.adapter.in.web;

public record TerminalConfigRequest(
        String c2bFieldName,
        String b2cFieldName,
        String tkbPayPrefix
) {
}
