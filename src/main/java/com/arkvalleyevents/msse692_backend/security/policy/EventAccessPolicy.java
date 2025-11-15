package com.arkvalleyevents.msse692_backend.security.policy;

import com.arkvalleyevents.msse692_backend.dto.response.EventDetailDto;
import com.arkvalleyevents.msse692_backend.model.EventStatus;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

/**
 * Policy class for event access control (visibility rules).
 * Keeps controllers/services thin and centralizes authorization decisions.
 */
@Component
public class EventAccessPolicy {
	private static final Logger log = LoggerFactory.getLogger(EventAccessPolicy.class);

	/**
	 * Returns true if the user is allowed to view the given event detail.
	 * Rules:
	 * - ADMIN: can view all
	 * - EDITOR: can view events they created
	 * - Anonymous/USER: only PUBLISHED
	 */
	public boolean canView(EventDetailDto dto, Optional<Long> userId, boolean isAdmin, boolean isEditor) {
		if (dto == null) {
			log.debug("canView: dto=null → result=false");
			return false;
		}
		Long uid = userId.orElse(null);
		if (isAdmin) {
			log.debug("canView: admin=true userId={} eventId={} → result=true", uid, dto.getEventId());
			return true;
		}
		if (isEditor && uid != null && Objects.equals(uid, dto.getCreatedByUserId())) {
			log.debug("canView: editor owner match userId={} ownerId={} eventId={} → result=true", uid, dto.getCreatedByUserId(), dto.getEventId());
			return true;
		}
		boolean allowed = dto.getStatus() == EventStatus.PUBLISHED;
		log.debug("canView: public check status={} userId={} eventId={} → result={}", dto.getStatus(), uid, dto.getEventId(), allowed);
		return allowed;
	}

	/**
	 * Asserts that the user can view the event; throws EntityNotFoundException to "hide" unauthorized.
	 */
	public void assertCanView(EventDetailDto dto, Optional<Long> userId, boolean isAdmin, boolean isEditor) {
		if (!canView(dto, userId, isAdmin, isEditor)) {
			Long uid = userId.orElse(null);
			Long ownerId = dto == null ? null : dto.getCreatedByUserId();
			Long eventId = dto == null ? null : dto.getEventId();
			EventStatus status = dto == null ? null : dto.getStatus();
			log.debug("assertCanView: deny userId={} admin={} editor={} ownerId={} status={} eventId={} → throwing 404",
					uid, isAdmin, isEditor, ownerId, status, eventId);
			// Hide unauthorized access as 404 per product policy
			throw new EntityNotFoundException("Event not found: " + eventId);
		}
	}

	/**
	 * Asserts that the user can modify the event; only ADMIN or EDITOR who owns the event.
	 * Denies as 404 to avoid leaking existence.
	 */
	public void assertCanModify(EventDetailDto dto, Optional<Long> userId, boolean isAdmin, boolean isEditor) {
		Long uid = userId.orElse(null);
		if (dto == null) {
			log.debug("assertCanModify: dto=null userId={} admin={} editor={} → throwing 404", uid, isAdmin, isEditor);
			throw new EntityNotFoundException("Event not found: null");
		}
		if (isAdmin) {
			log.debug("assertCanModify: admin=true userId={} eventId={} → allowed", uid, dto.getEventId());
			return;
		}
		boolean ownerEditor = isEditor && uid != null && java.util.Objects.equals(uid, dto.getCreatedByUserId());
		if (ownerEditor) {
			log.debug("assertCanModify: editor owner match userId={} ownerId={} eventId={} → allowed", uid, dto.getCreatedByUserId(), dto.getEventId());
			return;
		}
		log.debug("assertCanModify: deny userId={} admin={} editor={} ownerId={} eventId={} → throwing 404", uid, isAdmin, isEditor, dto.getCreatedByUserId(), dto.getEventId());
		throw new EntityNotFoundException("Event not found: " + dto.getEventId());
	}
}
