package com.marginalia.dev.services;

import com.marginalia.dev.dto.note.NoteCreateRequest;
import com.marginalia.dev.dto.note.NoteUpdateRequest;
import com.marginalia.dev.entities.Note;

import java.util.List;
import java.util.UUID;

public interface NoteService {

    Note create(NoteCreateRequest req, UUID userId);

    Note getOne(UUID id, UUID userId);

    List<Note> listForBook(UUID bookId, UUID userId);

    List<Note> listForHighlight(UUID highlightId, UUID userId);

    List<Note> listRecent(UUID userId, int limit);

    List<Note> listSaved(UUID userId);

    /** Partial update: bodyMd and/or isSaved — only non-null fields apply. */
    Note update(UUID id, NoteUpdateRequest req, UUID userId);

    void delete(UUID id, UUID userId);
}
