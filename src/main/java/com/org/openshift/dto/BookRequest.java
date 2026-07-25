package com.org.openshift.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Year;

public record BookRequest(
        @NotBlank(message = "title is required") String title,
        @NotBlank(message = "author is required") String author,
        @NotBlank(message = "isbn is required") String isbn,
        @NotNull(message = "price is required") @DecimalMin(value = "0.0", inclusive = true, message = "price must be >= 0") BigDecimal price,
        @Min(value = 1450, message = "publishedYear looks too old") @Max(value = 2100, message = "publishedYear looks too far in the future") Integer publishedYear) {

    public BookRequest {
        if (publishedYear == null) {
            publishedYear = Year.now().getValue();
        }
    }
}
