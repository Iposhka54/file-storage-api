package com.iposhka.filestorageapi.controller;

import com.iposhka.filestorageapi.dto.responce.UserResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/user")
public class UserController {
    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getUser(@AuthenticationPrincipal UserDetails userDetails) {
        UserResponseDto userResponseDto = UserResponseDto.builder()
                .username(userDetails.getUsername())
                .build();

        return ResponseEntity.ok(userResponseDto);
    }
}