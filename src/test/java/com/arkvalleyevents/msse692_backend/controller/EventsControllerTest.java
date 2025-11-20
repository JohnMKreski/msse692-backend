package com.arkvalleyevents.msse692_backend.controller;



import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.arkvalleyevents.msse692_backend.service.EventService;
import com.arkvalleyevents.msse692_backend.service.EventAuditService;
import com.arkvalleyevents.msse692_backend.dto.request.CreateEventDto;
import com.arkvalleyevents.msse692_backend.dto.response.EventDetailDto;
import com.arkvalleyevents.msse692_backend.dto.response.EventDto;
import com.arkvalleyevents.msse692_backend.model.EventStatus;
import com.arkvalleyevents.msse692_backend.model.EventAudit;
import com.arkvalleyevents.msse692_backend.dto.request.UpdateEventDto;
import com.arkvalleyevents.msse692_backend.security.policy.EventAccessPolicy;
import com.arkvalleyevents.msse692_backend.security.context.UserContextProvider;
import com.arkvalleyevents.msse692_backend.security.context.UserContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.BeforeEach;

class EventsControllerTest {

  @Mock
  private EventService eventService;
  @Mock
  private EventAuditService eventAuditService;
  @Mock
  private UserContextProvider userContextProvider;

  private MockMvc mockMvc;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    EventAccessPolicy policy = new EventAccessPolicy();
    when(userContextProvider.current()).thenReturn(new UserContext(10L, true, false));
    EventsController controller = new EventsController(eventService, eventAuditService, policy, userContextProvider);
    mockMvc = MockMvcBuilders.standaloneSetup(controller)
        .setControllerAdvice(new RestExceptionHandler())
        .build();
  }

    @Test
  void createEvent_returnsCreated_withOwnershipFields() throws Exception {
        EventDetailDto dto = new EventDetailDto();
        dto.setEventId(1L);
        dto.setEventName("Spring Festival");
    dto.setCreatedByUserId(10L);
    dto.setLastModifiedByUserId(10L);

        when(eventService.createEvent(any(CreateEventDto.class))).thenReturn(dto);

  mockMvc.perform(post("/api/v1/events").contentType(MediaType.APPLICATION_JSON_VALUE)
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
    EventDetailDto existing = new EventDetailDto();
    existing.setEventId(2L);
    existing.setCreatedByUserId(10L);
    when(eventService.getEventDetailOrThrow(2L)).thenReturn(existing);
  when(eventService.updateEvent(eq(2L), any(UpdateEventDto.class))).thenReturn(dto);

  mockMvc.perform(put("/api/v1/events/2").contentType(MediaType.APPLICATION_JSON_VALUE)
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
    EventDetailDto existing = new EventDetailDto();
    existing.setEventId(99L);
    existing.setCreatedByUserId(10L);
    when(eventService.getEventDetailOrThrow(99L)).thenReturn(existing);
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

  mockMvc.perform(get("/api/v1/events/99/audits").param("limit", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(2L))
        .andExpect(jsonPath("$[0].action").value("UPDATE"))
        .andExpect(jsonPath("$[0].actorUserId").value(8L))
        .andExpect(jsonPath("$[1].id").value(1L))
        .andExpect(jsonPath("$[1].action").value("CREATE"))
        .andExpect(jsonPath("$[1].actorUserId").value(7L));
  }

  // ===== New status transition & public feed tests =====

  @Test
  void publishEvent_returnsPublishedStatus() throws Exception {
    EventDetailDto existing = new EventDetailDto();
    existing.setEventId(50L);
    existing.setCreatedByUserId(10L);
    when(eventService.getEventDetailOrThrow(50L)).thenReturn(existing);
    EventDetailDto dto = new EventDetailDto();
    dto.setEventId(50L);
    dto.setStatus(EventStatus.PUBLISHED);
    when(eventService.publishEvent(50L)).thenReturn(dto);

    mockMvc.perform(post("/api/v1/events/50/publish"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.eventId").value(50L))
        .andExpect(jsonPath("$.status").value("PUBLISHED"));
  }

  @Test
  void publishEvent_illegalState_conflict() throws Exception {
    EventDetailDto existing = new EventDetailDto();
    existing.setEventId(51L);
    existing.setCreatedByUserId(10L);
    when(eventService.getEventDetailOrThrow(51L)).thenReturn(existing);
    when(eventService.publishEvent(51L)).thenThrow(new IllegalStateException("Only DRAFT events can be published"));

    mockMvc.perform(post("/api/v1/events/51/publish"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ILLEGAL_STATE"));
  }

  @Test
  void unpublishEvent_returnsUnpublishedStatus() throws Exception {
    EventDetailDto existing = new EventDetailDto();
    existing.setEventId(60L);
    existing.setCreatedByUserId(10L);
    when(eventService.getEventDetailOrThrow(60L)).thenReturn(existing);
    EventDetailDto dto = new EventDetailDto();
    dto.setEventId(60L);
    dto.setStatus(EventStatus.UNPUBLISHED);
    when(eventService.unpublishEvent(60L)).thenReturn(dto);

    mockMvc.perform(post("/api/v1/events/60/unpublish"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UNPUBLISHED"));
  }

  @Test
  void cancelEvent_returnsCancelledStatus() throws Exception {
    EventDetailDto existing = new EventDetailDto();
    existing.setEventId(70L);
    existing.setCreatedByUserId(10L);
    when(eventService.getEventDetailOrThrow(70L)).thenReturn(existing);
    EventDetailDto dto = new EventDetailDto();
    dto.setEventId(70L);
    dto.setStatus(EventStatus.CANCELLED);
    when(eventService.cancelEvent(70L)).thenReturn(dto);

    mockMvc.perform(post("/api/v1/events/70/cancel"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));
  }

  @Test
  void listPublicUpcoming_returnsPublishedEvents() throws Exception {
    EventDto e = new EventDto();
    e.setEventId(80L);
    e.setStatus(EventStatus.PUBLISHED);
    when(eventService.listPublicUpcoming(any(), eq(3))).thenReturn(java.util.List.of(e));

    mockMvc.perform(get("/api/v1/events/public-upcoming?limit=3"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].eventId").value(80L));
  }

  @Test
  void listEvents_delegatesToScopedService_andReturnsPage() throws Exception {
    EventDto dto = new EventDto();
    dto.setEventId(123L);
    Page<EventDto> page = new PageImpl<>(java.util.List.of(dto));
    when(eventService.listEventsPageScoped(anyMap(), anyInt(), anyInt(), anyString(), any(UserContext.class)))
        .thenReturn(page);

    mockMvc.perform(get("/api/v1/events")
            .param("page", "1")
            .param("size", "2")
            .param("sort", "-startAt")
            .param("type", "Concert"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].eventId").value(123L));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<java.util.Map<String,String>> capFilters = (ArgumentCaptor<java.util.Map<String,String>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(java.util.Map.class);
    verify(eventService).listEventsPageScoped(capFilters.capture(), eq(1), eq(2), eq("-startAt"), any(UserContext.class));
    java.util.Map<String,String> filters = capFilters.getValue();
    org.junit.jupiter.api.Assertions.assertEquals("Concert", filters.get("type"));
    org.junit.jupiter.api.Assertions.assertFalse(filters.containsKey("page"));
    org.junit.jupiter.api.Assertions.assertFalse(filters.containsKey("size"));
    org.junit.jupiter.api.Assertions.assertFalse(filters.containsKey("sort"));
  }

  @Test
  void listEvents_unsupportedSort_returns400InvalidArgument() throws Exception {
    mockMvc.perform(get("/api/v1/events")
            .param("sort", "badField"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
    verify(eventService, never()).listEventsPageScoped(anyMap(), anyInt(), anyInt(), anyString(), any(UserContext.class));
  }

  @Test
  void listPublicUpcoming_limitTooLow_returns400ConstraintViolation() throws Exception {
    mockMvc.perform(get("/api/v1/events/public-upcoming").param("limit", "0"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("CONSTRAINT_VIOLATION"));
    verify(eventService, never()).listPublicUpcoming(any(), anyInt());
  }

  @Test
  void listMyEvents_delegatesToOwnerService_andReturnsPage() throws Exception {
    EventDto dto = new EventDto();
    dto.setEventId(777L);
    Page<EventDto> page = new PageImpl<>(java.util.List.of(dto));
    when(eventService.listEventsByOwner(eq(10L), eq(0), eq(20), eq("-startAt"))).thenReturn(page);

    mockMvc.perform(get("/api/v1/events/mine").param("sort", "-startAt"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].eventId").value(777L));

    verify(eventService).listEventsByOwner(eq(10L), eq(0), eq(20), eq("-startAt"));
  }

  @Test
  void listMyEvents_missingUserId_accessDenied() throws Exception {
    when(userContextProvider.current()).thenReturn(new UserContext(null, true, false));
    mockMvc.perform(get("/api/v1/events/mine"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
  }
}
