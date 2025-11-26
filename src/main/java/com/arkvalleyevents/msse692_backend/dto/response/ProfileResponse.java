package com.arkvalleyevents.msse692_backend.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

import com.arkvalleyevents.msse692_backend.model.ProfileType;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.Getter;
@Data
@Getter
public class ProfileResponse {
	private Long id;
	@JsonProperty("user_id")
	private Long userId; // Maps to profiles.user_id FK (AppUser.id)
	private String displayName;
	private boolean completed;
	private boolean verified;
	private OffsetDateTime createdAt;
	private OffsetDateTime updatedAt;

	//Added fields
	private ProfileType profileType;
    private String location;
    private String description;
    private List<String> socials;
    private List<String> websites;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	public Long getUserId() { return userId; }
	public void setUserId(Long userId) { this.userId = userId; }

	public String getDisplayName() { return displayName; }
	public void setDisplayName(String displayName) { this.displayName = displayName; }

	public boolean isCompleted() { return completed; }
	public void setCompleted(boolean completed) { this.completed = completed; }

	public boolean isVerified() { return verified; }
	public void setVerified(boolean verified) { this.verified = verified; }

	public OffsetDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

	public OffsetDateTime getUpdatedAt() { return updatedAt; }
	public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
