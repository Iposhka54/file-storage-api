package com.iposhka.filestorageapi.controller;

import com.iposhka.filestorageapi.dto.responce.resourse.ResourceResponseDto;
import com.iposhka.filestorageapi.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

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
}