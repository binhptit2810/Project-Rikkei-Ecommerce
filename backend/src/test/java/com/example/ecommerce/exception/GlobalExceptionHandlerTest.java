package com.example.ecommerce.exception;

import com.example.ecommerce.dto.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit Test cho GlobalExceptionHandler.
 * Kiểm tra chuẩn hóa phản hồi ApiResponse cho tất cả các nhóm mã lỗi HTTP (400, 401, 403, 404, 500).
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("NFR-ERR-01: NoResourceFoundException trả về 404 NOT_FOUND")
    void handleNoResourceFound_Returns404() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "/api/v1/invalid");

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleNoResourceFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("Đường dẫn không tồn tại");
    }

    @Test
    @DisplayName("NFR-ERR-02: ResourceNotFoundException trả về 404 NOT_FOUND")
    void handleNotFound_Returns404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Không tìm thấy sản phẩm");

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Không tìm thấy sản phẩm");
    }

    @Test
    @DisplayName("NFR-ERR-03: BusinessException trả về 400 BAD_REQUEST")
    void handleBusiness_Returns400() {
        BusinessException ex = new BusinessException("Dữ liệu không hợp lệ");

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBusiness(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Dữ liệu không hợp lệ");
    }

    @Test
    @DisplayName("NFR-ERR-04: BadCredentialsException trả về 401 UNAUTHORIZED")
    void handleBadCredentials_Returns401() {
        BadCredentialsException ex = new BadCredentialsException("Sai thông tin");

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBadCredentials(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Email hoặc mật khẩu không đúng");
    }

    @Test
    @DisplayName("NFR-ERR-05: LockedAccountException trả về 403 FORBIDDEN kèm lý do khóa")
    void handleLockedAccount_Returns403WithReason() {
        LockedAccountException ex = new LockedAccountException("Tài khoản của bạn đã bị khóa.", "Vi phạm thanh toán");

        ResponseEntity<ApiResponse<Map<String, String>>> response = exceptionHandler.handleLockedAccount(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Tài khoản của bạn đã bị khóa.");
        assertThat(response.getBody().getData()).containsEntry("reason", "Vi phạm thanh toán");
        assertThat(response.getBody().getData()).containsEntry("lockReason", "Vi phạm thanh toán");
    }

    @Test
    @DisplayName("NFR-ERR-06: DisabledException trả về 403 FORBIDDEN")
    void handleDisabled_Returns403() {
        DisabledException ex = new DisabledException("User is disabled");

        ResponseEntity<ApiResponse<Map<String, String>>> response = exceptionHandler.handleDisabled(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Tài khoản của bạn đã bị khóa.");
    }

    @Test
    @DisplayName("NFR-ERR-07: AccessDeniedException trả về 403 FORBIDDEN")
    void handleAccessDenied_Returns403() {
        AccessDeniedException ex = new AccessDeniedException("Forbidden");

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleAccessDenied(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Bạn không có quyền thực hiện thao tác này");
    }

    @Test
    @DisplayName("NFR-ERR-08: General Exception trả về 500 INTERNAL_SERVER_ERROR")
    void handleGeneral_Returns500() {
        Exception ex = new RuntimeException("Lỗi máy chủ không mong muốn");

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleGeneral(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Lỗi hệ thống, vui lòng thử lại sau");
    }

    @Test
    @DisplayName("Branch: LockedAccountException không có reason -> data trả về null")
    void handleLockedAccount_WithoutReason_ReturnsNullData() {
        LockedAccountException ex = new LockedAccountException("Tài khoản của bạn đã bị khóa.", null);

        ResponseEntity<ApiResponse<Map<String, String>>> response = exceptionHandler.handleLockedAccount(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNull();
    }

    @Test
    @DisplayName("Branch: DisabledException với lý do tùy biến -> data trả về reason và lockReason")
    void handleDisabled_WithCustomMessage_ReturnsDataWithReason() {
        DisabledException ex = new DisabledException("Vi phạm quy chế bán hàng");

        ResponseEntity<ApiResponse<Map<String, String>>> response = exceptionHandler.handleDisabled(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).containsEntry("reason", "Vi phạm quy chế bán hàng");
        assertThat(response.getBody().getData()).containsEntry("lockReason", "Vi phạm quy chế bán hàng");
    }

    @Test
    @DisplayName("Branch: DisabledException với message rỗng -> data trả về null")
    void handleDisabled_WithBlankMessage_ReturnsNullData() {
        DisabledException ex = new DisabledException("   ");

        ResponseEntity<ApiResponse<Map<String, String>>> response = exceptionHandler.handleDisabled(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNull();
    }

    @Test
    @DisplayName("Branch: ResourceNotFoundException với resource name và id")
    void handleNotFound_WithResourceAndId() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Đơn hàng", 100L);

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Đơn hàng không tìm thấy với id: 100");
    }
}

