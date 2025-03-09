package com.iposhka.filestorageapi.service;

import com.iposhka.filestorageapi.dto.request.UserRequestDto;
import com.iposhka.filestorageapi.dto.responce.UserResponseDto;
import com.iposhka.filestorageapi.exception.DatabaseException;
import com.iposhka.filestorageapi.exception.UserAlreadyExistsException;
import com.iposhka.filestorageapi.exception.UserNotExistsException;
import com.iposhka.filestorageapi.mapper.UserMapper;
import com.iposhka.filestorageapi.model.AppUser;
import com.iposhka.filestorageapi.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final UserService userService;
    private final StorageService storageService;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;

    public UserResponseDto login(UserRequestDto userRequestDto, HttpServletRequest req) {

        SecurityContext context = authenticateUser(userRequestDto);

        setSecurityContextInSession(req, context);

        return setIdInUserResponseDto(userRequestDto);
    }

    @NotNull
    private UserResponseDto setIdInUserResponseDto(UserRequestDto userRequestDto) {
        Optional<AppUser> maybeUser = userRepository.findByUsername(userRequestDto.getUsername());
        AppUser user = maybeUser.orElseThrow(() -> new UserNotExistsException("User not found"));

        UserResponseDto userResponseDto = userMapper.userRequestDtoToUserResponseDto(userRequestDto);
        userResponseDto.setId(user.getId());
        return userResponseDto;
    }

    public UserResponseDto registration(UserRequestDto userRequestDto, HttpServletRequest request) {
        if (userRepository.existsByUsername(userRequestDto.getUsername())) {
            throw new UserAlreadyExistsException("User with that username already exists");
        }

        userRequestDto.setPassword(passwordEncoder.encode(userRequestDto.getPassword()));

        AppUser user = userMapper.userRequestDtoToAppUser(userRequestDto);
        try {
            userRepository.save(user);
        } catch (Exception e) {
            throw new DatabaseException("Any problem with database when register");
        }

        UserResponseDto userResponseDto = userMapper.appUserToUserResponseDto(user);

        UserDetails userDetails = userService.loadUserByUsername(userResponseDto.getUsername());

        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(authentication);

        setSecurityContextInSession(request, context);

        storageService.createUserDirectory(userResponseDto.getId());

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