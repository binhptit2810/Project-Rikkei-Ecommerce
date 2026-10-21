package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.LockUserRequest;
import com.example.ecommerce.dto.response.ApiResponse;
import com.example.ecommerce.dto.response.UserResponse;
import com.example.ecommerce.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Controller quản lý người dùng dành riêng cho Admin.
 *
 * Tất cả endpoint yêu cầu quyền ADMIN (được bảo vệ bởi SecurityConfig: /api/v1/admin/**
 * và @PreAuthorize("hasRole('ADMIN')")).
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    // Giới hạn số lượng phần tử tối đa trên mỗi trang để tránh truy vấn quá lớn
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * Lấy danh sách người dùng trong hệ thống (có phân trang).
     *
     * GET /api/v1/admin/users?page=0&size=10
     * Header: Authorization: Bearer <token_admin>
     *
     * @param page số trang (bắt đầu từ 0, mặc định: 0)
     * @param size số phần tử trên mỗi trang (mặc định: 10, tối đa: 100)
     * @return ApiResponse chứa Page<UserResponse> có đầy đủ thông tin phân trang
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // Validate và chuẩn hóa tham số page
        int pageIndex = Math.max(0, page);

        // Validate và giới hạn kích thước size
        int pageSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);

        // Sắp xếp mặc định theo createdAt giảm dần
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<UserResponse> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    /**
     * Khóa tài khoản người dùng kèm theo lý do bắt buộc.
     *
     * PATCH /api/v1/admin/users/{id}/lock
     * Header: Authorization: Bearer <token_admin>
     * Body: { "reason": "Lý do khóa..." }
     *
     * @param id               ID người dùng cần khóa
     * @param request          chứa lý do khóa
     * @param adminUserDetails tài khoản admin đang đăng nhập (lấy từ SecurityContext)
     * @return ApiResponse chứa UserResponse đã cập nhật trạng thái
     */
    @PatchMapping("/{id}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> lockUser(
            @PathVariable Long id,
            @Valid @RequestBody LockUserRequest request,
            @AuthenticationPrincipal UserDetails adminUserDetails) {

        UserResponse user = userService.lockUser(id, request, adminUserDetails);
        return ResponseEntity.ok(ApiResponse.success("Khóa tài khoản thành công", user));
    }

    /**
     * Mở khóa tài khoản người dùng.
     *
     * PATCH /api/v1/admin/users/{id}/unlock
     * Header: Authorization: Bearer <token_admin>
     *
     * @param id               ID người dùng cần mở khóa
     * @param adminUserDetails tài khoản admin đang đăng nhập (lấy từ SecurityContext)
     * @return ApiResponse chứa UserResponse đã cập nhật trạng thái
     */
    @PatchMapping("/{id}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> unlockUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails adminUserDetails) {

        UserResponse user = userService.unlockUser(id, adminUserDetails);
        return ResponseEntity.ok(ApiResponse.success("Mở khóa tài khoản thành công", user));
    }
}
