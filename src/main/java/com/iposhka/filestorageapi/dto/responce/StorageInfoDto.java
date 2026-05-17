package com.iposhka.filestorageapi.dto.responce;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StorageInfoDto {

    private long totalSpace;
    private long usedSpace;
    private long trashSpace;
    private long activeSpace;
}