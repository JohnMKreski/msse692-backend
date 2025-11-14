package com.arkvalleyevents.msse692_backend.controller;

import com.arkvalleyevents.msse692_backend.dto.response.ApiErrorDto;
import com.arkvalleyevents.msse692_backend.dto.response.FieldIssueDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.persistence.EntityNotFoundException;
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
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@ControllerAdvice
public class RestExceptionHandler {

    private ApiErrorDto build(HttpStatus status, String code, String message, HttpServletRequest req, List<FieldIssueDto> details) {
        String requestId = MDC.get("requestId");
        if (requestId == null || requestId.isBlank()) {
            requestId = req.getHeader("X-Request-ID");
        }
        return new ApiErrorDto(
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
    public ResponseEntity<ApiErrorDto> handleBodyValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<FieldIssueDto> issues = ex.getBindingResult().getAllErrors().stream()
                .map(err -> {
                    if (err instanceof FieldError fe) {
                        return new FieldIssueDto(fe.getField(), fe.getDefaultMessage(), fe.getRejectedValue());
                    }
                    return new FieldIssueDto(err.getObjectName(), err.getDefaultMessage(), null);
                })
                .collect(Collectors.toList());
        ApiErrorDto body = build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", req, issues);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorDto> handleParamValidation(ConstraintViolationException ex, HttpServletRequest req) {
        List<FieldIssueDto> issues = ex.getConstraintViolations().stream()
                .map(this::toFieldIssue)
                .collect(Collectors.toList());
        ApiErrorDto body = build(HttpStatus.BAD_REQUEST, "CONSTRAINT_VIOLATION", "Constraint violation", req, issues);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorDto> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        FieldIssueDto issue = new FieldIssueDto(ex.getName(), "Type mismatch", ex.getValue());
        ApiErrorDto body = build(HttpStatus.BAD_REQUEST, "TYPE_MISMATCH", ex.getMessage(), req, List.of(issue));
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiErrorDto> handleNotFound(NoSuchElementException ex, HttpServletRequest req) {
        ApiErrorDto body = build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), req, null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handleEntityNotFound(EntityNotFoundException ex, HttpServletRequest req) {
        ApiErrorDto body = build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), req, null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorDto> handleIllegalState(IllegalStateException ex, HttpServletRequest req) {
        ApiErrorDto body = build(HttpStatus.CONFLICT, "ILLEGAL_STATE", ex.getMessage(), req, null);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorDto> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        ApiErrorDto body = build(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", ex.getMessage(), req, null);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorDto> handleResponseStatus(ResponseStatusException ex, HttpServletRequest req) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String message = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        ApiErrorDto body = build(status, status.is4xxClientError() ? "CLIENT_ERROR" : "ERROR", message, req, null);
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorDto> handleGeneric(Exception ex, HttpServletRequest req) {
        // Avoid exposing internal exception details; log stack trace via logger if needed elsewhere.
        ApiErrorDto body = build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected server error", req, null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private FieldIssueDto toFieldIssue(ConstraintViolation<?> v) {
        String path = v.getPropertyPath() == null ? null : v.getPropertyPath().toString();
        return new FieldIssueDto(path, v.getMessage(), v.getInvalidValue());
    }
}
