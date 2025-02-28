package com.iposhka.filestorageapi.dto.responce;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ErrorResponseDto {
    private String message;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    Map<String, String> errors = new HashMap<>();

    public ErrorResponseDto(String message) {
        this.message = message;
    }

    public void putError(String key, String error){
        errors.put(key, error);
    }
}