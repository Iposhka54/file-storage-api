package com.iposhka.filestorageapi.dto.responce.resourse;

import com.iposhka.filestorageapi.dto.ResourceType;
import lombok.Data;

@Data
public abstract class ResourceResponseDto {
    protected String path;
    protected String name;
    protected ResourceType type;
}