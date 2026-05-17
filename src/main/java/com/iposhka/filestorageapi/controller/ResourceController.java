package com.iposhka.filestorageapi.controller;

import com.iposhka.filestorageapi.docs.resource.*;
import com.iposhka.filestorageapi.dto.request.DeleteFromTrashRequestDto;
import com.iposhka.filestorageapi.dto.request.MoveToTrashRequestDto;
import com.iposhka.filestorageapi.dto.request.RestoreFromTrashRequestDto;
import com.iposhka.filestorageapi.dto.responce.resourse.DownloadResourceDto;
import com.iposhka.filestorageapi.dto.responce.resourse.ResourceResponseDto;
import com.iposhka.filestorageapi.service.StorageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;

@Tag(name = "Resources", description = "Endpoints for resources manipulations")
@RestController
@RequiredArgsConstructor
@RequestMapping("api/resource")
public class ResourceController {

    private final StorageService storageService;

    @GetResourceInfoApiDocs
    @GetMapping
    public ResponseEntity<ResourceResponseDto> getInfoAboutResource(@RequestParam String path,
            @SessionAttribute long userId) {
        ResourceResponseDto resource = storageService.getInfoAboutResource(path, userId);

        return ResponseEntity.ok().body(resource);
    }

    @DeleteResourceApiDocs
    @DeleteMapping
    public ResponseEntity<Void> deleteResource(@RequestParam String path,
            @SessionAttribute long userId) {
        storageService.deleteResource(path, userId);
        return ResponseEntity.noContent().build();
    }

    @DownloadResourceApiDocs
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadResource(@RequestParam String path,
            @SessionAttribute long userId) {
        DownloadResourceDto downloadResourceDto = storageService.downloadResource(path, userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + downloadResourceDto.getName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(downloadResourceDto.getResource());
    }

    @SearchResourceApiDocs
    @GetMapping("/search")
    public ResponseEntity<List<ResourceResponseDto>> searchResource(@RequestParam String query,
            @SessionAttribute long userId) {
        List<ResourceResponseDto> resources = storageService.searchResource(query, userId);
        return ResponseEntity.ok().body(resources);
    }

    @MoveOrRenameResourceApiDocs
    @GetMapping("/move")
    public ResponseEntity<ResourceResponseDto> moveResource(@RequestParam String from,
            @RequestParam String to,
            @SessionAttribute long userId) {
        ResourceResponseDto resourceResponseDto = storageService.moveOrRenameResource(from, to, userId);
        return ResponseEntity.ok(resourceResponseDto);
    }

    @UploadResourceApiDocs
    @PostMapping
    public ResponseEntity<List<ResourceResponseDto>> uploadResource(@RequestParam String path,
            @RequestPart("object") List<MultipartFile> files,
            @SessionAttribute long userId) {
        List<ResourceResponseDto> resources = storageService.uploadResource(path, files, userId);
        return ResponseEntity.created(URI.create(path))
                .body(resources);
    }

    @PostMapping("/trash")
    public ResponseEntity<Void> moveToTrash(@RequestBody MoveToTrashRequestDto requestDto,
            @SessionAttribute long userId) {
        storageService.moveToTrash(requestDto.getPaths(), userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/trash/restore")
    public ResponseEntity<Void> restoreFromTrash(@RequestBody RestoreFromTrashRequestDto requestDto,
            @SessionAttribute long userId) {
        storageService.restoreFromTrash(requestDto.getPaths(), userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/trash")
    public ResponseEntity<List<ResourceResponseDto>> getTrash(@SessionAttribute long userId) {
        List<ResourceResponseDto> trashedResources = storageService.getTrash(userId);
        return ResponseEntity.ok().body(trashedResources);
    }

    @GetMapping("/trash/download")
    public ResponseEntity<Resource> downloadFromTrash(@RequestParam String path,
            @SessionAttribute long userId) {
        DownloadResourceDto downloadResourceDto = storageService.downloadFromTrash(path, userId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + downloadResourceDto.getName() + "\"")
                .body(downloadResourceDto.getResource());
    }

    @DeleteMapping("/trash/items")
    public ResponseEntity<Void> deleteFromTrash(@RequestBody DeleteFromTrashRequestDto requestDto,
            @SessionAttribute long userId) {
        storageService.deleteFromTrash(requestDto.getPaths(), userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/trash")
    public ResponseEntity<Void> emptyTrash(@SessionAttribute long userId) {
        storageService.emptyTrash(userId);
        return ResponseEntity.noContent().build();
    }
}