package com.jklu.campusflow.common.dto;

import java.util.List;

public record PaginatedResponse<T>(
        List<T> items,
        int page,
        int pageSize,
        long total,
        int totalPages
) {}
