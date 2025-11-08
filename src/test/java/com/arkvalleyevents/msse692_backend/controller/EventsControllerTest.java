package com.arkvalleyevents.msse692_backend.controller;



import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.arkvalleyevents.msse692_backend.repository.AppUserRepository;
import com.arkvalleyevents.msse692_backend.repository.ProfileRepository;
import com.arkvalleyevents.msse692_backend.service.EventService;
import com.arkvalleyevents.msse692_backend.service.EventAuditService;
import com.arkvalleyevents.msse692_backend.dto.request.CreateEventDto;
import com.arkvalleyevents.msse692_backend.dto.response.EventDetailDto;
import com.arkvalleyevents.msse692_backend.model.EventAudit;
import com.arkvalleyevents.msse692_backend.dto.request.UpdateEventDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;

@WebMvcTest(EventsController.class)
@AutoConfigureMockMvc(addFilters = false)
class EventsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

  // Added mock repositories to satisfy AppUserUpsertFilter constructor in slice context
  @MockitoBean
  private AppUserRepository appUserRepository;
  @MockitoBean
  private ProfileRepository profileRepository;

  @MockitoBean
  private EventAuditService eventAuditService;

    @Test
  void createEvent_returnsCreated_withOwnershipFields() throws Exception {
        EventDetailDto dto = new EventDetailDto();
        dto.setEventId(1L);
        dto.setEventName("Spring Festival");
    dto.setCreatedByUserId(10L);
    dto.setLastModifiedByUserId(10L);

        when(eventService.createEvent(any(CreateEventDto.class))).thenReturn(dto);

  mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("""
                                {
                                  "eventName": "Spring Festival",
                                  "typeDisplayName": "Concert",
                                  "startAt": "2025-10-14T05:09:08.338Z",
                                  "endAt": "2025-10-14T07:00:00.000Z",
                                  "eventLocation": "Downtown",
                                  "eventDescription": "Music & food"
                                }
                                """))
  .andExpect(status().isCreated())
    .andExpect(header().exists("Location"))
    .andExpect(jsonPath("$.eventName").value("Spring Festival"))
    .andExpect(jsonPath("$.createdByUserId").value(10L))
    .andExpect(jsonPath("$.lastModifiedByUserId").value(10L));
    }

    @Test
    void updateEvent_returnsUpdated_withModifiedOwnership() throws Exception {
  EventDetailDto dto = new EventDetailDto();
  dto.setEventId(2L);
  dto.setEventName("Updated Event");
  dto.setCreatedByUserId(10L); // original creator remains
  dto.setLastModifiedByUserId(11L); // modified by another user

  when(eventService.updateEvent(eq(2L), any(UpdateEventDto.class))).thenReturn(dto);

  mockMvc.perform(put("/api/events/2").contentType(MediaType.APPLICATION_JSON_VALUE)
      .content("""
        {
          "eventName": "Updated Event",
          "typeDisplayName": "Concert",
          "startAt": "2025-10-14T05:09:08.338Z",
          "endAt": "2025-10-14T07:00:00.000Z",
          "eventLocation": "Downtown",
          "eventDescription": "Music & food"
        }
        """))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.eventName").value("Updated Event"))
    .andExpect(jsonPath("$.createdByUserId").value(10L))
    .andExpect(jsonPath("$.lastModifiedByUserId").value(11L));
    }

  @Test
  void getEventAudits_returnsRecentActionsOrderedAndLimited() throws Exception {
    EventAudit older = new EventAudit();
    older.setId(1L);
    older.setEventId(99L);
    older.setActorUserId(7L);
    older.setAction("CREATE");
    older.setAt(java.time.OffsetDateTime.now().minusMinutes(5));

    EventAudit newer = new EventAudit();
    newer.setId(2L);
    newer.setEventId(99L);
    newer.setActorUserId(8L);
    newer.setAction("UPDATE");
    newer.setAt(java.time.OffsetDateTime.now());

    when(eventAuditService.getRecentForEvent(eq(99L), anyInt()))
        .thenReturn(java.util.List.of(newer, older));

    mockMvc.perform(get("/api/events/99/audits").param("limit", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(2L))
        .andExpect(jsonPath("$[0].action").value("UPDATE"))
        .andExpect(jsonPath("$[0].actorUserId").value(8L))
        .andExpect(jsonPath("$[1].id").value(1L))
        .andExpect(jsonPath("$[1].action").value("CREATE"))
        .andExpect(jsonPath("$[1].actorUserId").value(7L));
  }
}
