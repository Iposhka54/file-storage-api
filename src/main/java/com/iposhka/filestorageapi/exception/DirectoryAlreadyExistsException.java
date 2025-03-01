package com.iposhka.filestorageapi.exception;

public class DirectoryAlreadyExistsException extends RuntimeException {
  public DirectoryAlreadyExistsException(String message) {
    super(message);
  }
}
