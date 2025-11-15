package com.arkvalleyevents.msse692_backend.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

public class EnumsControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(new EnumsController()).build();
    }

    @Test
    void getEventStatuses_returnsAllStatusesWithLabels() throws Exception {
        int expected = com.arkvalleyevents.msse692_backend.model.EventStatus.values().length;
        mockMvc.perform(get("/api/v1/enums/event-statuses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(expected)))
                .andExpect(content().string(containsString("\"value\":\"CANCELLED\"")))
                .andExpect(content().string(containsString("\"label\":\"Cancelled\"")));
    }

    @Test
    void getEventTypes_returnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/enums/event-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
