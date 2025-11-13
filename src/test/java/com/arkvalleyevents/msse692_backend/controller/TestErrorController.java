package com.arkvalleyevents.msse692_backend.controller;

import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.util.NoSuchElementException;

@RestController
@Validated
class TestErrorController {

    @GetMapping("/test/violation")
    public void violation(@RequestParam(name = "value", defaultValue = "0") @Min(value = 1, message = "must be greater than 0") int value) {
        // If validation passes, do nothing
    }

    @GetMapping("/test/not-found")
    public void notFound() {
        throw new NoSuchElementException("Item not found");
    }

    @GetMapping("/test/generic")
    public void generic() {
        throw new RuntimeException("Boom");
    }
}
