package com.arkvalleyevents.msse692_backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@ControllerAdvice
public class RestExceptionHandler {

    record ErrorResponse(OffsetDateTime timestamp,
                         int status,
                         String error,
                         String code,
                         String message,
                         String path,
                         String requestId,
                         List<FieldIssue> details) {}

    record FieldIssue(String field, String message, Object rejectedValue) {}

    private ErrorResponse build(HttpStatus status, String code, String message, HttpServletRequest req, List<FieldIssue> details) {
        String requestId = MDC.get("requestId");
        if (requestId == null || requestId.isBlank()) {
            requestId = req.getHeader("X-Request-ID");
        }
        return new ErrorResponse(
                OffsetDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                req.getRequestURI(),
                requestId,
                details == null || details.isEmpty() ? null : details
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<FieldIssue> issues = ex.getBindingResult().getAllErrors().stream()
                .map(err -> {
                    if (err instanceof FieldError fe) {
                        return new FieldIssue(fe.getField(), fe.getDefaultMessage(), fe.getRejectedValue());
                    }
                    return new FieldIssue(err.getObjectName(), err.getDefaultMessage(), null);
                })
                .collect(Collectors.toList());
        ErrorResponse body = build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", req, issues);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleParamValidation(ConstraintViolationException ex, HttpServletRequest req) {
        List<FieldIssue> issues = ex.getConstraintViolations().stream()
                .map(this::toFieldIssue)
                .collect(Collectors.toList());
        ErrorResponse body = build(HttpStatus.BAD_REQUEST, "CONSTRAINT_VIOLATION", "Constraint violation", req, issues);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        FieldIssue issue = new FieldIssue(ex.getName(), "Type mismatch", ex.getValue());
        ErrorResponse body = build(HttpStatus.BAD_REQUEST, "TYPE_MISMATCH", ex.getMessage(), req, List.of(issue));
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException ex, HttpServletRequest req) {
        ErrorResponse body = build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), req, null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex, HttpServletRequest req) {
        ErrorResponse body = build(HttpStatus.CONFLICT, "ILLEGAL_STATE", ex.getMessage(), req, null);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        // Avoid exposing internal exception details; log stack trace via logger if needed elsewhere.
        ErrorResponse body = build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected server error", req, null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private FieldIssue toFieldIssue(ConstraintViolation<?> v) {
        String path = v.getPropertyPath() == null ? null : v.getPropertyPath().toString();
        return new FieldIssue(path, v.getMessage(), v.getInvalidValue());
    }
}
