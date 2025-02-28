package com.iposhka.filestorageapi.mapper;

import com.iposhka.filestorageapi.dto.request.UserRequestDto;
import com.iposhka.filestorageapi.dto.responce.UserResponseDto;
import com.iposhka.filestorageapi.model.AppUser;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    AppUser toEntity(UserRequestDto dto);

    UserResponseDto toDto(AppUser appUser);

    UserResponseDto toDto(UserRequestDto dto);
}
