package com.iposhka.filestorageapi.mapper;

import com.iposhka.filestorageapi.dto.request.UserRequestDto;
import com.iposhka.filestorageapi.dto.responce.UserResponseDto;
import com.iposhka.filestorageapi.model.UserApp;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    UserApp userRequestDtoToAppUser(UserRequestDto dto);

    UserResponseDto appUserToUserResponseDto(UserApp userApp);

    UserResponseDto userRequestDtoToUserResponseDto(UserRequestDto dto);
}