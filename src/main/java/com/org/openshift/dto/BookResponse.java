package com.org.openshift.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record BookResponse(
        Long id,
        String title,
        String author,
        String isbn,
        BigDecimal price,
        Integer publishedYear,
        Instant createdAt,
        Instant updatedAt) {
}
