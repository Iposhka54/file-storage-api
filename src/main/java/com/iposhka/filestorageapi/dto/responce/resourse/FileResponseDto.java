package com.iposhka.filestorageapi.dto.responce.resourse;

import com.iposhka.filestorageapi.dto.ResourceType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = false)
@Data
public class FileResponseDto extends ResourceResponseDto {
    private long size;

    public FileResponseDto(String path, String name, long size) {
        this.path = path;
        this.name = name;
        this.size = size;
        this.type = ResourceType.FILE;
    }
}