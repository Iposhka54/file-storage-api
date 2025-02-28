package com.iposhka.filestorageapi.dto.responce;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponseDto {
    private String username;
}