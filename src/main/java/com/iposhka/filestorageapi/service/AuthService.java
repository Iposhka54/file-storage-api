package com.iposhka.filestorageapi.service;

import com.iposhka.filestorageapi.dto.request.UserRequestDto;
import com.iposhka.filestorageapi.dto.responce.UserResponseDto;
import com.iposhka.filestorageapi.exception.UserAlreadyExistsException;
import com.iposhka.filestorageapi.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final UserService userService;
    private final DirectoryService directoryService;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;

    public UserResponseDto login(UserRequestDto userRequestDto, HttpServletRequest req) {

        SecurityContext context = authenticateUser(userRequestDto);

        setSecurityContextInSession(req, context);

        return userMapper.toDto(userRequestDto);
    }

    public UserResponseDto registration(UserRequestDto userRequestDto, HttpServletRequest request) {
        if (userService.existsByUsername(userRequestDto.getUsername())) {
            throw new UserAlreadyExistsException("User with that username already exists");
        }

        UserResponseDto userResponseDto = userService.register(userRequestDto);

        UserDetails userDetails = userService.loadUserByUsername(userResponseDto.getUsername());

        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(authentication);

        setSecurityContextInSession(request, context);

        directoryService.createUserFolder(userResponseDto.getId());

        return userResponseDto;
    }

    private SecurityContext authenticateUser(UserRequestDto userRequestDto) {
        Authentication token = new UsernamePasswordAuthenticationToken(userRequestDto.getUsername(), userRequestDto.getPassword());

        Authentication authentication = authenticationManager.authenticate(token);

        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(authentication);
        return context;
    }

    private static void setSecurityContextInSession(HttpServletRequest req, SecurityContext context) {
        HttpSession session = req.getSession(true);

        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }
}