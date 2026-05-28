package ru.copperside.sbprouter.management.routingconfig.application;

public record SaveTerminalConfigCommand(
        String c2bFieldName,
        String b2cFieldName,
        String tkbPayPrefix
) {
}
