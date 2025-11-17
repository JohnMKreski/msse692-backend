package com.arkvalleyevents.msse692_backend.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class RoleRequestCreateDto {

    @NotEmpty
    private List<String> requestedRoles; // validated server-side to allowed set (e.g., EDITOR only)

    @Size(max = 500)
    private String reason;
}
