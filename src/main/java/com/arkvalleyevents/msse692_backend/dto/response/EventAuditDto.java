package com.arkvalleyevents.msse692_backend.dto.response;

import java.time.OffsetDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EventAuditDto {
    private Long id;
    private Long eventId;
    private Long actorUserId;
    private String action; // CREATE, UPDATE, DELETE
    private OffsetDateTime at;
}
