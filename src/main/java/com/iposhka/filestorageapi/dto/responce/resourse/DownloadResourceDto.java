package com.iposhka.filestorageapi.dto.responce.resourse;

import lombok.Builder;
import lombok.Data;
import org.springframework.core.io.Resource;

@Data
@Builder
public class DownloadResourceDto {
    private Resource resource;
    private String name;
}