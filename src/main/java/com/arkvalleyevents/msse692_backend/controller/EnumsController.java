/**
 * REST endpoints exposing enum values used by the frontend.
 */
package com.arkvalleyevents.msse692_backend.controller;

import com.arkvalleyevents.msse692_backend.dto.response.EnumOptionDto;
import com.arkvalleyevents.msse692_backend.model.EventType;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/enums")
public class EnumsController {

    /**
     * Returns EventType options as value/label pairs for UI dropdowns.
     */
    @Operation(summary = "List EventType options")
    @GetMapping("/event-types")
    public ResponseEntity<List<EnumOptionDto>> getEventTypes() {
        List<EnumOptionDto> options = Arrays.stream(EventType.values())
                .map(et -> new EnumOptionDto(et.name(), et.getTypeDisplayName()))
                .toList();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(6, TimeUnit.HOURS).cachePublic())
                .body(options);
    }
}
