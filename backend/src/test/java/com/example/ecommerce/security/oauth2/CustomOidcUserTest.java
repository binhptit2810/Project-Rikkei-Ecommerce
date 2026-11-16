package com.example.ecommerce.security.oauth2;

import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit Test cho CustomOidcUser.
 * Phụ trách: Cao Chí Thiện
 * Task ID: TSK-D15-CCT
 */
class CustomOidcUserTest {

    private OidcUser createMockOidcUser() {
        OidcIdToken idToken = new OidcIdToken(
                "mock-id-token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("sub", "google-sub-12345", "email", "john.doe@example.com", "name", "John Doe")
        );
        OidcUserInfo userInfo = new OidcUserInfo(Map.of("email", "john.doe@example.com", "name", "John Doe"));

        return new DefaultOidcUser(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
                idToken,
                userInfo,
                "name"
        );
    }

    @Test
    @DisplayName("1. CustomOidcUser ủy quyền chính xác các phương thức OIDC cơ bản")
    void customOidcUser_delegatesBasicMethodsCorrectly() {
        OidcUser delegate = createMockOidcUser();
        User user = User.builder()
                .id(101L)
                .email("john.doe@example.com")
                .role(Role.CUSTOMER)
                .build();

        CustomOidcUser customOidcUser = new CustomOidcUser(delegate, user);

        assertThat(customOidcUser.getDelegate()).isSameAs(delegate);
        assertThat(customOidcUser.getUserEntity()).isSameAs(user);
        assertThat(customOidcUser.getName()).isEqualTo("John Doe");
        assertThat(customOidcUser.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(customOidcUser.getInternalUserId()).isEqualTo(101L);
        assertThat(customOidcUser.getClaims()).containsKey("sub");
        assertThat(customOidcUser.getAttributes()).containsKey("email");
        assertThat(customOidcUser.getIdToken()).isNotNull();
        assertThat(customOidcUser.getUserInfo()).isNotNull();
    }

    @Test
    @DisplayName("2. getAuthorities khi userEntity có Role.CUSTOMER -> Trả về ROLE_CUSTOMER")
    void getAuthorities_whenUserEntityHasRole_returnsRoleAuthority() {
        OidcUser delegate = createMockOidcUser();
        User user = User.builder()
                .id(102L)
                .email("customer@example.com")
                .role(Role.CUSTOMER)
                .build();

        CustomOidcUser customOidcUser = new CustomOidcUser(delegate, user);

        Collection<? extends GrantedAuthority> authorities = customOidcUser.getAuthorities();
        assertThat(authorities).hasSize(1);
        assertThat(authorities.iterator().next().getAuthority()).isEqualTo("ROLE_CUSTOMER");
    }

    @Test
    @DisplayName("3. getAuthorities khi userEntity null -> Trả về authorities từ delegate")
    void getAuthorities_whenUserEntityNull_fallsBackToDelegate() {
        OidcUser delegate = createMockOidcUser();

        CustomOidcUser customOidcUser = new CustomOidcUser(delegate, null);

        Collection<? extends GrantedAuthority> authorities = customOidcUser.getAuthorities();
        assertThat(authorities).hasSize(1);
        assertThat(authorities.iterator().next().getAuthority()).isEqualTo("ROLE_USER");
        assertThat(customOidcUser.getInternalUserId()).isNull();
    }
}

