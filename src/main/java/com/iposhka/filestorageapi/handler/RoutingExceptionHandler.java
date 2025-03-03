package com.iposhka.filestorageapi.handler;

import com.iposhka.filestorageapi.dto.responce.ErrorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice
public class RoutingExceptionHandler {
    private static final String UNKNOWN_ENDPOINT_MESSAGE = "Unknown endpoint: %s. Please, check api documentation";

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNoHandlerFoundException(NoHandlerFoundException e) {
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(UNKNOWN_ENDPOINT_MESSAGE
                .formatted(e.getRequestURL()));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponseDto);
    }
}