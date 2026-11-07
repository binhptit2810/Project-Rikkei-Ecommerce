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
 * Phụ trách: Cao Chí Thiện
 * Task ID: TSK-AUTH-00 - Hoàn thiện xác thực JWT, Opaque Token và RBAC
 */
// [TSK-D02-CCT]
@Getter
public class CustomUserDetails extends User {

    private final Long userId;
    private final Role role;
    private final boolean emailVerified;

    public CustomUserDetails(Long userId, String email, String password, Role role, boolean enabled, boolean emailVerified) {
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
        this.emailVerified = emailVerified;
    }

    public CustomUserDetails(Long userId, String email, String password, Role role, boolean enabled) {
        this(userId, email, password, role, enabled, true);
    }

    public CustomUserDetails(Long userId, String email, String password, Role role, boolean enabled, Collection<? extends GrantedAuthority> authorities) {
        super(email, password, enabled, true, true, true, authorities);
        this.userId = userId;
        this.role = role;
        this.emailVerified = true;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public Long getId() {
        return userId;
    }
}
