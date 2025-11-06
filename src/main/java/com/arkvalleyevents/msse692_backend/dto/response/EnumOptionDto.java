/**
 * DTO representing an enum option for UI selection.
 * value: enum constant; label: display text.
 */
package com.arkvalleyevents.msse692_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EnumOptionDto {
    private String value;
    private String label;
}
