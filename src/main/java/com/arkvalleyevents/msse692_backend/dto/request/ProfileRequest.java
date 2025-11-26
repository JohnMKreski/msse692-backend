package com.arkvalleyevents.msse692_backend.dto.request;

import java.util.List;

import com.arkvalleyevents.msse692_backend.model.ProfileType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;

/**
 * Incoming profile upsert request (simplified).
 * Fields kept minimal: displayName only for now.
 */
@Getter
@Data
public class ProfileRequest {

	@NotBlank
	@Size(max = 200)
	private String displayName;

	@NotNull
    private ProfileType profileType;

	private String location; // required if profileType == VENUE (validated in service/controller)
    private String description;
    private List<String> socials;
    private List<String> websites;
}
