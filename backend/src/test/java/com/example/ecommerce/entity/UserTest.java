package com.example.ecommerce.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit Test cho User Entity.
 * Phụ trách: Cao Chí Thiện
 * Task ID: TSK-AUTH-00 - Hoàn thiện xác thực JWT, Opaque Token và RBAC
 * Kiểm tra các hàm lifecycle JPA callback onCreate và onUpdate.
 */
class UserTest {

    @Test
    @DisplayName("User Entity: Kiểm tra onCreate thiết lập createdAt và updatedAt")
    void onCreate_SetsTimestamps() {
        User user = new User();
        ReflectionTestUtils.invokeMethod(user, "onCreate");

        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("User Entity: Kiểm tra onUpdate cập nhật updatedAt")
    void onUpdate_UpdatesTimestamp() {
        User user = new User();
        ReflectionTestUtils.invokeMethod(user, "onCreate");
        var initialUpdatedAt = user.getUpdatedAt();

        ReflectionTestUtils.invokeMethod(user, "onUpdate");

        assertThat(user.getUpdatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isAfterOrEqualTo(initialUpdatedAt);
    }

    @Test
    @DisplayName("User Entity: Kiểm tra trường emailVerified và emailVerifiedAt")
    void user_EmailVerifiedFields() {
        User user = new User();
        assertThat(user.getEmailVerified()).isFalse();

        user.setEmailVerified(true);
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        user.setEmailVerifiedAt(now);

        assertThat(user.getEmailVerified()).isTrue();
        assertThat(user.getEmailVerifiedAt()).isEqualTo(now);
    }
}
