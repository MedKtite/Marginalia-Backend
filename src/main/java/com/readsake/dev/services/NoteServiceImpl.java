package com.readsake.dev.services;

import com.readsake.dev.dto.note.NoteCreateRequest;
import com.readsake.dev.dto.note.NoteUpdateRequest;
import com.readsake.dev.entities.Highlight;
import com.readsake.dev.entities.Note;
import com.readsake.dev.exceptions.NotFoundException;
import com.readsake.dev.repo.dao.BookDao;
import com.readsake.dev.repo.dao.HighlightDao;
import com.readsake.dev.repo.dao.NoteDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NoteServiceImpl implements NoteService {

    @Autowired
    private NoteDao noteDao;

    @Autowired
    private BookDao bookDao;

    @Autowired
    private HighlightDao highlightDao;

    @Override
    @Transactional
    public Note create(NoteCreateRequest req, UUID userId) {
        // Verify book ownership.
        bookDao.findByIdAndUser(req.getBookId(), userId)
                .orElseThrow(() -> new NotFoundException("Book not found"));

        // If a highlight is referenced, verify it belongs to the user AND the same book.
        if (req.getHighlightId() != null) {
            Highlight h = highlightDao.findByIdAndUser(req.getHighlightId(), userId)
                    .orElseThrow(() -> new NotFoundException("Highlight not found"));
            if (!h.getBookId().equals(req.getBookId())) {
                throw new NotFoundException("Highlight does not belong to that book");
            }
        }

        Note n = new Note();
        n.setUserId(userId);
        n.setBookId(req.getBookId());
        n.setHighlightId(req.getHighlightId());
        n.setBodyMd(req.getBodyMd());

        return noteDao.insert(n);
    }

    @Override
    public Note getOne(UUID id, UUID userId) {
        return noteDao.findByIdAndUser(id, userId)
                .orElseThrow(() -> new NotFoundException("Note not found"));
    }

    @Override
    public List<Note> listForBook(UUID bookId, UUID userId) {
        bookDao.findByIdAndUser(bookId, userId)
                .orElseThrow(() -> new NotFoundException("Book not found"));
        return noteDao.findByBook(bookId, userId);
    }

    @Override
    public List<Note> listForHighlight(UUID highlightId, UUID userId) {
        highlightDao.findByIdAndUser(highlightId, userId)
                .orElseThrow(() -> new NotFoundException("Highlight not found"));
        return noteDao.findByHighlight(highlightId, userId);
    }

    @Override
    public List<Note> listRecent(UUID userId, int limit) {
        return noteDao.findRecentForUser(userId, Math.min(Math.max(limit, 1), 200));
    }

    @Override
    public List<Note> listSaved(UUID userId) {
        return noteDao.findSavedForUser(userId);
    }

    @Override
    @Transactional
    public Note update(UUID id, NoteUpdateRequest req, UUID userId) {
        Note current = noteDao.findByIdAndUser(id, userId)
                .orElseThrow(() -> new NotFoundException("Note not found"));
        if (req.getBodyMd() != null && !req.getBodyMd().isBlank()) {
            current = noteDao.updateBody(id, userId, req.getBodyMd())
                    .orElseThrow(() -> new NotFoundException("Note not found"));
        }
        if (req.getIsSaved() != null) {
            current = noteDao.patchSaved(id, userId, req.getIsSaved())
                    .orElseThrow(() -> new NotFoundException("Note not found"));
        }
        return current;
    }

    @Override
    @Transactional
    public void delete(UUID id, UUID userId) {
        if (!noteDao.delete(id, userId)) {
            throw new NotFoundException("Note not found");
        }
    }
}
