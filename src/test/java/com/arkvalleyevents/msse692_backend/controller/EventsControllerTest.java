package com.arkvalleyevents.msse692_backend.controller;


import static org.junit.jupiter.api.Assertions.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.arkvalleyevents.msse692_backend.controller.EventsController;
import com.arkvalleyevents.msse692_backend.service.EventService;
import com.arkvalleyevents.msse692_backend.dto.request.CreateEventDto;
import com.arkvalleyevents.msse692_backend.dto.response.EventDetailDto;
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

    @Test
    void createEvent_returnsCreated() throws Exception {
        EventDetailDto dto = new EventDetailDto();
        dto.setEventId(1L);
        dto.setEventName("Spring Festival");

        when(eventService.createEvent(any(CreateEventDto.class))).thenReturn(dto);

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
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
                .andExpect(jsonPath("$.eventName").value("Spring Festival"));
    }
}
