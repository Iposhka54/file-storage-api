package com.iposhka.filestorageapi.service;

import com.iposhka.filestorageapi.dto.responce.resourse.DirectoryResponseDto;
import com.iposhka.filestorageapi.exception.DatabaseException;
import com.iposhka.filestorageapi.exception.DirectoryAlreadyExistsException;
import com.iposhka.filestorageapi.exception.NotValidPathFolderException;
import com.iposhka.filestorageapi.exception.ParentDirectoryNotFoundException;
import com.iposhka.filestorageapi.repository.MinioRepository;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Service
@RequiredArgsConstructor
public class StorageService {
    private final MinioRepository minioRepository;
    public static final String USER_DIRECTORY = "user-%d-files/";

    @SneakyThrows
    public void createUserDirectory(long userId) {
        minioRepository.createUserDirectory(USER_DIRECTORY.formatted(userId));
    }

    public DirectoryResponseDto createEmptyDirectory(String path, long userId) {
        String fullPath = getFullPath(path, userId);

        String parentPath = getParentPath(fullPath, userId);

        Boolean parentExists = executeMinioOperationIgnoreNotFound(() -> minioRepository.getObject(parentPath) != null);

        if (!parentDirectoryExists(parentExists)) {
            throw new ParentDirectoryNotFoundException("Parent directory does not exist");
        }

        Boolean directoryExists = executeMinioOperationIgnoreNotFound(() -> minioRepository.getObject(fullPath) != null);

        if(Boolean.TRUE.equals(directoryExists)){
            throw new DirectoryAlreadyExistsException("Directory already exists");
        }

        executeMinioOperation(() -> minioRepository.createEmptyDirectory(fullPath));

        return buildDirectoryResponseDto(fullPath);
    }

    private static boolean parentDirectoryExists(Boolean parentExists) {
        return Boolean.TRUE.equals(parentExists) || parentExists != null;
    }

    private static String getFullPath(String path, long userId) {
        if (!path.endsWith("/") || path.startsWith("/")) {
            throw new NotValidPathFolderException("Path to folder not valid");
        }
        return USER_DIRECTORY.formatted(userId) + path;
    }

    private static DirectoryResponseDto buildDirectoryResponseDto(String fullPath) {
        String tempPath = fullPath.replaceAll("/$", "");

        int firstSlashIndex = tempPath.indexOf('/');
        int lastSlashIndex = tempPath.lastIndexOf('/');

        String parentPath = getParentPath(lastSlashIndex, firstSlashIndex, tempPath);
        String name = tempPath.substring(lastSlashIndex + 1);
        return new DirectoryResponseDto(parentPath, name);
    }

    private static String getParentPath(int lastSlashIndex, int firstSlashIndex, String tempPath) {
        return (lastSlashIndex == firstSlashIndex) ? "" : tempPath.substring(firstSlashIndex + 1, lastSlashIndex);
    }

    private static String getParentPath(String fullPath, long userId) {
        String userRoot = USER_DIRECTORY.formatted(userId);
        String tempPath = fullPath.replaceAll("/$", "");

        if (tempPath.equals(userRoot)) {
            return "";
        }

        int lastSlashIndex = tempPath.lastIndexOf('/');

        if (lastSlashIndex == userRoot.length() - 1) {
            return userRoot;
        }

        return tempPath.substring(0, lastSlashIndex + 1);
    }


    private void executeMinioOperation(MinioAction action) {
        try {
            action.execute();
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new DatabaseException("Any problems with database of files");
        }
    }

    private <T> T executeMinioOperationIgnoreNotFound(MinioSupplier<T> action) {
        try {
            return action.execute();
        } catch (ErrorResponseException e) {
            return null;
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new DatabaseException("Any problems with database of files");
        }
    }

    @FunctionalInterface
    private interface MinioAction {
        void execute() throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException;
    }

    @FunctionalInterface
    private interface MinioSupplier<T> {
        T execute() throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException;
    }
}