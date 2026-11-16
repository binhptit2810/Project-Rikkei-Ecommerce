package com.example.ecommerce.security.oauth2;

import com.example.ecommerce.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * CustomOidcUser đại diện cho Principal sau khi xác thực OpenID Connect với Google.
 * Bọc đối tượng OidcUser chuẩn từ Spring Security và User entity nội bộ.
 * Phụ trách: Cao Chí Thiện
 * Task ID: TSK-D15-CCT - Tích hợp đăng nhập nhanh bằng tài khoản Google
 */
@Getter
public class CustomOidcUser implements OidcUser {

    private final OidcUser delegate;
    private final User userEntity;

    public CustomOidcUser(OidcUser delegate, User userEntity) {
        this.delegate = delegate;
        this.userEntity = userEntity;
    }

    @Override
    public Map<String, Object> getClaims() {
        return delegate.getClaims();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return delegate.getUserInfo();
    }

    @Override
    public OidcIdToken getIdToken() {
        return delegate.getIdToken();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return delegate.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (userEntity != null && userEntity.getRole() != null) {
            return List.of(new SimpleGrantedAuthority("ROLE_" + userEntity.getRole().name()));
        }
        return delegate.getAuthorities();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    public Long getInternalUserId() {
        return userEntity != null ? userEntity.getId() : null;
    }

    public String getEmail() {
        return delegate.getEmail();
    }
}

