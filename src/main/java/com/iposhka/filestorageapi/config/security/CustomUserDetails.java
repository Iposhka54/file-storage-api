package com.iposhka.filestorageapi.config.security;

import com.iposhka.filestorageapi.model.Role;
import com.iposhka.filestorageapi.model.UserApp;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

import static com.iposhka.filestorageapi.model.Role.USER;

@Getter
@Setter
@AllArgsConstructor
public class CustomUserDetails implements UserDetails {
    private UserApp userApp;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Role role = userApp.getRole() != null ? userApp.getRole() : USER;
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return userApp.getPassword();
    }

    @Override
    public String getUsername() {
        return userApp.getUsername();
    }
}
