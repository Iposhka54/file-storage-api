package com.iposhka.filestorageapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequestDto {
    @NotBlank(message = "Username cannot be empty or just whitespace.")
    @Size(min = 3, max = 32, message = "Username must be between 3 and 32 characters.")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "Username can only contain letters and numbers.")
    private String username;

    @NotBlank(message = "Password cannot be empty or just whitespace.")
    @Size(min = 8, max = 32, message = "Password must be between 8 and 32 characters.")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "Password can only contain letters and numbers.")
    private String password;
}