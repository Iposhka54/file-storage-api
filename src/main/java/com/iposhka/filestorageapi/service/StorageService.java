package com.iposhka.filestorageapi.service;

import com.iposhka.filestorageapi.dto.responce.resourse.DirectoryResponseDto;
import com.iposhka.filestorageapi.dto.responce.resourse.FileResponseDto;
import com.iposhka.filestorageapi.dto.responce.resourse.ResourceResponseDto;
import com.iposhka.filestorageapi.exception.*;
import com.iposhka.filestorageapi.repository.MinioRepository;
import io.minio.Result;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StorageService {
    private final MinioRepository minioRepository;
    private static final String USER_DIR_PATTERN = "^user-\\d+-files/";
    private static final String USER_DIR_TEMPLATE = "user-%d-files/";
    private static final String LAST_SLASH_PATTERN = "/$";
    private static final String EMPTY = "";
    private static final boolean MUST_END_WITH_SLASH = true;
    private static final boolean MUST_NOT_END_WITH_SLASH = false;
    private static final boolean NOT_NEED_RECURSIVE = false;
    private static final boolean NEED_RECURSIVE = true;
    private static final String INVALID_PATH_ERROR_MESSAGE = "Path to folder not valid";
    private static final String DATABASE_ERROR_MESSAGE = "Any problems with database";
    private static final ResourceResponseDto MINIO_DIRECTORY_OBJECT = new DirectoryResponseDto();

    public void createUserDirectory(long userId) {
        String userDir = USER_DIR_TEMPLATE.formatted(userId);
        executeMinioOperation(() -> minioRepository.createUserDirectory(userDir), "with creating directory");
    }

    public List<ResourceResponseDto> getDirectoryFiles(String path, long userId) {
        String fullPath = validateAndBuildPath(path, userId, MUST_END_WITH_SLASH);

        if (!directoryExists(fullPath)) {
            throw new DirectoryNotFoundException("Directory not found");
        }

        List<ResourceResponseDto> result = new ArrayList<>();
        String parentPath = fullPath.replaceFirst(USER_DIR_PATTERN, EMPTY);
        String parentPathWithoutLastSlash = removeLastSlash(parentPath);

        for (Result<Item> itemResult : minioRepository.listObjects(fullPath, NOT_NEED_RECURSIVE)) {
            ResourceResponseDto resource = createResource(itemResult, parentPathWithoutLastSlash, fullPath);
            if (MINIO_DIRECTORY_OBJECT != resource) {
                result.add(resource);
            }
        }

        return result;
    }

    public DirectoryResponseDto createEmptyDirectory(String path, long userId) {
        String fullPath = validateAndBuildPath(path, userId, MUST_END_WITH_SLASH);
        String parentPath = getParentPath(fullPath, userId);

        if (!directoryExists(parentPath)) {
            throw new ParentDirectoryNotFoundException("Parent directory does not exist");
        }
        if (directoryExists(fullPath)) {
            throw new DirectoryAlreadyExistsException("Directory already exists");
        }

        executeMinioOperation(() -> minioRepository.createEmptyDirectory(fullPath), "with create directory");

        String parentPathWithoutUserDir = parentPath
                .replaceFirst(USER_DIR_PATTERN, EMPTY);
        String responsePath = removeLastSlash(parentPathWithoutUserDir);

        return new DirectoryResponseDto(responsePath, extractDirectoryName(fullPath));
    }

    public ResourceResponseDto getInfoAboutResource(String path, long userId) {
        if (path.isBlank()) {
            throw new InvalidResourcePathException("Invalid path");
        }

        String fullPath = USER_DIR_TEMPLATE.formatted(userId) + path;
        String parentPath = getParentPath(path, userId);

        Optional<StatObjectResponse> maybeResource = executeMinioOperationIgnoreNotFound(
                () -> minioRepository.statObject(fullPath));

        if (maybeResource.isEmpty()) {
            throw new ResourceNotFoundException("Resource not found");
        }

        StatObjectResponse resource = maybeResource.get();

        String parentPathWithoutUserDir = parentPath
                .replaceFirst(USER_DIR_PATTERN, EMPTY);
        String responsePath = removeLastSlash(parentPathWithoutUserDir);

        String name = extractDirectoryName(fullPath);

        return fullPath.endsWith("/")
                ? new DirectoryResponseDto(responsePath, name)
                : new FileResponseDto(responsePath, name, resource.size());
    }

    public void deleteResource(String path, long userId) {
        String fullPath = USER_DIR_TEMPLATE.formatted(userId) + path;

        Optional<StatObjectResponse> maybeResource = executeMinioOperationIgnoreNotFound(
                () -> minioRepository.statObject(fullPath));

        if (maybeResource.isEmpty()) {
            throw new ResourceNotFoundException("Resource not found");
        }

        if (!fullPath.endsWith("/")) {
            executeMinioOperation(() -> minioRepository.deleteObject(fullPath), "with deleting file");
            return;
        }

        List<String> objectsToDelete = new ArrayList<>();
        for (Result<Item> itemResult : minioRepository.listObjects(fullPath, NEED_RECURSIVE)) {
            Item item = executeMinioOperationIgnoreNotFound(itemResult::get)
                    .orElseThrow(() -> new DatabaseException("Any problems when get objects"));
            objectsToDelete.add(item.objectName());
        }

        for (String objectName : objectsToDelete) {
            executeMinioOperation(() -> minioRepository.deleteObject(objectName), "with delete object");
        }

        executeMinioOperation(() -> minioRepository.deleteObject(fullPath), "while deleting empty directory");
    }

    @SneakyThrows
    private ResourceResponseDto createResource(Result<Item> itemResult, String parentPath, String fullPath) {
        Item item = itemResult.get();

        if (fullPath.equals(item.objectName())
            && !item.isDir()
            && item.size() == 0) {
            return MINIO_DIRECTORY_OBJECT;
        }

        String objectName = removeLastSlash(item.objectName());
        String name = objectName.substring(objectName.lastIndexOf('/') + 1);

        return item.isDir()
                ? new DirectoryResponseDto(parentPath, name)
                : new FileResponseDto(parentPath, name, item.size());
    }

    private boolean directoryExists(String path) {
        return executeMinioOperationIgnoreNotFound(() -> minioRepository.getObject(path)).isPresent();
    }

    private static String extractDirectoryName(String fullPath) {
        String cleanedPath = removeLastSlash(fullPath);

        int lastSlashIndex = cleanedPath.lastIndexOf('/');

        if (lastSlashIndex == -1) {
            return cleanedPath;
        }

        return cleanedPath.substring(lastSlashIndex + 1);
    }

    private static String removeLastSlash(String path) {
        return path.replaceAll(LAST_SLASH_PATTERN, EMPTY);
    }

    private static String validateAndBuildPath(String path, long userId, boolean mustEndWithSlash) {
        if (path.isBlank()) return USER_DIR_TEMPLATE.formatted(userId);
        if (path.startsWith("/") || (mustEndWithSlash && !path.endsWith("/"))) {
            throw new InvalidPathFolderException(INVALID_PATH_ERROR_MESSAGE);
        }
        return USER_DIR_TEMPLATE.formatted(userId) + path;
    }

    private static String getParentPath(String fullPath, long userId) {
        String rootPath = USER_DIR_TEMPLATE.formatted(userId);
        if (fullPath.equals(rootPath)) return EMPTY;
        return fullPath.substring(0, fullPath.lastIndexOf('/', fullPath.length() - 2) + 1);
    }

    private void executeMinioOperation(MinioAction action, String fromMessage) {
        try {
            action.execute();
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new DatabaseException(DATABASE_ERROR_MESSAGE + " " + fromMessage);
        }
    }

    private <T> Optional<T> executeMinioOperationIgnoreNotFound(MinioSupplier<T> action) {
        try {
            return Optional.ofNullable(action.execute());
        } catch (ErrorResponseException e) {
            return Optional.empty();
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new DatabaseException(DATABASE_ERROR_MESSAGE);
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