package com.iposhka.filestorageapi.controller;

import com.iposhka.filestorageapi.docs.directory.CreateDirectoryApiDocs;
import com.iposhka.filestorageapi.docs.directory.GetDirectoryResourcesApiDocs;
import com.iposhka.filestorageapi.dto.responce.resourse.DirectoryResponseDto;
import com.iposhka.filestorageapi.dto.responce.resourse.ResourceResponseDto;
import com.iposhka.filestorageapi.service.StorageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Tag(name = "Directories", description = "Manipulations with directories")
@RestController
@RequiredArgsConstructor
@RequestMapping("api/directory")
public class DirectoryController {
    private final StorageService storageService;

    @GetDirectoryResourcesApiDocs
    @GetMapping
    public ResponseEntity<List<ResourceResponseDto>> getDirectoryResources(@RequestParam String path,
                                                                           @SessionAttribute long userId) {
        List<ResourceResponseDto> directoryResources = storageService.getDirectoryFiles(path, userId);
        return ResponseEntity.ok(directoryResources);
    }

    @CreateDirectoryApiDocs
    @PostMapping
    public ResponseEntity<DirectoryResponseDto> createDirectory(@RequestParam String path,
                                                                @SessionAttribute long userId) {
        DirectoryResponseDto directoryResponseDto = storageService.createDirectory(path, userId);
        return ResponseEntity.created(URI.create(path)).body(directoryResponseDto);
    }
}