package com.marginalia.dev.repo.dao;

import com.marginalia.dev.entities.Note;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NoteDao {

    Optional<Note> findByIdAndUser(UUID id, UUID userId);

    List<Note> findByBook(UUID bookId, UUID userId);

    List<Note> findByHighlight(UUID highlightId, UUID userId);

    /** The user's most-recent notes across all books, newest first. */
    List<Note> findRecentForUser(UUID userId, int limit);

    /** Notes bookmarked into the user's Saved collection, newest first. */
    List<Note> findSavedForUser(UUID userId);

    Note insert(Note note);

    Optional<Note> updateBody(UUID id, UUID userId, String bodyMd);

    Optional<Note> patchSaved(UUID id, UUID userId, boolean isSaved);

    boolean delete(UUID id, UUID userId);
}
