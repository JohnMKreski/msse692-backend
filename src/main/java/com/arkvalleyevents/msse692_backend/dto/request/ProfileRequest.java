package com.arkvalleyevents.msse692_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Incoming profile upsert request (simplified).
 * Fields kept minimal: displayName only for now.
 */
public class ProfileRequest {

	@NotBlank
	@Size(max = 200)
	private String displayName;

	public String getDisplayName() { return displayName; }
	public void setDisplayName(String displayName) { this.displayName = displayName; }
}
