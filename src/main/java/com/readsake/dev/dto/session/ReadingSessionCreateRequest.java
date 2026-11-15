package com.readsake.dev.dto.session;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ReadingSessionCreateRequest {

    @NotNull
    private UUID bookId;

    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private OffsetDateTime startedAt;

    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private OffsetDateTime endedAt;

    /** Optional — useful for the 5-device Pro cap and per-device analytics. */
    private UUID deviceId;

    @DecimalMin("0.00")
    @DecimalMax("100.00")
    private BigDecimal startProgressPct;

    @DecimalMin("0.00")
    @DecimalMax("100.00")
    private BigDecimal endProgressPct;

    public UUID getBookId() { return bookId; }
    public void setBookId(UUID bookId) { this.bookId = bookId; }

    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }

    public OffsetDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(OffsetDateTime endedAt) { this.endedAt = endedAt; }

    public UUID getDeviceId() { return deviceId; }
    public void setDeviceId(UUID deviceId) { this.deviceId = deviceId; }

    public BigDecimal getStartProgressPct() { return startProgressPct; }
    public void setStartProgressPct(BigDecimal startProgressPct) { this.startProgressPct = startProgressPct; }

    public BigDecimal getEndProgressPct() { return endProgressPct; }
    public void setEndProgressPct(BigDecimal endProgressPct) { this.endProgressPct = endProgressPct; }
}
