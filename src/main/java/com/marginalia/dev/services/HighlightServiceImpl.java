package com.marginalia.dev.services;

import com.marginalia.dev.dto.highlight.HighlightCreateRequest;
import com.marginalia.dev.dto.highlight.HighlightUpdateRequest;
import com.marginalia.dev.entities.Book;
import com.marginalia.dev.entities.Highlight;
import com.marginalia.dev.exceptions.NotFoundException;
import com.marginalia.dev.repo.dao.BookDao;
import com.marginalia.dev.repo.dao.HighlightDao;
import com.marginalia.dev.repo.dao.TagDao;
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

    @Autowired
    private TagDao tagDao;

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

        Highlight created = highlightDao.insert(h);
        syncSystemTag(created.getId(), req.getColorTag());
        return created;
    }

    /**
     * Mirrors color_tag into highlight_tags so tag-join queries (tag-filtered
     * search, Insights) agree with the color the client shows. Only system
     * tags are managed here — custom user tags are left untouched.
     */
    private void syncSystemTag(UUID highlightId, String colorTag) {
        if (colorTag == null || colorTag.isBlank()) return;
        // Drop any OTHER system tag mirrored earlier (color change case).
        for (var t : tagDao.findByHighlight(highlightId)) {
            if (Boolean.TRUE.equals(t.getIsSystem())
                    && !t.getName().equalsIgnoreCase(colorTag)) {
                tagDao.detachFromHighlight(highlightId, t.getId());
            }
        }
        tagDao.findSystemByName(colorTag)
                .ifPresent(t -> tagDao.attachToHighlight(highlightId, t.getId()));
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
    public List<Highlight> listSaved(UUID userId) {
        return highlightDao.findSavedForUser(userId);
    }

    @Override
    public List<Highlight> listByTag(UUID tagId, UUID userId) {
        tagDao.findVisibleByIdForUser(tagId, userId)
                .orElseThrow(() -> new NotFoundException("Tag not found"));
        return highlightDao.findByTag(tagId, userId);
    }

    @Override
    @Transactional
    public Highlight update(UUID id, HighlightUpdateRequest req, UUID userId) {
        Highlight current = highlightDao.findByIdAndUser(id, userId)
                .orElseThrow(() -> new NotFoundException("Highlight not found"));
        if (req.getColorTag() != null && !req.getColorTag().isBlank()) {
            current = highlightDao.patchColorTag(id, userId, req.getColorTag())
                    .orElseThrow(() -> new NotFoundException("Highlight not found"));
            syncSystemTag(current.getId(), current.getColorTag());
        }
        if (req.getIsSaved() != null) {
            current = highlightDao.patchSaved(id, userId, req.getIsSaved())
                    .orElseThrow(() -> new NotFoundException("Highlight not found"));
        }
        return current;
    }

    @Override
    @Transactional
    public void delete(UUID id, UUID userId) {
        if (!highlightDao.delete(id, userId)) {
            throw new NotFoundException("Highlight not found");
        }
    }
}
