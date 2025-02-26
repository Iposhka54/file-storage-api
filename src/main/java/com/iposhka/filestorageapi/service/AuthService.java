package com.iposhka.filestorageapi.service;

import com.iposhka.filestorageapi.dto.request.UserRequestDto;
import com.iposhka.filestorageapi.dto.responce.UserResponseDto;
import com.iposhka.filestorageapi.exception.DatabaseException;
import com.iposhka.filestorageapi.exception.UserAlreadyExistException;
import com.iposhka.filestorageapi.mapper.UserMapper;
import com.iposhka.filestorageapi.model.User;
import com.iposhka.filestorageapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserResponseDto create(UserRequestDto userRequestDto) {
        userRequestDto.setPassword(passwordEncoder.encode(userRequestDto.getPassword()));
        User user = userMapper.toEntity(userRequestDto);

        try {
            userRepository.save(user);
            log.info("User with username {} saved to the database", user.getUsername());
        } catch (DataIntegrityViolationException e) {
            throw new UserAlreadyExistException("User with that username already exists");
        }catch (Exception e){
            throw new DatabaseException("Any problems with database");
        }

        return userMapper.toDto(user);
    }
}