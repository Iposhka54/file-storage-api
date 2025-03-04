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
    public ResponseEntity<ResourceResponseDto> deleteResource(@RequestParam String path,
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
}