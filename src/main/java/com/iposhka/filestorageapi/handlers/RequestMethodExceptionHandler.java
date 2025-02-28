package com.iposhka.filestorageapi.handlers;

import com.iposhka.filestorageapi.dto.responce.ErrorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RequestMethodExceptionHandler {
    private static final String METHOD_NOT_ALLOWED_MESSAGE = "Method %s is not allowed for this endpoint. Please, check api documentation";

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodNotAllowedException(HttpRequestMethodNotSupportedException e) {
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(METHOD_NOT_ALLOWED_MESSAGE
                .formatted(e.getMethod()));
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponseDto);
    }
}