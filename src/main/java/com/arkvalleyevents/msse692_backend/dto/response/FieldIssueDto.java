package com.arkvalleyevents.msse692_backend.dto.response;

/**
 * A single field-level issue included in an API error response.
 * Used primarily for validation errors to identify which input caused a problem.
 */
public class FieldIssueDto {
    /**
     * The name of the field or parameter that failed validation.
     * Examples: "displayName", "page", or a nested path like "address.city".
     */
    private String field;

    /**
     * A human-readable message describing the issue for this field.
     * Typically comes from Bean Validation or type conversion messages.
     */
    private String message;

    /**
     * The value that was rejected by validation or conversion.
     * May be null if unavailable or unsafe to echo back.
     */
    private Object rejectedValue;

    public FieldIssueDto() {}

    public FieldIssueDto(String field, String message, Object rejectedValue) {
        this.field = field;
        this.message = message;
        this.rejectedValue = rejectedValue;
    }

    public String getField() { return field; }
    public void setField(String field) { this.field = field; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Object getRejectedValue() { return rejectedValue; }
    public void setRejectedValue(Object rejectedValue) { this.rejectedValue = rejectedValue; }
}
