package com.iposhka.filestorageapi.dto.responce;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuditResponseDto {
    private UUID id;
    private String username;
    private String action;
    private Integer actionType;
    private LocalDateTime actionTime;
}

