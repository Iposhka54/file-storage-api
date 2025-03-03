package com.iposhka.filestorageapi.handler;

import com.iposhka.filestorageapi.dto.responce.ErrorResponseDto;
import com.iposhka.filestorageapi.exception.DatabaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class DatabaseExceptionHandler {
    @ExceptionHandler(DatabaseException.class)
    public ResponseEntity<ErrorResponseDto> handleDatabaseException(Exception e) {
        ErrorResponseDto databaseError = new ErrorResponseDto(e.getMessage());

        return ResponseEntity.internalServerError().body(databaseError);
    }
}