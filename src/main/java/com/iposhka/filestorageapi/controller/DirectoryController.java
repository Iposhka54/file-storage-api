package com.iposhka.filestorageapi.controller;

import com.iposhka.filestorageapi.dto.responce.resourse.DirectoryResponseDto;
import com.iposhka.filestorageapi.dto.responce.resourse.ResourceResponseDto;
import com.iposhka.filestorageapi.service.DirectoryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/directory")
public class DirectoryController {
    private final DirectoryService directoryService;

    @GetMapping
    public ResponseEntity<List<ResourceResponseDto>> getDirectoryResources(@RequestParam String path,
                                                                      HttpServletRequest request) {
        long userId = getUserId(request);
        List<ResourceResponseDto> directoryResources = directoryService.listDirectoryContents(path, userId);
        return ResponseEntity.ok(directoryResources);
    }

    @PostMapping
    public ResponseEntity<DirectoryResponseDto> createEmptyDirectory(@RequestParam String path,
                                                                     HttpServletRequest request) {
        long userId = getUserId(request);
        DirectoryResponseDto directoryResponseDto = directoryService.createEmptyDirectory(path, userId);
        return ResponseEntity.created(URI.create(path)).body(directoryResponseDto);
    }

    private static long getUserId(HttpServletRequest request) {
        return (long) request.getSession().getAttribute("userId");
    }
}