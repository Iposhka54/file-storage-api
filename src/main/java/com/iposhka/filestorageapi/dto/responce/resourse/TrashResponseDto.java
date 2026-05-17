package com.iposhka.filestorageapi.dto.responce.resourse;

import com.iposhka.filestorageapi.dto.ResourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TrashResponseDto {
    private Long id;
    private String path;
    private String name;
    private ResourceType type;
    private LocalDateTime deletedAt;
}
