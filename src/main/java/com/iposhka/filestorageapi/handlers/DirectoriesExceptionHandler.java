package com.iposhka.filestorageapi.handlers;

import com.iposhka.filestorageapi.dto.responce.ErrorResponseDto;
import com.iposhka.filestorageapi.exception.DirectoryAlreadyExistsException;
import com.iposhka.filestorageapi.exception.NotValidPathFolderException;
import com.iposhka.filestorageapi.exception.ParentDirectoryNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestControllerAdvice
public class DirectoriesExceptionHandler {

    @ExceptionHandler(NotValidPathFolderException.class)
    public ResponseEntity<ErrorResponseDto> handleNotValidPathException(Exception e) {
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(e.getMessage());
        return ResponseEntity.badRequest().body(errorResponseDto);
    }

    @ExceptionHandler(DirectoryAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDto> handleDirectoryAlreadyExistsException(Exception e) {
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(e.getMessage());
        return ResponseEntity.status(CONFLICT).body(errorResponseDto);
    }

    @ExceptionHandler(ParentDirectoryNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleParentDirectoryNotFoundException(Exception e) {
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(e.getMessage());
        return ResponseEntity.status(NOT_FOUND).body(errorResponseDto);
    }
}