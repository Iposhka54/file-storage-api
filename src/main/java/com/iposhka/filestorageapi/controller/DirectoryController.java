package com.iposhka.filestorageapi.controller;

import com.iposhka.filestorageapi.dto.responce.resourse.DirectoryResponseDto;
import com.iposhka.filestorageapi.dto.responce.resourse.ResourceResponseDto;
import com.iposhka.filestorageapi.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/directory")
public class DirectoryController {
    private final StorageService storageService;

    @GetMapping
    public ResponseEntity<List<ResourceResponseDto>> getDirectoryResources(@RequestParam String path,
                                                                           @SessionAttribute long userId) {
        List<ResourceResponseDto> directoryResources = storageService.getDirectoryFiles(path, userId);
        return ResponseEntity.ok(directoryResources);
    }

    @PostMapping
    public ResponseEntity<DirectoryResponseDto> createEmptyDirectory(@RequestParam String path,
                                                                     @SessionAttribute long userId) {
        DirectoryResponseDto directoryResponseDto = storageService.createEmptyDirectory(path, userId);
        return ResponseEntity.created(URI.create(path)).body(directoryResponseDto);
    }
}