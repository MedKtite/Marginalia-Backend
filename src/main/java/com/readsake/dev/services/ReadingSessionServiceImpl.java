package com.readsake.dev.services;

import com.readsake.dev.dto.session.ReadingSessionCreateRequest;
import com.readsake.dev.entities.ReadingSession;
import com.readsake.dev.exceptions.NotFoundException;
import com.readsake.dev.repo.dao.BookDao;
import com.readsake.dev.repo.dao.ReadingSessionDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class ReadingSessionServiceImpl implements ReadingSessionService {

    @Autowired
    private ReadingSessionDao sessionDao;

    @Autowired
    private BookDao bookDao;

    @Override
    @Transactional
    public ReadingSession create(ReadingSessionCreateRequest req, UUID userId) {
        // Ownership check.
        bookDao.findByIdAndUser(req.getBookId(), userId)
                .orElseThrow(() -> new NotFoundException("Book not found"));

        if (!req.getEndedAt().isAfter(req.getStartedAt())) {
            throw new IllegalArgumentException("endedAt must be strictly after startedAt");
        }

        ReadingSession s = new ReadingSession();
        s.setUserId(userId);
        s.setBookId(req.getBookId());
        s.setStartedAt(req.getStartedAt());
        s.setEndedAt(req.getEndedAt());
        s.setDurationSec((int) Duration.between(req.getStartedAt(), req.getEndedAt()).getSeconds());
        s.setDeviceId(req.getDeviceId());
        s.setStartProgressPct(req.getStartProgressPct());
        s.setEndProgressPct(req.getEndProgressPct());

        return sessionDao.insert(s);
    }

    @Override
    public List<ReadingSession> listRecent(UUID userId, int limit) {
        return sessionDao.findRecentForUser(userId, Math.min(Math.max(limit, 1), 100));
    }

    @Override
    public List<ReadingSession> listForBook(UUID bookId, UUID userId) {
        bookDao.findByIdAndUser(bookId, userId)
                .orElseThrow(() -> new NotFoundException("Book not found"));
        return sessionDao.findByBook(bookId, userId);
    }
}
