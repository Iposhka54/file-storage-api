package com.iposhka.filestorageapi.controller;

import com.iposhka.filestorageapi.dto.responce.resourse.DownloadResourceDto;
import com.iposhka.filestorageapi.dto.responce.resourse.ResourceResponseDto;
import com.iposhka.filestorageapi.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/resource")
public class ResourceController {
    private final StorageService storageService;

    @GetMapping
    public ResponseEntity<ResourceResponseDto> getInfoAboutResource(@RequestParam String path,
                                                                    @SessionAttribute long userId) {
        ResourceResponseDto resource = storageService.getInfoAboutResource(path, userId);

        return ResponseEntity.ok().body(resource);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteResource(@RequestParam String path,
                                                              @SessionAttribute long userId) {
        storageService.deleteResource(path, userId);
        return ResponseEntity.noContent().build();
    }

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

    @GetMapping("/search")
    public ResponseEntity<List<ResourceResponseDto>> searchResource(@RequestParam String query,
                                                                    @SessionAttribute long userId) {
        List<ResourceResponseDto> resources = storageService.searchResource(query, userId);
        return ResponseEntity.ok().body(resources);
    }

    @GetMapping("/move")
    public ResponseEntity<ResourceResponseDto> moveResource(@RequestParam String from,
                                                            @RequestParam String to,
                                                            @SessionAttribute long userId) {
        ResourceResponseDto resourceResponseDto = storageService.moveOrRenameResource(from, to, userId);
        return ResponseEntity.ok(resourceResponseDto);
    }

    @PostMapping
    public ResponseEntity<List<ResourceResponseDto>> uploadResource(@RequestParam String path,
                                                                    @RequestPart("object") List<MultipartFile> files,
                                                                    @SessionAttribute long userId) {
        List<ResourceResponseDto> resources = storageService.uploadResource(path, files, userId);
        return ResponseEntity.created(URI.create(path))
                .body(resources);
    }
}