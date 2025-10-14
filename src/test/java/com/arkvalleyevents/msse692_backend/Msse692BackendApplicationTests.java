package com.arkvalleyevents.msse692_backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import com.arkvalleyevents.msse692_backend.repository.EventRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class Msse692BackendApplicationTests {

  @MockitoBean
  private EventRepository eventRepository; // replaces the bean in the ApplicationContext

  @Test
  void contextLoads() {}
}
