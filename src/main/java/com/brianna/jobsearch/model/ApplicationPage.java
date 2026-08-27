package com.brianna.jobsearch.model;

import java.util.ArrayList;
import java.util.List;

public class ApplicationPage {

    private final List<JobApplication> items;
    private final long totalItems;
    private final int page;
    private final int size;
    private final int totalPages;

    public ApplicationPage(List<JobApplication> items, long totalItems, int page, int size) {
        this.items = items;
        this.totalItems = totalItems;
        this.page = Math.max(0, page);
        this.size = size;
        this.totalPages = totalItems == 0 ? 0 : (int) Math.ceil(totalItems / (double) size);
    }

    public List<Integer> getVisiblePages() {
        if (totalPages <= 0) {
            return List.of();
        }
        int start = Math.max(0, page - 2);
        int end = Math.min(totalPages - 1, start + 4);
        start = Math.max(0, end - 4);
        List<Integer> pages = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            pages.add(i);
        }
        return pages;
    }

    public List<JobApplication> getItems() { return items; }
    public long getTotalItems() { return totalItems; }
    public int getPage() { return page; }
    public int getPageNumber() { return page + 1; }
    public int getSize() { return size; }
    public int getTotalPages() { return totalPages; }
    public boolean isHasPrevious() { return page > 0; }
    public boolean isHasNext() { return page + 1 < totalPages; }
    public int getFirstItem() { return totalItems == 0 ? 0 : page * size + 1; }
    public long getLastItem() { return Math.min(totalItems, (long) (page + 1) * size); }
}
