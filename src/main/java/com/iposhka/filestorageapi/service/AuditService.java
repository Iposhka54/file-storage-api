package com.iposhka.filestorageapi.service;

import com.iposhka.filestorageapi.dto.responce.AuditResponseDto;
import com.iposhka.filestorageapi.model.UserActionAudit;
import com.iposhka.filestorageapi.repository.UserActionAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditService {

    private final UserActionAuditRepository userActionAuditRepository;

    public Page<AuditResponseDto> getAllAuditRecords(Pageable pageable) {
        Page<UserActionAudit> auditPage = userActionAuditRepository.findAll(pageable);
        return auditPage.map(this::toAuditResponseDto);
    }

    public Page<AuditResponseDto> getAuditRecordsByUsername(String username, Pageable pageable) {
        Page<UserActionAudit> auditPage = userActionAuditRepository.findByUsernameContainingIgnoreCase(username,
                pageable);
        return auditPage.map(this::toAuditResponseDto);
    }

    public Page<AuditResponseDto> getAuditRecordsByActionType(Integer actionType, Pageable pageable) {
        Page<UserActionAudit> auditPage = userActionAuditRepository.findByActionType(actionType, pageable);
        return auditPage.map(this::toAuditResponseDto);
    }

    public Page<AuditResponseDto> getAuditRecordsByUsernameAndActionType(String username, Integer actionType,
            Pageable pageable) {
        Page<UserActionAudit> auditPage = userActionAuditRepository.findByUsernameContainingIgnoreCaseAndActionType(
                username, actionType, pageable);
        return auditPage.map(this::toAuditResponseDto);
    }

    private AuditResponseDto toAuditResponseDto(UserActionAudit audit) {
        return AuditResponseDto.builder()
                .id(audit.getId())
                .username(audit.getUsername())
                .action(audit.getAction())
                .actionType(audit.getActionType())
                .actionTime(audit.getActionTime())
                .build();
    }
}

