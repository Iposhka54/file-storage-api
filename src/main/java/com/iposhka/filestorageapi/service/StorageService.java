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
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.iposhka.filestorageapi.utils.MinioUtils.executeMinioOperation;
import static com.iposhka.filestorageapi.utils.MinioUtils.executeMinioOperationIgnoreNotFound;
import static java.util.regex.Pattern.CASE_INSENSITIVE;

@Service
@RequiredArgsConstructor
public class StorageService {
    private final MinioRepository minioRepository;

    public static final String INVALID_PATH_ERROR_MESSAGE = "Path to resource not valid";
    public static final String DATABASE_ERROR_MESSAGE = "Any problems with database";
    public static final String RESOURCE_NOT_FOUND_MESSAGE = "Resource not found";

    private static final int BUFFER_SIZE = 8192;
    private static final String USER_DIR_PATTERN = "^user-\\d+-files/";
    private static final String USER_DIR_TEMPLATE = "user-%d-files/";
    private static final String SEARCH_TEMPLATE = ".*%s.*";
    private static final String LAST_SLASH_PATTERN = "/$";
    private static final String EMPTY = "";
    private static final boolean MUST_END_WITH_SLASH = true;
    private static final boolean NOT_NEED_RECURSIVE = false;
    private static final boolean NEED_RECURSIVE = true;
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

        if (fullPath.endsWith("/")) {
            deleteDirectoryRecursively(fullPath);
        } else {
            executeMinioOperation(() -> minioRepository.deleteObject(fullPath), "with deleting file");
        }
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

    public List<ResourceResponseDto> searchResources(String query, long userId) {
        String userDirectoryPath = USER_DIR_TEMPLATE.formatted(userId);
        List<ResourceResponseDto> result = new ArrayList<>();

        Pattern pattern = Pattern.compile(SEARCH_TEMPLATE.formatted(query), CASE_INSENSITIVE);

        Iterable<Result<Item>> resources = minioRepository.listObjects(userDirectoryPath, NEED_RECURSIVE);
        for (Result<Item> resultItem : resources) {
            Item item = executeMinioOperation(resultItem::get);
            if (isMinioDirectoryObject(item.objectName(), item)) {
                continue;
            }

            String itemName = extractName(item.objectName());

            Matcher matcher = pattern.matcher(itemName);
            if (matcher.find()) {
                String parentPath = getParentPath(item.objectName(), userId);
                String parentPathWithoutUserDir = parentPath
                        .replaceFirst(USER_DIR_PATTERN, EMPTY);
                String responsePath = removeLastSlash(parentPathWithoutUserDir);

                result.add(new FileResponseDto(responsePath, itemName, item.size()));
            }
        }

        return result;
    }

    public ResourceResponseDto moveOrRenameResource(String from, String to, long userId) {
        if (from.isBlank() || to.isBlank()) {
            throw new InvalidResourcePathException(INVALID_PATH_ERROR_MESSAGE);
        }

        String userDir = String.format(USER_DIR_TEMPLATE, userId);
        String fullFromPath = userDir + from;
        String fullToPath = userDir + to;

        executeMinioOperationIgnoreNotFound(()
                -> minioRepository.statObject(fullFromPath))
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE_NOT_FOUND_MESSAGE));

        executeMinioOperationIgnoreNotFound(()
                -> minioRepository.statObject(fullToPath))
                .ifPresent(resource -> {
                    throw new ResourceAlreadyExistsException("Resource already exists");
                });

        String parentFrom = getParentPath(fullFromPath, userId);
        String parentTo = getParentPath(fullToPath, userId);

        return parentFrom.equals(parentTo)
                ? renameResource(fullFromPath, fullToPath, userId)
                : moveResource(fullFromPath, fullToPath, userId);
    }

    private ResourceResponseDto moveResource(String fullFromPath, String fullToPath, long userId) {
        if (fullFromPath.endsWith("/")) {
            executeMinioOperation(() -> minioRepository.createEmptyDirectory(fullToPath), "creating target directory");
            copyDirectoryRecursively(fullFromPath, fullToPath);
            deleteDirectoryRecursively(fullFromPath);
        } else {
            executeMinioOperation(() -> minioRepository.copyObject(fullFromPath, fullToPath),
                    "with moving file");
            executeMinioOperation(() -> minioRepository.deleteObject(fullFromPath),
                    "with deleting file after move");
        }

        return createResponse(fullToPath, userId);
    }


    private ResourceResponseDto renameResource(String fullFromPath, String fullToPath, long userId) {
        if (fullFromPath.endsWith("/")) {
            copyDirectoryRecursively(fullFromPath, fullToPath);
            deleteDirectoryRecursively(fullFromPath);
        } else {
            executeMinioOperation(() -> minioRepository.copyObject(fullFromPath, fullToPath),
                    "with renaming file");
            executeMinioOperation(() -> minioRepository.deleteObject(fullFromPath),
                    "with deleting file after rename");
        }

        return createResponse(fullToPath, userId);
    }


    private ResourceResponseDto createResponse(String fullPath, long userId) {
        String parentPath = getParentPath(fullPath, userId);
        String parentPathWithoutUserDir = parentPath
                .replaceFirst(USER_DIR_PATTERN, EMPTY);
        String responsePath = removeLastSlash(parentPathWithoutUserDir);

        StatObjectResponse stat = executeMinioOperationIgnoreNotFound(() -> minioRepository.statObject(fullPath)).get();

        return fullPath.endsWith("/")
                ? new DirectoryResponseDto(responsePath, extractName(fullPath))
                : new FileResponseDto(responsePath, extractName(fullPath), stat.size());
    }

    private void copyDirectoryRecursively(String from, String to) {
        for (Result<Item> itemResult : minioRepository.listObjects(from, NEED_RECURSIVE)) {
            Item item = executeMinioOperation(itemResult::get);
            String itemName = item.objectName();

            if (isMinioDirectoryObject(from, item)) {
                continue;
            }

            String newDestination = itemName.replaceFirst(Pattern.quote(from), to);

            executeMinioOperation(() -> minioRepository.copyObject(itemName, newDestination),
                    "with copying file/directory recursively");
        }
    }

    private void deleteDirectoryRecursively(String path) {
        List<String> objectsToDelete = new ArrayList<>();

        for (Result<Item> itemResult : minioRepository.listObjects(path, NEED_RECURSIVE)) {
            Item item = executeMinioOperationIgnoreNotFound(itemResult::get)
                    .orElseThrow(() -> new DatabaseException("Error retrieving objects for deletion"));
            objectsToDelete.add(item.objectName());
        }

        for (String objectName : objectsToDelete) {
            executeMinioOperation(() -> minioRepository.deleteObject(objectName), "with deleting object");
        }

        executeMinioOperation(() -> minioRepository.deleteObject(path), "while deleting empty directory");
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

                if (isMinioDirectoryObject(fullPath, item)) continue;

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
}