package com.arkvalleyevents.msse692_backend.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RoleRequestDecisionDto {

    @Size(max = 500)
    private String approverNote;
}
