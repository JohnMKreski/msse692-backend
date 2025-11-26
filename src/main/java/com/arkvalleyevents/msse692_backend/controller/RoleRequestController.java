package com.arkvalleyevents.msse692_backend.controller;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.arkvalleyevents.msse692_backend.dto.request.RoleRequestCreateDto;
import com.arkvalleyevents.msse692_backend.dto.response.RoleRequestDto;
import com.arkvalleyevents.msse692_backend.dto.response.ApiErrorDto;
import com.arkvalleyevents.msse692_backend.model.RoleRequestStatus;
import com.arkvalleyevents.msse692_backend.service.RoleRequestService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("isAuthenticated()")
@Tag(name = "User Role Requests", description = "User role request management")
public class RoleRequestController {
    private final RoleRequestService service;
    
    public RoleRequestController(RoleRequestService service) {
        this.service = service;
    }

    @PostMapping("/roles/requests")
    @Operation(
        summary = "Create role request", 
        description = "Authenticated users create a role elevation request (e.g., EDITOR)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = RoleRequestDto.class))),
        @ApiResponse(responseCode = "400", description = "Bad Request",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public RoleRequestDto create(@AuthenticationPrincipal Jwt principal, @RequestBody @Valid RoleRequestCreateDto body) {
        return service.create(principal.getSubject(), body);
    }

    @GetMapping("/roles/requests")
    @Operation(
        summary = "List my role requests", 
        description = "Authenticated users list their own role elevation requests with optional status filter."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = RoleRequestDto.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public Page<RoleRequestDto> myRequests(@AuthenticationPrincipal Jwt principal, @RequestParam Optional<RoleRequestStatus> status, Pageable pageable) {
        return service.listForUser(principal.getSubject(), status, pageable);
    }

    @PostMapping("/roles/requests/{id}/cancel")
    @Operation(
        summary = "Cancel role request", 
        description = "Authenticated users cancel their own pending role request by ID."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = RoleRequestDto.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public RoleRequestDto cancel(@AuthenticationPrincipal Jwt principal, @PathVariable UUID id) {
        return service.cancel(principal.getSubject(), id);
    }
}

