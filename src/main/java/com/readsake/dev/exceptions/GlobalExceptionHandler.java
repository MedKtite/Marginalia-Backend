package com.readsake.dev.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> unavailable(ServiceUnavailableException e) {
        var response = error(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        return ResponseEntity.status(503).header("Retry-After", "60").body(response.getBody());
    }

    @ExceptionHandler(BookPreparingException.class)
    public ResponseEntity<Map<String, Object>> preparing(BookPreparingException e) {
        var response = error(HttpStatus.CONFLICT, e.getMessage());
        return ResponseEntity.status(409).header("Retry-After", "5").body(response.getBody());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(NotFoundException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> conflict(ConflictException e) {
        return error(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> unauthorized(UnauthorizedException e) {
        return error(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, Object>> forbidden(ForbiddenException e) {
        return error(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, Object>> badRequestExplicit(BadRequestException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        return error(HttpStatus.BAD_REQUEST, msg);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> internal(Exception e) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message != null ? message : "");
        body.put("timestamp", OffsetDateTime.now().toString());
        return ResponseEntity.status(status).body(body);
    }
}
