package com.marginalia.dev.services;

import com.marginalia.dev.dto.session.ReadingSessionCreateRequest;
import com.marginalia.dev.entities.ReadingSession;

import java.util.List;
import java.util.UUID;

public interface ReadingSessionService {

    ReadingSession create(ReadingSessionCreateRequest req, UUID userId);

    List<ReadingSession> listRecent(UUID userId, int limit);

    List<ReadingSession> listForBook(UUID bookId, UUID userId);
}
