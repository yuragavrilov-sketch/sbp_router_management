package ru.copperside.sbprouter.management.common.web;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.copperside.sbprouter.management.routing.domain.RoutingConfigProblemException;
import ru.copperside.sbprouter.management.traffic.domain.TrafficTransactionProblemException;

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
        HttpStatus status = ex.code().equals("ROUTING_CONFIG_NOT_FOUND") ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        return problem(status, ex.code(), "Routing config problem", messageWithoutCode(ex), null);
    }

    @ExceptionHandler(TrafficTransactionProblemException.class)
    ResponseEntity<ProblemEnvelope> handleTrafficProblem(TrafficTransactionProblemException ex) {
        HttpStatus status = ex.code().equals("TRAFFIC_TRANSACTION_NOT_FOUND")
                ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        return problem(status, ex.code(), "Traffic transaction problem", messageWithoutCode(ex), null);
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
