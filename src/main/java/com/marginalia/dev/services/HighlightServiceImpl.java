package com.marginalia.dev.services;

import com.marginalia.dev.dto.highlight.HighlightCreateRequest;
import com.marginalia.dev.dto.highlight.HighlightUpdateRequest;
import com.marginalia.dev.entities.Book;
import com.marginalia.dev.entities.Highlight;
import com.marginalia.dev.exceptions.NotFoundException;
import com.marginalia.dev.repo.dao.BookDao;
import com.marginalia.dev.repo.dao.HighlightDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class HighlightServiceImpl implements HighlightService {

    @Autowired
    private HighlightDao highlightDao;

    @Autowired
    private BookDao bookDao;

    @Override
    @Transactional
    public Highlight create(HighlightCreateRequest req, UUID userId) {
        // Ownership check — the bookId in the request must belong to this user.
        Book b = bookDao.findByIdAndUser(req.getBookId(), userId)
                .orElseThrow(() -> new NotFoundException("Book not found"));

        Highlight h = new Highlight();
        h.setUserId(userId);
        h.setBookId(b.getId());
        h.setTextChapterRef(req.getTextChapterRef());
        h.setTextStartOffset(req.getTextStartOffset());
        h.setTextEndOffset(req.getTextEndOffset());
        h.setPassageText(req.getPassageText());
        h.setAudioStartSec(req.getAudioStartSec());
        h.setAudioEndSec(req.getAudioEndSec());
        h.setColorTag(req.getColorTag());

        return highlightDao.insert(h);
    }

    @Override
    public Highlight getOne(UUID id, UUID userId) {
        return highlightDao.findByIdAndUser(id, userId)
                .orElseThrow(() -> new NotFoundException("Highlight not found"));
    }

    @Override
    public List<Highlight> listForBook(UUID bookId, UUID userId) {
        // Verifies ownership of the book first; returns empty if user doesn't own it.
        bookDao.findByIdAndUser(bookId, userId)
                .orElseThrow(() -> new NotFoundException("Book not found"));
        return highlightDao.findByBook(bookId, userId);
    }

    @Override
    public List<Highlight> listRecent(UUID userId, int limit) {
        return highlightDao.findRecentForUser(userId, Math.min(Math.max(limit, 1), 100));
    }

    @Override
    @Transactional
    public Highlight updateColor(UUID id, HighlightUpdateRequest req, UUID userId) {
        return highlightDao.patchColorTag(id, userId, req.getColorTag())
                .orElseThrow(() -> new NotFoundException("Highlight not found"));
    }

    @Override
    @Transactional
    public void delete(UUID id, UUID userId) {
        if (!highlightDao.delete(id, userId)) {
            throw new NotFoundException("Highlight not found");
        }
    }
}
