package com.marginalia.dev.repo.dao;

import java.util.UUID;

public interface BookRequestDao {

    /** Logs one "Request this book" tap. Duplicates are fine — they ARE the demand signal. */
    void insert(UUID userId, String title, String author, String isbn13, String googleId);
}
