package com.example.ecommerce.security;

import com.example.ecommerce.entity.Role;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.List;

/**
 * CustomUserDetails mở rộng từ Spring Security User,
 * bổ sung userId và role enum để phục vụ sinh và xác minh JWT claims.
 */
@Getter
public class CustomUserDetails extends User {

    private final Long userId;
    private final Role role;

    public CustomUserDetails(Long userId, String email, String password, Role role, boolean enabled) {
        super(
                email,
                password,
                enabled,
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
        this.userId = userId;
        this.role = role;
    }

    public CustomUserDetails(Long userId, String email, String password, Role role, boolean enabled, Collection<? extends GrantedAuthority> authorities) {
        super(email, password, enabled, true, true, true, authorities);
        this.userId = userId;
        this.role = role;
    }

    public Long getId() {
        return userId;
    }
}
