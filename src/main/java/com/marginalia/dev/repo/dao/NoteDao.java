package com.marginalia.dev.repo.dao;

import com.marginalia.dev.entities.Note;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NoteDao {

    Optional<Note> findByIdAndUser(UUID id, UUID userId);

    List<Note> findByBook(UUID bookId, UUID userId);

    List<Note> findByHighlight(UUID highlightId, UUID userId);

    Note insert(Note note);

    Optional<Note> updateBody(UUID id, UUID userId, String bodyMd);

    boolean delete(UUID id, UUID userId);
}
