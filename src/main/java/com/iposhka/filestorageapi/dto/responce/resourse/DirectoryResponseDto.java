package com.iposhka.filestorageapi.dto.responce.resourse;

import com.iposhka.filestorageapi.dto.ResourceType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = false)
@Data
@NoArgsConstructor
public class DirectoryResponseDto extends ResourceResponseDto {

    public DirectoryResponseDto(String path, String name) {
        this.path = path;
        this.name = name;
        this.type = ResourceType.DIRECTORY;
    }
}