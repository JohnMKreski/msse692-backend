package com.arkvalleyevents.msse692_backend.service.mapping;

import com.arkvalleyevents.msse692_backend.dto.request.ProfileRequest;
import com.arkvalleyevents.msse692_backend.dto.response.ProfileResponse;
import com.arkvalleyevents.msse692_backend.model.Profile;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ProfileMapper {

    // -------- Create (DTO -> Entity) --------
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "completed", ignore = true)
    @Mapping(target = "verified", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    // no optimistic version column on Profile
    @Mapping(target = "socials", expression = "java(listToJson(src.getSocials()))")
    @Mapping(target = "websites", expression = "java(listToJson(src.getWebsites()))")
    Profile toEntity(ProfileRequest src);

    // -------- Update (partial; ignores nulls) --------
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "completed", ignore = true)
    @Mapping(target = "verified", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    // no optimistic version column on Profile
    // socials/websites handled manually in @AfterMapping to honor null-ignore behavior
    @Mapping(target = "socials", ignore = true)
    @Mapping(target = "websites", ignore = true)
    void updateEntity(@MappingTarget Profile target, ProfileRequest src);

    // -------- Entity -> DTO --------
        @Mappings({
            @Mapping(target = "socials", expression = "java(jsonToList(src.getSocials()))"),
            @Mapping(target = "websites", expression = "java(jsonToList(src.getWebsites()))"),
            @Mapping(target = "userId", expression = "java(src.getUser() != null ? src.getUser().getId() : null)" )
        })
    ProfileResponse toResponse(Profile src);

    // ===== Helpers for JSON array string <-> List<String> =====
    default String listToJson(List<String> values) {
        if (values == null) return null; // respect nulls in partial update
        // Store as JSON array string, e.g. ["a","b"]
        // Use a minimal implementation to avoid adding dependencies here.
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        boolean first = true;
        for (String v : values) {
            if (!first) sb.append(',');
            first = false;
            if (v == null) {
                sb.append("null");
            } else {
                sb.append('"').append(escapeJson(v)).append('"');
            }
        }
        sb.append(']');
        return sb.toString();
    }

    @AfterMapping
    default void afterUpdate(@MappingTarget Profile target, ProfileRequest src) {
        if (src.getSocials() != null) {
            target.setSocials(listToJson(src.getSocials()));
        }
        if (src.getWebsites() != null) {
            target.setWebsites(listToJson(src.getWebsites()));
        }
    }

    default List<String> jsonToList(String json) {
        if (json == null || json.isBlank()) return null;
        // Very small parser for ["..."] or [] to avoid ObjectMapper here.
        // Assumes simple arrays of strings; ignore whitespace.
        String trimmed = json.trim();
        if (trimmed.equals("[]")) return java.util.List.of();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) return java.util.List.of();
        trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        if (trimmed.isEmpty()) return java.util.List.of();
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        int i = 0;
        while (i < trimmed.length()) {
            char c = trimmed.charAt(i);
            if (c == '"') {
                int j = i + 1;
                StringBuilder sb = new StringBuilder();
                while (j < trimmed.length()) {
                    char cj = trimmed.charAt(j);
                    if (cj == '\\') {
                        if (j + 1 < trimmed.length()) {
                            char next = trimmed.charAt(j + 1);
                            if (next == '"' || next == '\\' || next == '/') { sb.append(next); j += 2; continue; }
                            if (next == 'b') { sb.append('\b'); j += 2; continue; }
                            if (next == 'f') { sb.append('\f'); j += 2; continue; }
                            if (next == 'n') { sb.append('\n'); j += 2; continue; }
                            if (next == 'r') { sb.append('\r'); j += 2; continue; }
                            if (next == 't') { sb.append('\t'); j += 2; continue; }
                        }
                    }
                    if (cj == '"') { break; }
                    sb.append(cj);
                    j++;
                }
                out.add(sb.toString());
                // move past closing quote
                i = j + 1;
                // skip comma and whitespace
                while (i < trimmed.length() && (trimmed.charAt(i) == ',' || Character.isWhitespace(trimmed.charAt(i)))) i++;
            } else if (c == 'n') { // null
                // consume "null"
                if (trimmed.startsWith("null", i)) {
                    out.add(null);
                    i += 4;
                    while (i < trimmed.length() && (trimmed.charAt(i) == ',' || Character.isWhitespace(trimmed.charAt(i)))) i++;
                } else {
                    i++; // fallback
                }
            } else {
                i++; // skip unexpected
            }
        }
        return out;
    }

    private static String escapeJson(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        // control chars as unicode
                        sb.append(String.format("\\u%04x", (int)c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
