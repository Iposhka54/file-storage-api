package com.iposhka.filestorageapi.handlers;

import com.iposhka.filestorageapi.dto.responce.ErrorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice
public class RoutingExceptionHandler {
    private static final String UNKNOWN_ENDPOINT_MESSAGE = "Method %s is not allowed for this endpoint. Please, check api documentation";

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNoHandlerFoundException(NoHandlerFoundException e) {
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(UNKNOWN_ENDPOINT_MESSAGE
                .formatted(e.getRequestURL()));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponseDto);
    }
}