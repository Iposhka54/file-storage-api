package com.iposhka.filestorageapi.exception;

import java.util.List;
import lombok.Getter;

@Getter
public class StorageQuotaExceededException extends RuntimeException {

    private final List<String> rejectedFiles;

    public StorageQuotaExceededException(String message, List<String> rejectedFiles) {
        super(message);
        this.rejectedFiles = rejectedFiles;
    }
}