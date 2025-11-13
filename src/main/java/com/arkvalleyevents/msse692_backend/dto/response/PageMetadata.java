package com.arkvalleyevents.msse692_backend.dto.response;

/**
 * Pagination metadata describing the current page and totals.
 */
public class PageMetadata {
    private int number;
    private int size;
    private long totalElements;
    private int totalPages;

    public int getNumber() { return number; }
    public void setNumber(int number) { this.number = number; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
}
