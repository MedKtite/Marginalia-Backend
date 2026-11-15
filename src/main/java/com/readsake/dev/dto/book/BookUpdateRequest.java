package com.readsake.dev.dto.book;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * PATCH semantics — only fields present in the request body are updated.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookUpdateRequest {

    @Pattern(regexp = "reading|listening|finished|archived")
    private String status;

    @DecimalMin("0.00")
    @DecimalMax("100.00")
    private BigDecimal progressPct;

    /** Raw JSON string stored verbatim in the cursor JSONB column. */
    private String cursor;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getProgressPct() { return progressPct; }
    public void setProgressPct(BigDecimal progressPct) { this.progressPct = progressPct; }

    public String getCursor() { return cursor; }
    public void setCursor(String cursor) { this.cursor = cursor; }
}
