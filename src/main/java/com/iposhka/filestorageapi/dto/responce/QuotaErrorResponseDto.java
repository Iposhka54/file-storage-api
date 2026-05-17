package com.iposhka.filestorageapi.dto.responce;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuotaErrorResponseDto {

    private String message;
    private List<String> rejectedFiles;
}
