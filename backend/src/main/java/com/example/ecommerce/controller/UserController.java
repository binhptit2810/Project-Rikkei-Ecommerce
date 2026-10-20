package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.ChangePasswordRequest;
import com.example.ecommerce.dto.request.UpdateProfileRequest;
import com.example.ecommerce.dto.response.ApiResponse;
import com.example.ecommerce.dto.response.UserResponse;
import com.example.ecommerce.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Controller hồ sơ người dùng.
 *
 * Tất cả endpoint yêu cầu xác thực (Authorization: Bearer <token>).
 * Người dùng chỉ truy cập được hồ sơ của chính mình — không có userId trong path.
 *
 * Được sử dụng bởi:
 *   - Frontend: trang hồ sơ cá nhân (/profile)
 *   - Đỗ Hoàng Anh: hiển thị tên người dùng trên header sau đăng nhập
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Lấy thông tin hồ sơ người dùng đang đăng nhập.
     *
     * GET /api/v1/users/me
     * Header: Authorization: Bearer <token>
     *
     * Response 200 OK:
     *   {
     *     "success": true,
     *     "data": {
     *       "id": 1,
     *       "email": "user@example.com",
     *       "fullName": "Nguyễn Văn A",
     *       "phone": "0912345678",
     *       "address": "...",
     *       "role": "CUSTOMER",
     *       "active": true,
     *       "createdAt": "2024-10-15T10:00:00"
     *     }
     *   }
     *
     * Response 401: chưa đăng nhập hoặc token hết hạn
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {

        UserResponse user = userService.getCurrentUser(userDetails);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    /**
     * Cập nhật hồ sơ người dùng đang đăng nhập.
     *
     * PUT /api/v1/users/me
     * Header: Authorization: Bearer <token>
     *
     * Request body (partial update — null = giữ nguyên):
     *   {
     *     "fullName": "Tên mới",       // tùy chọn
     *     "phone": "0987654321",       // tùy chọn
     *     "address": "Địa chỉ mới"    // tùy chọn
     *   }
     *
     * Response 200 OK: UserResponse sau khi cập nhật
     * Response 400: validation lỗi
     * Response 401: chưa đăng nhập hoặc token hết hạn
     *
     * Lưu ý: email, password, role KHÔNG thể thay đổi qua endpoint này.
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {

        UserResponse user = userService.updateProfile(userDetails, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật hồ sơ thành công", user));
    }

    /**
     * Đổi mật khẩu cho người dùng đang đăng nhập.
     *
     * PUT /api/v1/users/me/password
     * Header: Authorization: Bearer <token>
     *
     * Request body:
     *   {
     *     "currentPassword": "Password123",
     *     "newPassword": "NewPassword456",
     *     "confirmPassword": "NewPassword456"
     *   }
     *
     * Response 200 OK:
     *   {
     *     "success": true,
     *     "message": "Đổi mật khẩu thành công",
     *     "data": null
     *   }
     *
     * Response 400: validation lỗi (thiếu field, độ dài sai) hoặc mật khẩu cũ sai / xác nhận không khớp
     * Response 401: chưa đăng nhập hoặc token hết hạn
     */
    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {

        userService.changePassword(userDetails, request);
        return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công", null));
    }
}
