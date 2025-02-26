package com.iposhka.filestorageapi;

import com.iposhka.filestorageapi.dto.responce.ErrorResponseDto;
import com.iposhka.filestorageapi.exception.UserAlreadyExistException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApplicationExceptionHandler {
    @ExceptionHandler({UserAlreadyExistException.class})
    public ResponseEntity<ErrorResponseDto> handleUserAlreadyExistException(UserAlreadyExistException e){
        return ResponseEntity.
    }
}