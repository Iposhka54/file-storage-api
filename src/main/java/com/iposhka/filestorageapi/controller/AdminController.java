package com.iposhka.filestorageapi.controller;

import com.iposhka.filestorageapi.dto.responce.AuditResponseDto;
import com.iposhka.filestorageapi.service.AuditService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin", description = "Admin endpoints for viewing user actions audit")
@RestController
@RequiredArgsConstructor
@RequestMapping("api/admin")
public class AdminController {

    private final AuditService auditService;

    @GetMapping("/audit")
    public ResponseEntity<Page<AuditResponseDto>> getAuditRecords(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer actionType,
            @PageableDefault(size = 20, sort = "actionTime", direction = Sort.Direction.DESC) Pageable pageable) {
        
        Page<AuditResponseDto> auditPage;
        
        if (username != null && actionType != null) {
            auditPage = auditService.getAuditRecordsByUsernameAndActionType(username, actionType, pageable);
        } else if (username != null) {
            auditPage = auditService.getAuditRecordsByUsername(username, pageable);
        } else if (actionType != null) {
            auditPage = auditService.getAuditRecordsByActionType(actionType, pageable);
        } else {
            auditPage = auditService.getAllAuditRecords(pageable);
        }
        
        return ResponseEntity.ok(auditPage);
    }
}

