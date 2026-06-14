package com.marginalia.dev.dto.session;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.marginalia.dev.entities.ReadingSession;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReadingSessionResponse {

    private UUID id;
    private UUID bookId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime startedAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime endedAt;

    private Integer durationSec;
    private UUID deviceId;
    private BigDecimal startProgressPct;
    private BigDecimal endProgressPct;

    public static ReadingSessionResponse from(ReadingSession s) {
        ReadingSessionResponse r = new ReadingSessionResponse();
        r.id                = s.getId();
        r.bookId            = s.getBookId();
        r.startedAt         = s.getStartedAt();
        r.endedAt           = s.getEndedAt();
        r.durationSec       = s.getDurationSec();
        r.deviceId          = s.getDeviceId();
        r.startProgressPct  = s.getStartProgressPct();
        r.endProgressPct    = s.getEndProgressPct();
        return r;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getBookId() { return bookId; }
    public void setBookId(UUID bookId) { this.bookId = bookId; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }
    public OffsetDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(OffsetDateTime endedAt) { this.endedAt = endedAt; }
    public Integer getDurationSec() { return durationSec; }
    public void setDurationSec(Integer durationSec) { this.durationSec = durationSec; }
    public UUID getDeviceId() { return deviceId; }
    public void setDeviceId(UUID deviceId) { this.deviceId = deviceId; }
    public BigDecimal getStartProgressPct() { return startProgressPct; }
    public void setStartProgressPct(BigDecimal startProgressPct) { this.startProgressPct = startProgressPct; }
    public BigDecimal getEndProgressPct() { return endProgressPct; }
    public void setEndProgressPct(BigDecimal endProgressPct) { this.endProgressPct = endProgressPct; }
}
