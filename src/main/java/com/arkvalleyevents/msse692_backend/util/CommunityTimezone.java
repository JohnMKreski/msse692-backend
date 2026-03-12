package com.arkvalleyevents.msse692_backend.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;
import java.util.List;

/**
 * Community timezone helpers.
 *
 * Goal: treat all event "wall clock" times as America/Denver (DST-aware), regardless of
 * where the user or server is located.
 *
 * Key idea:
 * - {@link LocalDateTime} has NO timezone/offset information.
 * - To persist or compare times safely, convert to an {@link Instant} (an absolute moment).
 * - During DST transitions, some local times are invalid (gap) or ambiguous (overlap), so
 *   conversion must be explicit and consistent.
 */
public final class CommunityTimezone {

    // Single source of truth for "community time".
    // NOTE: America/Denver observes DST (MDT in summer, MST in winter).
    public static final ZoneId COMMUNITY_ZONE = ZoneId.of("America/Denver");

    private CommunityTimezone() {}

    /**
     * Converts a Denver-local "wall clock" timestamp into an absolute {@link Instant}.
     *
     * DST behavior:
     * - Gap (spring-forward): throw an error (the local time never occurs).
     * - Overlap (fall-back): choose the EARLIER offset deterministically.
     */
    public static Instant toInstant(LocalDateTime denverLocal) {
        // Null-safe for MapStruct / optional fields.
        if (denverLocal == null) return null;

        // ZoneRules contains the complete history of offset/DST rules for America/Denver.
        ZoneRules rules = COMMUNITY_ZONE.getRules();
        List<ZoneOffset> validOffsets = rules.getValidOffsets(denverLocal);

        // The number of valid offsets tells us what kind of local time this is:
        // - size == 1 : normal (unambiguous)
        // - size == 0 : invalid (DST "gap" during spring-forward)
        // - size == 2 : ambiguous (DST "overlap" during fall-back)
        if (validOffsets.size() == 1) {
            // Normal case: one offset -> one real instant.
            return denverLocal.atOffset(validOffsets.get(0)).toInstant();
        }

        if (validOffsets.isEmpty()) {
            // Gap case: this local time does not exist.
            // transition.getDateTimeAfter() is the first valid local time after the jump.
            ZoneOffsetTransition transition = rules.getTransition(denverLocal);
            String hint = transition == null ? "" : (" Try " + transition.getDateTimeAfter() + " instead.");
            throw new IllegalArgumentException(
                "Invalid local time in America/Denver (spring-forward gap): " + denverLocal + "." + hint
            );
        }

        // Overlap (fall-back): the local time occurs twice.
        // Policy decision: choose the EARLIER offset deterministically.
        // (If you ever need users to specify which one they meant, the API must accept an offset.)
        ZoneOffset chosen = validOffsets.get(0); // earlier offset
        return denverLocal.atOffset(chosen).toInstant();
    }

    /**
     * Converts an {@link Instant} (stored time) into an {@link OffsetDateTime} in America/Denver.
     *
     * This is ideal for API responses because it includes an explicit offset, e.g.:
     * - 2026-07-10T19:00:00-06:00 (MDT)
     * - 2026-12-10T19:00:00-07:00 (MST)
     */
    public static OffsetDateTime toDenverOffset(Instant instant) {
        if (instant == null) return null;
        return instant.atZone(COMMUNITY_ZONE).toOffsetDateTime();
    }
}