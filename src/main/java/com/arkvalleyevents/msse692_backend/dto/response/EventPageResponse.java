package com.arkvalleyevents.msse692_backend.dto.response;

import java.util.List;

/**
 * Page-wrapped response for events: items plus pagination metadata.
 */
public class EventPageResponse {
    private List<EventDto> items;
    private PageMetadata page;

    public List<EventDto> getItems() { return items; }
    public void setItems(List<EventDto> items) { this.items = items; }

    public PageMetadata getPage() { return page; }
    public void setPage(PageMetadata page) { this.page = page; }
}
