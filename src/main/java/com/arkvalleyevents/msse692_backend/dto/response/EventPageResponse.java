package com.arkvalleyevents.msse692_backend.dto.response;

import java.util.List;
import org.springframework.data.domain.Page;

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

    /** Factory method to build an EventPageResponse from a Spring Page. */
    public static EventPageResponse from(Page<EventDto> page) {
        EventPageResponse out = new EventPageResponse();
        PageMetadata meta = new PageMetadata();
        meta.setNumber(page.getNumber());
        meta.setSize(page.getSize());
        meta.setTotalElements(page.getTotalElements());
        meta.setTotalPages(page.getTotalPages());
        out.setPage(meta);
        out.setItems(page.getContent());
        return out;
    }
}
