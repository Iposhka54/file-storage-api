package com.iposhka.filestorageapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("api/directory")
public class DirectoryController {

    @PostMapping
    public ResponseEntity<?> createEmptyDirectory(@RequestParam String path) {

        return ResponseEntity.created(URI.create(path)).body(null);
    }
}