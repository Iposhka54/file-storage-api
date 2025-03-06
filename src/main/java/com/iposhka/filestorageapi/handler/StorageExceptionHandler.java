package com.iposhka.filestorageapi.handler;

import com.iposhka.filestorageapi.dto.responce.ErrorResponseDto;
import com.iposhka.filestorageapi.exception.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestControllerAdvice
public class StorageExceptionHandler {

    @ExceptionHandler({InvalidPathFolderException.class,
            InvalidResourcePathException.class})
    public ResponseEntity<ErrorResponseDto> handleNotValidPathException(Exception e) {
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(e.getMessage());
        return ResponseEntity.badRequest().body(errorResponseDto);
    }

    @ExceptionHandler({DirectoryAlreadyExistsException.class,
            ResourceAlreadyExistsException.class})
    public ResponseEntity<ErrorResponseDto> handleDirectoryAlreadyExistsException(Exception e) {
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(e.getMessage());
        return ResponseEntity.status(CONFLICT).body(errorResponseDto);
    }

    @ExceptionHandler({ParentDirectoryNotFoundException.class,
            DirectoryNotFoundException.class,
            ResourceNotFoundException.class
    })
    public ResponseEntity<ErrorResponseDto> handleParentDirectoryNotFoundException(Exception e) {
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(e.getMessage());
        return ResponseEntity.status(NOT_FOUND).body(errorResponseDto);
    }
}