package com.iposhka.filestorageapi.integration.service;

import com.iposhka.filestorageapi.dto.request.UserRequestDto;
import com.iposhka.filestorageapi.dto.responce.UserResponseDto;
import com.iposhka.filestorageapi.exception.UserAlreadyExistsException;
import com.iposhka.filestorageapi.integration.IntegrationTestBase;
import com.iposhka.filestorageapi.model.AppUser;
import com.iposhka.filestorageapi.repository.MinioRepository;
import com.iposhka.filestorageapi.repository.UserRepository;
import com.iposhka.filestorageapi.service.AuthService;
import com.iposhka.filestorageapi.service.StorageService;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceIT extends IntegrationTestBase {
    @MockitoBean
    private MinioClient minioClient;
    @MockitoBean
    private MinioRepository minioRepository;
    @MockitoBean
    private StorageService storageService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuthService authService;
    @Autowired
    private UserRepository userRepository;

    UserRequestDto yaroslav = UserRequestDto.builder()
            .username("Yaroslav")
            .password("12345678")
            .build();

    @Nested
    @DisplayName("Tests for registration user")
    class RegistrationTests {

        UserRequestDto yaroslav = UserRequestDto.builder()
                .username("Yaroslav")
                .password("12345678")
                .build();

        @Test
        @DisplayName("Success registration")
        public void registrationSuccess() {
            MockHttpServletRequest req = new MockHttpServletRequest();
            UserResponseDto actualResult = authService.registration(yaroslav, req);

            Optional<AppUser> maybeYaroslav = userRepository.findByUsername(yaroslav.getUsername());
            assertTrue(maybeYaroslav.isPresent(), "User should be saved in the database");

            AppUser yaroslav = maybeYaroslav.get();
            assertEquals(yaroslav.getUsername(), actualResult.getUsername(), "Usernames should match");

            SecurityContext context = (SecurityContext) req.getSession().getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            assertNotNull(context, "Context should be set in session");
            assertEquals(context.getAuthentication().getName(), yaroslav.getUsername(), "Usernames should match");

            Mockito.verify(storageService, Mockito.times(1)).createUserDirectory(actualResult.getId());
        }

        @Test
        @DisplayName("User already exists")
        public void registrationUserAlreadyExists() {
            UserRequestDto existingUserRequest = UserRequestDto.builder()
                    .username("Yaroslav")
                    .password("12345678")
                    .build();

            authService.registration(existingUserRequest, new MockHttpServletRequest());

            UserRequestDto newUserRequest = UserRequestDto.builder()
                    .username("Yaroslav")
                    .password("newpassword")
                    .build();

            assertThrows(UserAlreadyExistsException.class, () -> {
                authService.registration(newUserRequest, new MockHttpServletRequest());
            });
        }
    }

    @Nested
    @DisplayName("Tests for login user")
    class LoginTests {
        private AppUser user;

        @BeforeEach
        public void saveUser() {
            user = new AppUser();
            user.setUsername("Yaroslav");
            user.setPassword(passwordEncoder.encode("12345678"));
            userRepository.save(user);
        }

        @Test
        @DisplayName("Successful login")
        public void loginUserSuccess() {
            MockHttpServletRequest req = new MockHttpServletRequest();
            UserResponseDto userDto = authService.login(yaroslav, req);

            assertEquals(userDto.getUsername(), user.getUsername(), "Username should match");
            assertEquals(user.getId(), userDto.getId(), "User id should match");

            SecurityContext context = (SecurityContext) req.getSession().getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            assertNotNull(context, "Context should be set in session");
            assertEquals(context.getAuthentication().getName(), yaroslav.getUsername(), "Usernames should match");
        }

        @Test
        @DisplayName("Login failed because bad credentials")
        public void loginWithBadCredentials() {
            UserRequestDto userRequestDto = UserRequestDto.builder()
                    .username("Yaroslav")
                    .password("anyPassword")
                    .build();

            assertThrows(BadCredentialsException.class,
                    () -> authService.login(userRequestDto, new MockHttpServletRequest()));
        }
    }
}