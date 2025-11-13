/**
 * REST endpoints exposing enum values used by the frontend.
 */
package com.arkvalleyevents.msse692_backend.controller;

import com.arkvalleyevents.msse692_backend.dto.response.EnumOptionDto;
import com.arkvalleyevents.msse692_backend.model.EventType;
import com.arkvalleyevents.msse692_backend.model.EventStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/enums") // API versioned base path (added v1)
@Tag(name = "Enums", description = "Reference data used by the frontend")
public class EnumsController {

    /**
     * Returns EventType options as value/label pairs for UI dropdowns.
     */
    @Operation(summary = "List EventType options")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = EnumOptionDto.class))))
    })
    @GetMapping("/event-types") // GET /api/v1/enums/event-types
    public ResponseEntity<List<EnumOptionDto>> getEventTypes() {
        List<EnumOptionDto> options = Arrays.stream(EventType.values())
                .map(et -> new EnumOptionDto(et.name(), et.getTypeDisplayName()))
                .toList();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(6, TimeUnit.HOURS).cachePublic())
                .body(options);
    }

    /**
     * Returns EventStatus options as value/label pairs for UI dropdowns and logic.
     */
    @Operation(summary = "List EventStatus options")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = EnumOptionDto.class))))
    })
    @GetMapping("/event-statuses") // GET /api/v1/enums/event-statuses
    public ResponseEntity<List<EnumOptionDto>> getEventStatuses() {
    List<EnumOptionDto> options = Arrays.stream(EventStatus.values())
        .map(es -> new EnumOptionDto(es.name(), es.getStatusDisplayName()))
        .toList();

    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(6, TimeUnit.HOURS).cachePublic())
        .body(options);
    }
}
