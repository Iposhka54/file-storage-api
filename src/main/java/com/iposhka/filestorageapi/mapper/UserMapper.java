package com.iposhka.filestorageapi.mapper;

import com.iposhka.filestorageapi.dto.request.UserRequestDto;
import com.iposhka.filestorageapi.dto.responce.UserResponseDto;
import com.iposhka.filestorageapi.model.AppUser;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    AppUser userRequestDtoToAppUser(UserRequestDto dto);

    UserResponseDto appUserToUserResponseDto(AppUser appUser);

    UserResponseDto userRequestDtoToUserResponseDto(UserRequestDto dto);
}