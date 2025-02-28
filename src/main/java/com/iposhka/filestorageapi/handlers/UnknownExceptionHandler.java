package com.iposhka.filestorageapi.handlers;

import com.iposhka.filestorageapi.dto.responce.ErrorResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class UnknownExceptionHandler {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleException(Exception e) {
        log.error("Unknown error: {}", e.getMessage());
        return ResponseEntity.internalServerError().body(new ErrorResponseDto("Unknown error"));
    }
}