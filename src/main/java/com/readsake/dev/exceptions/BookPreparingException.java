package com.readsake.dev.exceptions;

/** The durable ingestion worker has not published a readable package yet. */
public class BookPreparingException extends RuntimeException {
    public BookPreparingException(String message) { super(message); }
}
