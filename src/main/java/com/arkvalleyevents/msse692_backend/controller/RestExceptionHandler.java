package com.arkvalleyevents.msse692_backend.controller;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleBodyValidation(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest().body(ex.getBindingResult().toString());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> handleParamValidation(ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
