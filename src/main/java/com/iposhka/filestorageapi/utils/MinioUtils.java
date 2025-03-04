package com.iposhka.filestorageapi.utils;

import com.iposhka.filestorageapi.exception.DatabaseException;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import static com.iposhka.filestorageapi.service.StorageService.DATABASE_ERROR_MESSAGE;

public class MinioUtils {
    @FunctionalInterface
    public interface MinioAction {
        void execute() throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException;
    }

    @FunctionalInterface
    public interface MinioSupplier<T> {
        T execute() throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException;
    }

    public static void executeMinioOperation(MinioAction action, String fromMessage) {
        try {
            action.execute();
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new DatabaseException(DATABASE_ERROR_MESSAGE + " " + fromMessage);
        }
    }

    public static <T> Optional<T> executeMinioOperationIgnoreNotFound(MinioSupplier<T> action) {
        try {
            return Optional.ofNullable(action.execute());
        } catch (ErrorResponseException e) {
            return Optional.empty();
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new DatabaseException(DATABASE_ERROR_MESSAGE);
        }
    }

    public static <T> T executeMinioOperation(MinioSupplier<T> action) {
        try {
            return action.execute();
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new DatabaseException(DATABASE_ERROR_MESSAGE);
        }
    }
}
