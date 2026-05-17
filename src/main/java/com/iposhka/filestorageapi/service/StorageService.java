package com.iposhka.filestorageapi.service;

import com.iposhka.filestorageapi.dto.responce.resourse.DirectoryResponseDto;
import com.iposhka.filestorageapi.dto.responce.resourse.DownloadResourceDto;
import com.iposhka.filestorageapi.dto.responce.resourse.FileResponseDto;
import com.iposhka.filestorageapi.dto.responce.resourse.ResourceResponseDto;
import com.iposhka.filestorageapi.exception.*;
import com.iposhka.filestorageapi.listener.AuditPublisher;
import com.iposhka.filestorageapi.model.Action;
import com.iposhka.filestorageapi.model.UserApp;
import com.iposhka.filestorageapi.model.Trash;
import com.iposhka.filestorageapi.repository.MinioRepository;
import com.iposhka.filestorageapi.repository.TrashRepository;
import com.iposhka.filestorageapi.repository.UserRepository;
import io.minio.GetObjectResponse;
import io.minio.Result;
import io.minio.SnowballObject;
import io.minio.StatObjectResponse;
import io.minio.messages.Item;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final MinioRepository minioRepository;
    private final UserRepository userRepository;
    private final TrashRepository trashRepository;

    private final AuditPublisher publisher;

    public static final String INVALID_PATH_ERROR_MESSAGE = "Path to resource not valid";
    public static final String DATABASE_ERROR_MESSAGE = "Any problems with database";
    public static final String RESOURCE_NOT_FOUND_MESSAGE = "Resource not found";

    private static final int BUFFER_SIZE = 8192;
    private static final String USER_DIR_PATTERN = "^user-\\d+-files/";
    private static final String USER_DIR_TEMPLATE = "user-%d-files/";
    private static final String SEARCH_TEMPLATE = ".*%s.*";
    private static final String LAST_SLASH_PATTERN = "/$";
    private static final String EMPTY = "";
    private static final boolean NOT_NEED_RECURSIVE = false;
    private static final boolean NEED_RECURSIVE = true;

    private static final ResourceResponseDto MINIO_DIRECTORY_OBJECT = new DirectoryResponseDto("", "", 0L);

    public void createUserDirectory(long userId) {
        String userDir = USER_DIR_TEMPLATE.formatted(userId);
        executeMinioOperation(() -> minioRepository.createUserDirectory(userDir), "with creating directory");
    }

    private String getUsername(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotExistsException("User not found"))
                .getUsername();
    }

    public List<ResourceResponseDto> getDirectoryFiles(String path, long userId) {
        String fullPath = validateAndBuildPath(path, userId);

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

    public DirectoryResponseDto createDirectory(String path, long userId) {
        String fullPath = validateAndBuildPath(path, userId);
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

        String directoryName = extractName(fullPath);

        log.info("{} create directory with name: {}", getUsername(userId), directoryName);
        publisher.publish(getUsername(userId), String.format(Action.CREATE_DIRECTORY.getDescription(), directoryName),
                Action.CREATE_DIRECTORY);

        return new DirectoryResponseDto(responsePath, directoryName, 0L);
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
                ? new DirectoryResponseDto(responsePath, name, calculateDirectorySize(fullPath)) // добавили вычисление
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
            String deletedObjects = deleteDirectoryRecursively(fullPath);
            log.info("{} delete resource with name: {}", getUsername(userId), deletedObjects);
            publisher.publish(getUsername(userId),
                    String.format(Action.DELETE_RESOURCE.getDescription(), deletedObjects),
                    Action.DELETE_RESOURCE);
        } else {
            executeMinioOperation(() -> minioRepository.deleteObject(fullPath), "with deleting file");
            log.info("{} delete resource with name: {}", getUsername(userId), extractName(fullPath));
            publisher.publish(getUsername(userId),
                    String.format(Action.DELETE_RESOURCE.getDescription(), extractName(fullPath)),
                    Action.DELETE_RESOURCE);
        }
    }

    public DownloadResourceDto downloadResource(String path, long userId) {
        if (path.isBlank() || path.startsWith("/")) {
            throw new InvalidResourcePathException(INVALID_PATH_ERROR_MESSAGE);
        }
        String fullPath = USER_DIR_TEMPLATE.formatted(userId) + path;

        Optional<GetObjectResponse> maybeResource = executeMinioOperationIgnoreNotFound(
                () -> minioRepository.getObject(fullPath));
        GetObjectResponse resource = maybeResource.orElseThrow(
                () -> new ResourceNotFoundException(RESOURCE_NOT_FOUND_MESSAGE));

        String resourceName = extractName(fullPath);
        log.info("{} download resource with name: {}", getUsername(userId), resourceName);
        publisher.publish(getUsername(userId), String.format(Action.DOWNLOAD_RESOURCE.getDescription(), resourceName),
                Action.DOWNLOAD_RESOURCE);

        return !fullPath.endsWith("/")
                ? downloadFile(resource, fullPath)
                : downloadZipDirectory(fullPath);
    }

    public List<ResourceResponseDto> searchResource(String query, long userId) {
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

    public List<ResourceResponseDto> uploadResource(String path, List<MultipartFile> files, long userId) {
        String fullPath = USER_DIR_TEMPLATE.formatted(userId) + path;
        executeMinioOperationIgnoreNotFound(() -> minioRepository.statObject(fullPath))
                .orElseThrow(() -> new DirectoryNotFoundException("Directory not found"));

        List<SnowballObject> objects = new ArrayList<>();

        for (MultipartFile file : files) {
            String fileName = extractName(file.getOriginalFilename());
            String uploadFilePath = fullPath + fileName;

            if (executeMinioOperationIgnoreNotFound(() -> minioRepository.statObject(uploadFilePath)).isPresent()) {
                throw new ResourceAlreadyExistsException("File '%s' already exists".formatted(fileName));
            }

            try {
                objects.add(new SnowballObject(uploadFilePath, file.getInputStream(), file.getSize(), null));
            } catch (IOException e) {
                throw new ResourceUploadException("Error reading file '%s'".formatted(fileName));
            }
        }

        executeMinioOperation(() -> minioRepository.uploadSnowballObject(objects), "uploading files");

        List<ResourceResponseDto> result = new ArrayList<>();
        List<String> uploadedFileNames = new ArrayList<>();

        for (MultipartFile file : files) {
            String fileName = extractName(file.getOriginalFilename());
            String responsePath = removeLastSlash(path);
            result.add(new FileResponseDto(responsePath, fileName, file.getSize()));
            uploadedFileNames.add(fileName);
        }

        String uploadedResources = String.join(", ", uploadedFileNames);
        log.info("{} upload resources: {}", getUsername(userId), uploadedResources);
        publisher.publish(getUsername(userId),
                String.format(Action.UPLOAD_RESOURCE.getDescription(), uploadedResources), Action.UPLOAD_RESOURCE);

        return result;
    }

    public void moveToTrash(List<String> paths, long userId) {
        UserApp user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotExistsException("User not found"));
        String userDir = USER_DIR_TEMPLATE.formatted(userId);

        for (String path : paths) {
            String fullPath = userDir + path;

            Optional<StatObjectResponse> maybeResource = executeMinioOperationIgnoreNotFound(
                    () -> minioRepository.statObject(fullPath));

            if (maybeResource.isEmpty()) {
                continue;
            }

            if (fullPath.endsWith("/")) {
                addDirectoryToTrashRecursively(fullPath, userId, user);
            } else {
                addFileToTrash(fullPath, userId, path, user, maybeResource.get().size());
            }
        }
    }

    private void addDirectoryToTrashRecursively(String directoryPath, long userId, UserApp user) {
        List<Result<Item>> items = new ArrayList<>();
        executeMinioOperation(() -> {
            for (Result<Item> itemResult : minioRepository.listObjects(directoryPath, NEED_RECURSIVE)) {
                items.add(itemResult);
            }
        }, "listing objects for trash");

        for (Result<Item> itemResult : items) {
            Item item = executeMinioOperation(itemResult::get);
            String fullItemPath = item.objectName();
            String relativeItemPath = fullItemPath.replaceFirst(USER_DIR_TEMPLATE.formatted(userId), EMPTY);

            if (isMinioDirectoryObject(fullItemPath, item)) {
                long dirSize = calculateDirectorySize(fullItemPath);
                saveTrashRecord(user, relativeItemPath, extractName(fullItemPath), true, null, dirSize);
                executeMinioOperation(() -> minioRepository.deleteObject(fullItemPath), "delete dir marker");
                continue;
            }

            addFileToTrash(fullItemPath, userId, relativeItemPath, user, item.size());
        }

        executeMinioOperation(() -> minioRepository.deleteObject(directoryPath), "delete dir marker");
    }

    private void addFileToTrash(String fullPath, long userId, String relativePath, UserApp user, long size) {
        String trashObjectName = java.util.UUID.randomUUID().toString();

        saveTrashRecord(user, relativePath, extractName(fullPath), false, trashObjectName, size);

        executeMinioOperation(() -> minioRepository.moveObjectToTrashBucket(fullPath, trashObjectName),
                "moving file to trash bucket");

        log.info("Moved file to trash: {}", relativePath);
        publisher.publish(getUsername(userId), String.format(Action.MOVE_TO_TRASH.getDescription(), relativePath),
                Action.MOVE_TO_TRASH);
    }

    private void saveTrashRecord(UserApp user, String path, String name, boolean isDirectory, String trashObjectName,
            long size) {
        if (trashRepository.findByPathAndUserId(path, user.getId()).isEmpty()) {
            Trash trashEntry = Trash.builder()
                    .user(user)
                    .path(path)
                    .name(name)
                    .isDirectory(isDirectory)
                    .trashObjectName(trashObjectName)
                    .size(size) // Сохраняем размер в БД
                    .deletedAt(LocalDateTime.now())
                    .build();
            trashRepository.save(trashEntry);
        }
    }

    public void restoreFromTrash(List<String> paths, long userId) {
        String userDir = USER_DIR_TEMPLATE.formatted(userId);

        for (String requestedPath : paths) {
            List<Trash> trashItemsToRestore = trashRepository.findAllByUserId(userId).stream()
                    .filter(t -> t.getPath().equals(requestedPath) || t.getPath().startsWith(requestedPath))
                    .toList();

            for (Trash trashItem : trashItemsToRestore) {
                String targetFullPath = calculateRestorePath(userDir, requestedPath, trashItem.getPath());

                if (trashItem.isDirectory()) {
                    executeMinioOperation(() -> minioRepository.createEmptyDirectory(targetFullPath),
                            "create dir on restore");
                } else if (trashItem.getTrashObjectName() != null) {
                    executeMinioOperation(() -> minioRepository.restoreObjectFromTrashBucket(
                            trashItem.getTrashObjectName(), targetFullPath), "restoring file");
                }

                trashRepository.delete(trashItem);

                log.info("Restored resource from trash: {}", trashItem.getPath());
                publisher.publish(getUsername(userId),
                        String.format(Action.RESTORE_FROM_TRASH.getDescription(), trashItem.getPath()),
                        Action.RESTORE_FROM_TRASH);
            }
        }
    }

    private String calculateRestorePath(String userDir, String requestedPath, String itemPath) {
        String parentPath;
        if (requestedPath.endsWith("/")) {
            int lastSlash = requestedPath.lastIndexOf('/', requestedPath.length() - 2);
            parentPath = lastSlash == -1 ? "" : requestedPath.substring(0, lastSlash + 1);
        } else {
            int lastSlash = requestedPath.lastIndexOf('/');
            parentPath = lastSlash == -1 ? "" : requestedPath.substring(0, lastSlash + 1);
        }

        boolean parentExists = parentPath.isEmpty() || directoryExists(userDir + parentPath);

        if (parentExists) {
            return userDir + itemPath;
        } else {
            String newRelativePath = itemPath.substring(parentPath.length());
            return userDir + newRelativePath;
        }
    }

    public List<ResourceResponseDto> getTrash(long userId) {
        List<Trash> trashedItems = trashRepository.findAllByUserId(userId);
        List<ResourceResponseDto> result = new ArrayList<>();

        for (Trash trashItem : trashedItems) {
            // Теперь берем размер из БД как для файлов, так и для папок
            long size = trashItem.getSize() != null ? trashItem.getSize() : 0L;

            if (trashItem.isDirectory()) {
                result.add(new DirectoryResponseDto(trashItem.getPath(), trashItem.getName(), size));
            } else {
                result.add(new FileResponseDto(trashItem.getPath(), trashItem.getName(), size));
            }
        }

        return result;
    }

    public void deleteFromTrash(List<String> paths, long userId) {
        for (String requestedPath : paths) {
            List<Trash> trashItemsToDelete = trashRepository.findAllByUserId(userId).stream()
                    .filter(t -> t.getPath().equals(requestedPath) || t.getPath().startsWith(requestedPath))
                    .toList();

            for (Trash trashItem : trashItemsToDelete) {
                if (trashItem.getTrashObjectName() != null) {
                    executeMinioOperation(() -> minioRepository.deleteObjectFromTrashBucket(
                            trashItem.getTrashObjectName()), "deleting file from trash bucket");
                }
                trashRepository.delete(trashItem);
            }

            log.info("Permanently deleted resource from trash: {}", requestedPath);
            publisher.publish(getUsername(userId),
                    String.format("Permanently deleted from trash: %s", requestedPath),
                    Action.DELETE_RESOURCE);
        }
    }

    public void emptyTrash(long userId) {
        List<Trash> trashedItems = trashRepository.findAllByUserId(userId);

        for (Trash trashItem : trashedItems) {
            if (trashItem.getTrashObjectName() != null) {
                executeMinioOperation(() -> minioRepository.deleteObjectFromTrashBucket(
                        trashItem.getTrashObjectName()), "deleting file from trash bucket");
            }
        }

        trashRepository.deleteAll(trashedItems);

        log.info("Emptied trash for user: {}", userId);
        publisher.publish(getUsername(userId), "Emptied whole trash", Action.DELETE_RESOURCE);
    }

    // ================= DOWNLOAD FROM TRASH LOGIC =================

    public DownloadResourceDto downloadFromTrash(String path, long userId) {
        if (path.isBlank() || path.startsWith("/")) {
            throw new InvalidResourcePathException(INVALID_PATH_ERROR_MESSAGE);
        }

        // Пытаемся найти точное совпадение (если это файл)
        Optional<Trash> exactMatch = trashRepository.findByPathAndUserId(path, userId);

        if (exactMatch.isPresent() && !exactMatch.get().isDirectory()) {
            Trash fileInfo = exactMatch.get();

            GetObjectResponse resource = executeMinioOperation(
                    () -> minioRepository.getObjectFromTrashBucket(fileInfo.getTrashObjectName()));

            log.info("{} download resource from trash with name: {}", getUsername(userId), fileInfo.getName());
            publisher.publish(getUsername(userId),
                    String.format(Action.DOWNLOAD_RESOURCE.getDescription(), fileInfo.getName()),
                    Action.DOWNLOAD_RESOURCE);

            return DownloadResourceDto.builder()
                    .resource(new InputStreamResource(resource))
                    .name(fileInfo.getName())
                    .build();
        }

        // Если точное совпадение — это папка, или мы скачиваем папку, которой нет как отдельного маркера
        List<Trash> folderItems = trashRepository.findAllByUserId(userId).stream()
                .filter(t -> t.getPath().startsWith(path) && !t.isDirectory() && t.getTrashObjectName() != null)
                .toList();

        if (folderItems.isEmpty()) {
            throw new ResourceNotFoundException("Resource not found in trash");
        }

        log.info("{} download directory from trash with path: {}", getUsername(userId), path);
        publisher.publish(getUsername(userId), String.format(Action.DOWNLOAD_RESOURCE.getDescription(), path),
                Action.DOWNLOAD_RESOURCE);

        return downloadZipFromTrash(folderItems, path);
    }

    private DownloadResourceDto downloadZipFromTrash(List<Trash> items, String basePath) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try (ZipOutputStream zipOut = new ZipOutputStream(byteArrayOutputStream)) {
            for (Trash item : items) {
                GetObjectResponse fileResponse = executeMinioOperation(
                        () -> minioRepository.getObjectFromTrashBucket(item.getTrashObjectName()));

                String prefixToStrip = basePath.endsWith("/") ? basePath : basePath + "/";
                String entryName = item.getPath().replaceFirst(Pattern.quote(prefixToStrip), EMPTY);

                if (entryName.equals(item.getPath())) {
                    entryName = item.getName();
                }

                zipOut.putNextEntry(new ZipEntry(entryName));
                try (InputStream inputStream = fileResponse) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        zipOut.write(buffer, 0, bytesRead);
                    }
                }
                zipOut.closeEntry();
            }
            zipOut.finish();
        } catch (IOException e) {
            throw new CreateZipException("Error with creating zip archive from trash");
        }

        ByteArrayInputStream zipByteInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        String zipName = extractName(basePath) + ".zip";

        return DownloadResourceDto.builder()
                .resource(new InputStreamResource(() -> zipByteInputStream))
                .name(zipName)
                .build();
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

        String oldPath = fullFromPath.replaceFirst(USER_DIR_PATTERN, EMPTY);
        String newPath = fullToPath.replaceFirst(USER_DIR_PATTERN, EMPTY);
        log.info("{} delete resource with old Path: {} and new Path: {}", getUsername(userId), oldPath, newPath);
        publisher.publish(getUsername(userId), String.format(Action.MOVE_RESOURCE.getDescription(), oldPath, newPath),
                Action.MOVE_RESOURCE);

        return createResponse(fullToPath, userId);
    }


    private ResourceResponseDto renameResource(String fullFromPath, String fullToPath, long userId) {
        if (fullFromPath.endsWith("/")) {
            executeMinioOperation(() -> minioRepository.createEmptyDirectory(fullToPath), "creating target directory");
            copyDirectoryRecursively(fullFromPath, fullToPath);
            deleteDirectoryRecursively(fullFromPath);
        } else {
            executeMinioOperation(() -> minioRepository.copyObject(fullFromPath, fullToPath),
                    "with renaming file");
            executeMinioOperation(() -> minioRepository.deleteObject(fullFromPath),
                    "with deleting file after rename");
        }

        String oldName = extractName(fullFromPath);
        String newName = extractName(fullToPath);
        log.info("{} rename resource with old Name: {} and new Name: {}", getUsername(userId), oldName, newName);
        publisher.publish(getUsername(userId), String.format(Action.RENAME_RESOURCE.getDescription(), oldName, newName),
                Action.RENAME_RESOURCE);

        return createResponse(fullToPath, userId);
    }


    private ResourceResponseDto createResponse(String fullPath, long userId) {
        String responsePath = getResponsePath(fullPath, userId);

        StatObjectResponse stat = executeMinioOperationIgnoreNotFound(() -> minioRepository.statObject(fullPath)).get();

        return fullPath.endsWith("/")
                ? new DirectoryResponseDto(responsePath, extractName(fullPath), calculateDirectorySize(fullPath))
                : new FileResponseDto(responsePath, extractName(fullPath), stat.size());
    }

    private String getResponsePath(String fullPath, long userId) {
        String parentPath = getParentPath(fullPath, userId);
        String parentPathWithoutUserDir = parentPath
                .replaceFirst(USER_DIR_PATTERN, EMPTY);
        return removeLastSlash(parentPathWithoutUserDir);
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

    private String deleteDirectoryRecursively(String path) {
        List<String> objectsToDelete = new ArrayList<>();

        for (Result<Item> itemResult : minioRepository.listObjects(path, NEED_RECURSIVE)) {
            Item item = executeMinioOperationIgnoreNotFound(itemResult::get)
                    .orElseThrow(() -> new DatabaseException("Error retrieving objects for deletion"));
            objectsToDelete.add(item.objectName());
        }

        for (String objectName : objectsToDelete) {
            executeMinioOperation(() -> minioRepository.deleteObject(objectName), "with deleting object");
        }

        objectsToDelete.add(path);

        executeMinioOperation(() -> minioRepository.deleteObject(path), "while deleting empty directory");

        return objectsToDelete.stream().collect(Collectors.joining(", ", "[", "]"));
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

                if (isMinioDirectoryObject(fullPath, item)) {
                    continue;
                }

                GetObjectResponse fileResponse = executeMinioOperation(
                        () -> minioRepository.getObject(item.objectName()));

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

        if (item.isDir()) {
            long dirSize = calculateDirectorySize(item.objectName());
            return new DirectoryResponseDto(parentPath, name, dirSize);
        } else {
            return new FileResponseDto(parentPath, name, item.size());
        }
    }

    private long calculateDirectorySize(String dirFullPath) {
        long totalSize = 0L;
        for (Result<Item> itemResult : minioRepository.listObjects(dirFullPath, NEED_RECURSIVE)) {
            Item item = executeMinioOperation(itemResult::get);
            if (!item.isDir()) {
                totalSize += item.size();
            }
        }
        return totalSize;
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

    private static String validateAndBuildPath(String path, long userId) {
        if (path.isBlank()) {
            return USER_DIR_TEMPLATE.formatted(userId);
        }
        if (path.startsWith("/") || (!path.endsWith("/"))) {
            throw new InvalidPathFolderException(INVALID_PATH_ERROR_MESSAGE);
        }
        return USER_DIR_TEMPLATE.formatted(userId) + path;
    }

    private static String getParentPath(String fullPath, long userId) {
        String rootPath = USER_DIR_TEMPLATE.formatted(userId);
        if (fullPath.equals(rootPath)) {
            return EMPTY;
        }
        return fullPath.substring(0, fullPath.lastIndexOf('/', fullPath.length() - 2) + 1);
    }
}