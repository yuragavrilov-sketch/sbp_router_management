package ru.copperside.sbprouter.management.common.web;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.copperside.sbprouter.management.routingconfig.domain.RoutingConfigProblemException;
import ru.copperside.sbprouter.management.routingmanifest.domain.RoutingManifestProblemException;

import java.time.Clock;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String TYPE_BASE = "https://contracts.newpay/errors/";

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemEnvelope> handleBodyValidation(MethodArgumentNotValidException ex) {
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed", ex.getMessage(), null);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ProblemEnvelope> handleConstraintValidation(ConstraintViolationException ex) {
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed", ex.getMessage(), null);
    }

    @ExceptionHandler(RoutingConfigProblemException.class)
    ResponseEntity<ProblemEnvelope> handleRoutingConfigProblem(RoutingConfigProblemException ex) {
        HttpStatus status = switch (ex.code()) {
            case "UPSTREAM_NOT_FOUND", "EXTRACTION_RULE_NOT_FOUND", "TERMINAL_CONFIG_NOT_FOUND",
                 "TKB_PAY_ENTRY_NOT_FOUND", "ROUTING_FLAG_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "VALIDATION_ERROR" -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.CONFLICT;
        };
        return problem(status, ex.code(), "Routing config problem", messageWithoutCode(ex), null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemEnvelope> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return problem(HttpStatus.CONFLICT, "ROUTING_MANIFEST_CONFLICT",
                "Routing manifest conflict", "Concurrent publish conflict", null);
    }

    @ExceptionHandler(RoutingManifestProblemException.class)
    ResponseEntity<ProblemEnvelope> handleRoutingManifestProblem(RoutingManifestProblemException ex) {
        HttpStatus status = switch (ex.code()) {
            case "ROUTING_MANIFEST_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "VALIDATION_ERROR" -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.CONFLICT;
        };
        return problem(status, ex.code(), titleForManifestProblem(ex.code()), messageWithoutCode(ex), ex.details());
    }

    private String titleForManifestProblem(String code) {
        return switch (code) {
            case "ROUTING_MANIFEST_CONFLICT" -> "Routing manifest conflict";
            case "ROUTING_MANIFEST_NOT_FOUND" -> "Routing manifest not found";
            default -> "Routing manifest problem";
        };
    }

    private ResponseEntity<ProblemEnvelope> problem(
            HttpStatus status, String code, String title, String message, Object details) {
        ProblemDetail detail = new ProblemDetail(
                TYPE_BASE + code.toLowerCase().replace('_', '-'),
                title,
                status.value(),
                code,
                message,
                details,
                UUID.randomUUID().toString()
        );
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(ProblemEnvelope.of(detail, clock));
    }

    private String messageWithoutCode(RuntimeException ex) {
        String message = ex.getMessage();
        int separator = message == null ? -1 : message.indexOf(": ");
        return separator < 0 ? message : message.substring(separator + 2);
    }
}
