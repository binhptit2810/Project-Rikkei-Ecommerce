package com.example.ecommerce.entity;

/**
 * Vai trò người dùng — khớp với ENUM('CUSTOMER', 'ADMIN') trong bảng users.
 * Trong Spring Security, được ánh xạ thành ROLE_CUSTOMER và ROLE_ADMIN.
 * Phụ trách: Cao Chí Thiện
 * Task ID: TSK-AUTH-00 - Hoàn thiện xác thực JWT, Opaque Token và RBAC
 */
public enum Role {
    CUSTOMER,
    ADMIN
}
