package com.emenu.features.resource.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class StaleTrackerRequest {

    /** Filter by application; null means search across all applications. */
    private String applicationName;

    /** Return resourceIds not used since before this date (yyyy-MM-dd). */
    @NotNull(message = "before date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate before;
}
