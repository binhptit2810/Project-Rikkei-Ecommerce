package com.example.ecommerce.entity;

/**
 * Vai trò người dùng — khớp với ENUM('CUSTOMER', 'ADMIN') trong bảng users.
 * Trong Spring Security, được ánh xạ thành ROLE_CUSTOMER và ROLE_ADMIN.
 */
public enum Role {
    CUSTOMER,
    ADMIN
}
