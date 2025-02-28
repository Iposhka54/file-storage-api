package com.iposhka.filestorageapi.dto.responce;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponseDto {
    @JsonIgnore
    private int id;

    private String username;
}