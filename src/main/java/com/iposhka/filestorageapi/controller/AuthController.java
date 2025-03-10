package com.iposhka.filestorageapi.controller;

import com.iposhka.filestorageapi.docs.auth.LoginApiDocs;
import com.iposhka.filestorageapi.docs.auth.RegistrationApiDocs;
import com.iposhka.filestorageapi.dto.request.UserRequestDto;
import com.iposhka.filestorageapi.dto.responce.UserResponseDto;
import com.iposhka.filestorageapi.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Tag(name = "Authentication", description = "Endpoints for user authentication and registration")
@RestController
@RequiredArgsConstructor
@RequestMapping("api/auth")
public class AuthController {
    private final AuthService authService;

    @LoginApiDocs
    @PostMapping("/sign-in")
    public ResponseEntity<UserResponseDto> signIn(@RequestBody @Valid UserRequestDto userRequestDto,
                                                  HttpServletRequest req) {
        UserResponseDto userResponseDto = authService.login(userRequestDto, req);

        req.getSession().setAttribute("userId", userResponseDto.getId());

        return ResponseEntity.ok(userResponseDto);
    }

    @RegistrationApiDocs
    @PostMapping("/sign-up")
    public ResponseEntity<UserResponseDto> signUp(@RequestBody @Valid UserRequestDto userRequestDto,
                                                  HttpServletRequest request) {
        UserResponseDto userResponseDto = authService.registration(userRequestDto, request);

        request.getSession().setAttribute("userId", userResponseDto.getId());

        return ResponseEntity.created(URI.create("api/user/me")).body(userResponseDto);
    }
}