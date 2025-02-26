package com.iposhka.filestorageapi.mapper;

import com.iposhka.filestorageapi.dto.request.UserRequestDto;
import com.iposhka.filestorageapi.dto.responce.UserResponseDto;
import com.iposhka.filestorageapi.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toEntity(UserRequestDto dto);

    UserResponseDto toDto(User user);
}
