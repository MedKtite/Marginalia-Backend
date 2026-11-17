package com.readsake.dev.exceptions;

/** A temporary dependency outage; callers may retry later. */
public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message) { super(message); }
}
