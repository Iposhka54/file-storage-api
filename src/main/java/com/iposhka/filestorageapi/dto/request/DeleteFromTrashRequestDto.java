package com.iposhka.filestorageapi.dto.request;

import java.util.List;
import lombok.Data;

@Data
public class DeleteFromTrashRequestDto {

    private List<String> paths;
}