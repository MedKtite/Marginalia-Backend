package com.marginalia.dev.repo.dao;

import com.marginalia.dev.entities.ReadingSession;

import java.util.List;
import java.util.UUID;

public interface ReadingSessionDao {

    ReadingSession insert(ReadingSession s);

    List<ReadingSession> findRecentForUser(UUID userId, int limit);

    List<ReadingSession> findByBook(UUID bookId, UUID userId);
}
