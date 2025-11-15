package com.arkvalleyevents.msse692_backend.service.impl;

import com.arkvalleyevents.msse692_backend.dto.response.EventDto;
import com.arkvalleyevents.msse692_backend.model.Event;
import com.arkvalleyevents.msse692_backend.repository.EventRepository;
import com.arkvalleyevents.msse692_backend.security.context.UserContext;
import com.arkvalleyevents.msse692_backend.security.policy.EventListPolicy;
import com.arkvalleyevents.msse692_backend.service.EventAuditService;
import com.arkvalleyevents.msse692_backend.service.mapping.EventMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EventServiceImplListScopedTest {

    @Mock private EventRepository eventRepository;
    @Mock private EventMapper eventMapper;
    @Mock private EventAuditService eventAuditService;

    private EventListPolicy eventListPolicy;
    private EventServiceImpl service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        eventListPolicy = new EventListPolicy();
        service = new EventServiceImpl(eventRepository, eventMapper, eventAuditService, eventListPolicy);
    }

    @Test
    void adminWithEmptyFilters_usesPageableOnly() {
        when(eventRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        UserContext admin = new UserContext(1L, true, false);
        Page<EventDto> result = service.listEventsPageScoped(Map.of(), 0, 10, "startAt,asc", admin);

        assertEquals(0, result.getTotalElements());
        verify(eventRepository, times(1)).findAll(any(Pageable.class));
        verify(eventRepository, never()).findAll(ArgumentMatchers.<Specification<Event>>any(), any(Pageable.class));
    }

    @Test
    void editorWithEmptyFilters_usesSpecification() {
        Event e = new Event();
        when(eventRepository.findAll(ArgumentMatchers.<Specification<Event>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(e)));
        when(eventMapper.toDto(e)).thenReturn(new EventDto());

        UserContext editor = new UserContext(10L, false, true);
        Page<EventDto> result = service.listEventsPageScoped(Map.of(), 0, 10, "startAt,asc", editor);

        assertEquals(1, result.getTotalElements());
        verify(eventRepository, times(1)).findAll(ArgumentMatchers.<Specification<Event>>any(), any(Pageable.class));
        verify(eventRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void publicUser_defaultsToPublished_usesSpecification() {
        when(eventRepository.findAll(ArgumentMatchers.<Specification<Event>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        UserContext user = new UserContext(null, false, false);
        Page<EventDto> result = service.listEventsPageScoped(Map.of(), 0, 5, "startAt,asc", user);

        assertEquals(0, result.getTotalElements());
        verify(eventRepository, times(1)).findAll(ArgumentMatchers.<Specification<Event>>any(), any(Pageable.class));
        verify(eventRepository, never()).findAll(any(Pageable.class));
    }
}
