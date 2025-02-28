package com.iposhka.filestorageapi;

import com.iposhka.filestorageapi.dto.responce.ErrorResponseDto;
import com.iposhka.filestorageapi.exception.DatabaseException;
import com.iposhka.filestorageapi.exception.UserAlreadyExistsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ApplicationExceptionHandler {
    private static final String BAD_REQUEST_MESSAGE = "Request body is empty or invalid. Please provide required fields.";
    private static final String UNAUTHORIZED_MESSAGE = "Invalid username or email";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationException(MethodArgumentNotValidException e) {
        ErrorResponseDto badRequestError = new ErrorResponseDto(BAD_REQUEST_MESSAGE);

        e.getBindingResult().getFieldErrors()
                .forEach(error -> badRequestError.putError(error.getField(), error.getDefaultMessage()));

        return ResponseEntity.badRequest().body(badRequestError);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDto> handleUserAlreadyExistsException(UserAlreadyExistsException e) {
        log.error(e.getMessage());

        ErrorResponseDto conflictError = new ErrorResponseDto(e.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(conflictError);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponseDto> handleBadCredentialsException(){
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponseDto(UNAUTHORIZED_MESSAGE));
    }

    @ExceptionHandler(DatabaseException.class)
    public ResponseEntity<ErrorResponseDto> handleDatabaseException(Exception e) {
        log.error("Database error while saving user: {}", e.getMessage());

        return ResponseEntity.internalServerError().body(new ErrorResponseDto(e.getMessage()));
    }
}