package com.iposhka.filestorageapi.mapper;

import com.iposhka.filestorageapi.dto.request.UserRequestDto;
import com.iposhka.filestorageapi.dto.responce.UserResponseDto;
import com.iposhka.filestorageapi.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    User userRequestDtoToAppUser(UserRequestDto dto);

    UserResponseDto appUserToUserResponseDto(User user);

    UserResponseDto userRequestDtoToUserResponseDto(UserRequestDto dto);
}