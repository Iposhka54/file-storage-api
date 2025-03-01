package com.iposhka.filestorageapi.service;

import com.iposhka.filestorageapi.dto.request.UserRequestDto;
import com.iposhka.filestorageapi.dto.responce.UserResponseDto;
import com.iposhka.filestorageapi.exception.DatabaseException;
import com.iposhka.filestorageapi.exception.UserAlreadyExistsException;
import com.iposhka.filestorageapi.exception.UserNotExistsException;
import com.iposhka.filestorageapi.mapper.UserMapper;
import com.iposhka.filestorageapi.model.AppUser;
import com.iposhka.filestorageapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Transactional
    public UserResponseDto register(UserRequestDto userRequestDto) {
        userRequestDto.setPassword(passwordEncoder.encode(userRequestDto.getPassword()));

        AppUser user = userMapper.toEntity(userRequestDto);
        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new UserAlreadyExistsException("User with that username already exists");
        } catch (Exception e) {
            throw new DatabaseException("Any problem with database when register");
        }
        return userMapper.toDto(user);
    }

    public long getIdByUsername(String username) {
        try {
            return userRepository.findByUsername(username)
                    .orElseThrow(() -> new UserNotExistsException("User with username '%s' not exists".formatted(username)))
                    .getId();
        } catch (Exception e){
            throw new DatabaseException("Any problem with database");
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .map(user -> User.withUsername(username)
                        .password(user.getPassword())
                        .authorities(Collections.emptyList())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Failed to retrieve user: " + username));
    }
}
