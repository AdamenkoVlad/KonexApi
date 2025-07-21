package com.example.konexapi.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleMaxSizeException(MaxUploadSizeExceededException e) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("message", "File size exceeds maximum limit");
        error.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException e) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("message", e.getMessage());
        error.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.status(500).body(error);
    }
}