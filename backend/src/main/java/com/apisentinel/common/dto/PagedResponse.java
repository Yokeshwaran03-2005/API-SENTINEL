package com.apisentinel.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Generic pagination wrapper for API Sentinel list responses.
 *
 * @param <T> DTO item type
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean isFirst,
        boolean isLast
) {
    public static <T> PagedResponse<T> fromPage(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
