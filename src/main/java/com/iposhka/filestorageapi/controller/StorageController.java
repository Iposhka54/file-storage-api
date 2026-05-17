package com.iposhka.filestorageapi.controller;

import com.iposhka.filestorageapi.dto.responce.StorageInfoDto;
import com.iposhka.filestorageapi.service.StorageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;

@Tag(name = "Resources", description = "Endpoints for resources manipulations")
@RestController
@RequiredArgsConstructor
@RequestMapping("api/storage")
public class StorageController {

    private final StorageService storageService;

    @GetMapping("/info")
    public ResponseEntity<StorageInfoDto> getStorageInfo(@SessionAttribute long userId) {
        StorageInfoDto info = storageService.getStorageInfo(userId);
        return ResponseEntity.ok(info);
    }
}
