package com.iposhka.filestorageapi.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iposhka.filestorageapi.dto.responce.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AuthenticationEntryPoint implements org.springframework.security.web.AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;
    private static final String AUTHENTICATION_MESSAGE = "User is not authenticated";
    private static final ErrorResponseDto AUTHENTICATION_ERROR = new ErrorResponseDto(AUTHENTICATION_MESSAGE);

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        objectMapper.writeValue(response.getWriter(), AUTHENTICATION_ERROR);
    }
}
