package com.arkvalleyevents.msse692_backend.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Standard API error envelope returned by the server for non-2xx responses.
 * Produced by the global RestExceptionHandler so errors are consistent.
 */
public class ApiErrorDto {
    /**
     * Server timestamp when the error was generated (typically UTC).
     */
    private OffsetDateTime timestamp;

    /**
     * HTTP status code (e.g., 400, 404, 409, 500).
     */
    private int status;

    /**
     * HTTP reason phrase associated with the status (e.g., "Bad Request").
     */
    private String error;

    /**
     * Application-specific error code (e.g., "VALIDATION_FAILED", "NOT_FOUND").
     * Useful for programmatic handling on the client.
     */
    private String code;

    /**
     * Human-readable error description safe to show to end users.
     */
    private String message;

    /**
     * The request path that produced the error (e.g., "/api/v1/events/123").
     */
    private String path;

    /**
     * Correlation identifier for tracing (from MDC or the X-Request-ID header).
     */
    private String requestId;

    /**
     * Optional list of field-level issues (present for validation errors).
     */
    private List<FieldIssueDto> details;

    public ApiErrorDto() {}

    public ApiErrorDto(OffsetDateTime timestamp, int status, String error, String code, String message,
                       String path, String requestId, List<FieldIssueDto> details) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.code = code;
        this.message = message;
        this.path = path;
        this.requestId = requestId;
        this.details = details;
    }

    public OffsetDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(OffsetDateTime timestamp) { this.timestamp = timestamp; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public List<FieldIssueDto> getDetails() { return details; }
    public void setDetails(List<FieldIssueDto> details) { this.details = details; }
}
