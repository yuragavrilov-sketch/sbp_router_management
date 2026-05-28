package ru.copperside.sbprouter.management.routingmanifest.domain;

import java.util.List;

public class RoutingManifestProblemException extends RuntimeException {

    private final String code;
    private final RoutingManifestDiagnosticsDetails details;

    public RoutingManifestProblemException(String code, String message) {
        this(code, message, List.of());
    }

    public RoutingManifestProblemException(String code, String message, List<ManifestDiagnostic> diagnostics) {
        super(code + ": " + message);
        this.code = code;
        this.details = new RoutingManifestDiagnosticsDetails(diagnostics);
    }

    public String code() {
        return code;
    }

    public List<ManifestDiagnostic> diagnostics() {
        return details.diagnostics();
    }

    public RoutingManifestDiagnosticsDetails details() {
        return details;
    }
}
