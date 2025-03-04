package com.iposhka.filestorageapi.service;

import com.iposhka.filestorageapi.dto.responce.resourse.DirectoryResponseDto;
import com.iposhka.filestorageapi.dto.responce.resourse.DownloadResourceDto;
import com.iposhka.filestorageapi.dto.responce.resourse.FileResponseDto;
import com.iposhka.filestorageapi.dto.responce.resourse.ResourceResponseDto;
import com.iposhka.filestorageapi.exception.*;
import com.iposhka.filestorageapi.repository.MinioRepository;
import io.minio.GetObjectResponse;
import io.minio.Result;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class StorageService {
    private final MinioRepository minioRepository;
    private static final int BUFFER_SIZE = 8192;
    private static final String USER_DIR_PATTERN = "^user-\\d+-files/";
    private static final String USER_DIR_TEMPLATE = "user-%d-files/";
    private static final String LAST_SLASH_PATTERN = "/$";
    private static final String EMPTY = "";
    private static final boolean MUST_END_WITH_SLASH = true;
    private static final boolean NOT_NEED_RECURSIVE = false;
    private static final boolean NEED_RECURSIVE = true;
    private static final String INVALID_PATH_ERROR_MESSAGE = "Path to resource not valid";
    private static final String DATABASE_ERROR_MESSAGE = "Any problems with database";
    private static final String RESOURCE_NOT_FOUND_MESSAGE = "Resource not found";
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

        return new DirectoryResponseDto(responsePath, extractName(fullPath));
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
            throw new ResourceNotFoundException(RESOURCE_NOT_FOUND_MESSAGE);
        }

        StatObjectResponse resource = maybeResource.get();

        String parentPathWithoutUserDir = parentPath
                .replaceFirst(USER_DIR_PATTERN, EMPTY);
        String responsePath = removeLastSlash(parentPathWithoutUserDir);

        String name = extractName(fullPath);

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

    public DownloadResourceDto downloadResource(String path, long userId) {
        if (path.isBlank() || path.startsWith("/")) {
            throw new InvalidResourcePathException(INVALID_PATH_ERROR_MESSAGE);
        }
        String fullPath = USER_DIR_TEMPLATE.formatted(userId) + path;

        Optional<GetObjectResponse> maybeResource = executeMinioOperationIgnoreNotFound(() -> minioRepository.getObject(fullPath));
        GetObjectResponse resource = maybeResource.orElseThrow(() -> new ResourceNotFoundException(RESOURCE_NOT_FOUND_MESSAGE));

        return !fullPath.endsWith("/")
                ? downloadFile(resource, fullPath)
                : downloadZipDirectory(fullPath);
    }

    private DownloadResourceDto downloadFile(GetObjectResponse resource, String fullPath) {
        return DownloadResourceDto.builder()
                .resource(new InputStreamResource(resource))
                .name(extractName(fullPath))
                .build();
    }

    private DownloadResourceDto downloadZipDirectory(String fullPath) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try (ZipOutputStream zipOut = new ZipOutputStream(byteArrayOutputStream)) {
            for (Result<Item> itemResult : minioRepository.listObjects(fullPath, NEED_RECURSIVE)) {
                Item item = executeMinioOperation(itemResult::get);

                if(isMinioDirectoryObject(fullPath, item)) continue;

                GetObjectResponse fileResponse = executeMinioOperation(() -> minioRepository.getObject(item.objectName()));

                zipOut.putNextEntry(new ZipEntry(item.objectName().replaceFirst(fullPath, EMPTY)));
                try (InputStream inputStream = fileResponse) {
                    byte[] buffer = new byte[BUFFER_SIZE];//8 Kb
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        zipOut.write(buffer, 0, bytesRead);
                    }
                }
                zipOut.closeEntry();
            }
            zipOut.finish();
        } catch (IOException e) {
            throw new CreateZipException("Error with creating zip archive");
        }

        ByteArrayInputStream zipByteInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        return DownloadResourceDto.builder()
                .resource(new InputStreamResource(() -> zipByteInputStream))
                .name(extractName(fullPath) + ".zip")
                .build();
    }

    private ResourceResponseDto createResource(Result<Item> itemResult, String parentPath, String fullPath) {
        Item item = executeMinioOperation(itemResult::get);

        if (isMinioDirectoryObject(fullPath, item)) {
            return MINIO_DIRECTORY_OBJECT;
        }

        String objectName = removeLastSlash(item.objectName());
        String name = objectName.substring(objectName.lastIndexOf('/') + 1);

        return item.isDir()
                ? new DirectoryResponseDto(parentPath, name)
                : new FileResponseDto(parentPath, name, item.size());
    }

    private boolean isMinioDirectoryObject(String fullPath, Item item) {
        return fullPath.equals(item.objectName())
               && !item.isDir()
               && item.size() == 0;
    }

    private boolean directoryExists(String path) {
        return executeMinioOperationIgnoreNotFound(() -> minioRepository.getObject(path)).isPresent();
    }

    private static String extractName(String fullPath) {
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

    private <T> T executeMinioOperation(MinioSupplier<T> action) {
        try {
            return action.execute();
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