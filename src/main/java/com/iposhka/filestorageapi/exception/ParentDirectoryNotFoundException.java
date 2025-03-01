package com.iposhka.filestorageapi.exception;

public class ParentDirectoryNotFoundException extends RuntimeException {
    public ParentDirectoryNotFoundException(String message) {
        super(message);
    }
}
