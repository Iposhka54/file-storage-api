package com.iposhka.filestorageapi.service;

import com.iposhka.filestorageapi.dto.responce.resourse.DirectoryResponseDto;
import com.iposhka.filestorageapi.dto.responce.resourse.FileResponseDto;
import com.iposhka.filestorageapi.dto.responce.resourse.ResourceResponseDto;
import com.iposhka.filestorageapi.exception.*;
import com.iposhka.filestorageapi.repository.MinioRepository;
import io.minio.Result;
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

import static com.iposhka.filestorageapi.dto.ResourceType.FILE;

@Service
@RequiredArgsConstructor
public class DirectoryService {
    private final MinioRepository minioRepository;
    private static final String USER_DIR_PATTERN = "^user-\\d+-files/";
    private static final String USER_DIR_TEMPLATE = "user-%d-files/";
    private static final String LAST_SLASH_PATTERN = "/$";
    private static final String EMPTY = "";
    private static final boolean MUST_END_WITH_SLASH = true;
    private static final boolean MUST_NOT_END_WITH_SLASH = true;
    private static final String INVALID_PATH_ERROR_MESSAGE = "Path to folder not valid";
    private static final String DATABASE_ERROR_MESSAGE = "Any problems with database of files";

    @SneakyThrows
    public void createUserDirectory(long userId) {
        minioRepository.createUserDirectory(USER_DIR_TEMPLATE.formatted(userId));
    }

    public List<ResourceResponseDto> listDirectoryContents(String path, long userId) {
        String fullPath = validateAndBuildPath(path, userId, MUST_NOT_END_WITH_SLASH);

        if (!directoryExists(fullPath)) {
            throw new DirectoryNotFoundException("Directory not found");
        }

        List<ResourceResponseDto> result = new ArrayList<>();
        String parentPath = fullPath.replaceFirst(USER_DIR_PATTERN, EMPTY);
        String parentPathWithoutLastSlash = removeLastSlash(parentPath);

        for (Result<Item> itemResult : minioRepository.listObjects(fullPath)) {
            ResourceResponseDto resource = createResource(itemResult, parentPathWithoutLastSlash);
            if(isNotMinioDir(resource, parentPathWithoutLastSlash)){
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

        executeMinioOperation(() -> minioRepository.createEmptyDirectory(fullPath));

        String parentPathWithoutUserDir = parentPath
                .replaceFirst(USER_DIR_PATTERN, EMPTY);
        String parentPathWithoutUserDirAndLastSlash = removeLastSlash(parentPathWithoutUserDir);

        return new DirectoryResponseDto(parentPathWithoutUserDirAndLastSlash, extractDirectoryName(fullPath));
    }

    @SneakyThrows
    private ResourceResponseDto createResource(Result<Item> itemResult, String parentPath) {
        Item item = itemResult.get();

        String objectName = removeLastSlash(item.objectName());
        String name = objectName.substring(objectName.lastIndexOf('/') + 1);

        return item.isDir()
                ? new DirectoryResponseDto(parentPath, name)
                : new FileResponseDto(parentPath, name, item.size());
    }

    private static boolean isNotMinioDir(ResourceResponseDto resource, String parentPathWithoutLastSlash) {
        return !(resource instanceof FileResponseDto file
                 && file.getSize() == 0
                 && file.getName().equals(parentPathWithoutLastSlash));
    }

    private boolean directoryExists(String path) {
        return Boolean.TRUE.equals(executeMinioOperationIgnoreNotFound(()
                -> minioRepository.getObject(path) != null));
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
        if (path.isEmpty()) return USER_DIR_TEMPLATE.formatted(userId);
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

    private void executeMinioOperation(MinioAction action) {
        try {
            action.execute();
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new DatabaseException(DATABASE_ERROR_MESSAGE);
        }
    }

    private <T> T executeMinioOperationIgnoreNotFound(MinioSupplier<T> action) {
        try {
            return action.execute();
        } catch (ErrorResponseException e) {
            return null;
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