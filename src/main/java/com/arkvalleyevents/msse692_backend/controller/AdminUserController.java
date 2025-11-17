package com.arkvalleyevents.msse692_backend.controller;

import com.arkvalleyevents.msse692_backend.dto.response.ApiErrorDto;
 
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
 
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import com.arkvalleyevents.msse692_backend.dto.request.RoleRequestDecisionDto;
import com.arkvalleyevents.msse692_backend.dto.response.RoleRequestDto;
import com.arkvalleyevents.msse692_backend.model.RoleRequestStatus;
import com.arkvalleyevents.msse692_backend.service.RoleRequestService;
import com.arkvalleyevents.msse692_backend.service.UserRoleService;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Users", description = "Admin-only user role management")
public class AdminUserController {

    // No controller-level logs; service layer performs auditing/logging.

    private final RoleRequestService service;
    private final UserRoleService userRoleService;

    public AdminUserController(RoleRequestService service, UserRoleService userRoleService) {
        this.service = service;
        this.userRoleService = userRoleService;
    }

    public record RolesRequest(Set<String> roles) {}
    public record RolesResponse(String firebaseUid, Set<String> roles) {}

    @GetMapping("/{uid}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Get user roles", 
        description = "Returns the roles assigned to the specified user (ADMIN only)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = RolesResponse.class))),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public RolesResponse getRoles(@PathVariable String uid) {
        var view = userRoleService.getRoles(uid);
        return new RolesResponse(view.firebaseUid(), view.roles());
    }

    @PostMapping("/{uid}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Add roles to user", 
        description = "Adds validated roles to a user and triggers claims sync (ADMIN only)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = RolesResponse.class))),
        @ApiResponse(responseCode = "400", description = "Bad Request",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public RolesResponse addRoles(@PathVariable String uid, @RequestBody RolesRequest body) {
        var view = userRoleService.addRoles(uid, body != null ? body.roles() : null);
        return new RolesResponse(view.firebaseUid(), view.roles());
    }

    @DeleteMapping("/{uid}/roles/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Remove role from user", 
        description = "Removes a role from the user (ADMIN only). Returns 200 or 304 if role absent."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "304", description = "Not Modified"),
        @ApiResponse(responseCode = "400", description = "Bad Request",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public ResponseEntity<?> removeRole(@PathVariable String uid, @PathVariable String role) {
        var result = userRoleService.removeRole(uid, role);
        if (result.removed()) {
            return ResponseEntity.ok(Map.of("removed", result.role(), "uid", result.uid()));
        }
        return ResponseEntity.status(HttpStatus.NOT_MODIFIED).body(Map.of("message", "Role not present", "role", result.role()));
    }

    @PostMapping("/{uid}/roles/sync")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Sync user role claims", 
        description = "Triggers Firebase role claims sync for a user (ADMIN only)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Accepted"),
            @ApiResponse(responseCode = "400", description = "Bad Request",
                content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "404", description = "Not Found",
                content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public ResponseEntity<?> syncRolesClaims(@PathVariable String uid, @RequestParam(name = "force", defaultValue = "false") boolean force) {
        var result = userRoleService.syncClaims(uid, force);
        return ResponseEntity.accepted().body(Map.of(
            "uid", result.uid(),
            "message", "Role claims sync triggered",
            "force", result.force()
        ));
    }

    // Admin Requests
    // ----------------------------------------------------------------
    // These endpoints allow admins to manage role requests made by users
    // ----------------------------------------------------------------

    @GetMapping("roles/requests")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "List role requests",
        description = "Admin-only: Lists user role elevation requests with optional status and text search filters. Returns a paginated result."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = RoleRequestDto.class))),
        @ApiResponse(responseCode = "400", description = "Bad Request",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public Page<RoleRequestDto> list(@RequestParam Optional<RoleRequestStatus> status,
                                    @RequestParam Optional<String> search,
                                    Pageable pageable) {
        return service.adminList(status, search, pageable);
    }

    @GetMapping("roles/requests/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Get role request detail",
        description = "Admin-only: Retrieves a specific user role request by ID."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = RoleRequestDto.class))),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public RoleRequestDto detail(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping("roles/requests/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Approve role request",
        description = "Admin-only: Approves a user role request. Applies roles and triggers claims sync in the service layer."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = RoleRequestDto.class))),
        @ApiResponse(responseCode = "400", description = "Bad Request",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public RoleRequestDto approve(@AuthenticationPrincipal Jwt principal,
                                    @PathVariable UUID id,
                                    @RequestBody @Valid RoleRequestDecisionDto body) {
        RoleRequestDto updated = service.approve(id, principal.getSubject(), body);
        return updated;
    }

    @PostMapping("roles/requests/{id}/reject")
    @Operation(
        summary = "Reject role request",
        description = "Admin-only: Rejects a user role request and records the approver's note if provided."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = RoleRequestDto.class))),
        @ApiResponse(responseCode = "400", description = "Bad Request",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public RoleRequestDto reject(@AuthenticationPrincipal Jwt principal,
                                @PathVariable UUID id,
                                @RequestBody @Valid RoleRequestDecisionDto body) {
        return service.reject(id, principal.getSubject(), body);
    }

}
