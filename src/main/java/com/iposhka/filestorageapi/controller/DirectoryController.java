package com.iposhka.filestorageapi.controller;

import com.iposhka.filestorageapi.dto.responce.resourse.DirectoryResponseDto;
import com.iposhka.filestorageapi.service.StorageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/directory")
public class DirectoryController {
    private final StorageService storageService;

    @PostMapping
    public ResponseEntity<DirectoryResponseDto> createEmptyDirectory(@RequestParam String path,
                                                                     HttpServletRequest request) {
        long userId = (long) request.getSession().getAttribute("userId");
        DirectoryResponseDto directoryResponseDto = storageService.createEmptyDirectory(path, userId);
        return ResponseEntity.created(URI.create(path)).body(directoryResponseDto);
    }
}