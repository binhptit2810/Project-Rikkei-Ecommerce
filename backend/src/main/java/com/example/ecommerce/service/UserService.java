package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.ChangePasswordRequest;
import com.example.ecommerce.dto.request.UpdateProfileRequest;
import com.example.ecommerce.dto.response.UserResponse;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Nghiệp vụ quản lý hồ sơ người dùng.
 *
 * Nguyên tắc bảo mật:
 *   - Danh tính người dùng luôn được lấy từ SecurityContext (UserDetails), KHÔNG tin tưởng ID do client gửi.
 *   - Người dùng chỉ được đọc và sửa hồ sơ của chính mình.
 *   - Không cho phép tự thay đổi email, password, role hoặc active qua API này.
 *
 * Cách các module khác lấy userId:
 *   - Inject UserRepository và gọi findByEmail(userDetails.getUsername())
 *   - HOẶC đọc JWT claims trong SecurityContext principal
 *   - VD trong OrderService: userRepository.findByEmail(userDetails.getUsername()).getId()
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Lấy thông tin hồ sơ người dùng đang đăng nhập.
     *
     * @param userDetails từ @AuthenticationPrincipal trong Controller
     * @return UserResponse không chứa password
     * @throws ResourceNotFoundException nếu user không tồn tại trong DB
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UserDetails userDetails) {
        User user = findUserByDetails(userDetails);
        return UserResponse.fromEntity(user);
    }

    /**
     * Cập nhật hồ sơ người dùng đang đăng nhập (partial update).
     *
     * Các trường được phép cập nhật: fullName, phone, address.
     * null = giữ nguyên giá trị cũ.
     * Chuỗi rỗng cho phone/address = xóa giá trị.
     *
     * @param userDetails từ @AuthenticationPrincipal trong Controller
     * @param request     dữ liệu cập nhật đã được validate
     * @return UserResponse sau khi cập nhật
     */
    @Transactional
    public UserResponse updateProfile(UserDetails userDetails, UpdateProfileRequest request) {
        User user = findUserByDetails(userDetails);

        // Partial update: chỉ cập nhật trường không null
        if (StringUtils.hasText(request.getFullName())) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }

        user = userRepository.save(user);
        log.info("Cập nhật hồ sơ người dùng: {}", user.getEmail());
        return UserResponse.fromEntity(user);
    }

    /**
     * Đổi mật khẩu cho người dùng đang đăng nhập.
     *
     * Xác thực:
     *   1. Kiểm tra xác nhận mật khẩu mới khớp
     *   2. Kiểm tra mật khẩu hiện tại đúng với BCrypt hash trong DB
     *   3. Kiểm tra mật khẩu mới không trùng mật khẩu cũ
     *   4. Hash mật khẩu mới bằng BCrypt và lưu DB
     *
     * @param userDetails từ @AuthenticationPrincipal trong Controller
     * @param request     dữ liệu đổi mật khẩu
     * @throws BusinessException nếu mật khẩu xác nhận không khớp, mật khẩu cũ sai, hoặc mật khẩu mới trùng mật khẩu cũ
     */
    @Transactional
    public void changePassword(UserDetails userDetails, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("Mật khẩu mới và xác nhận mật khẩu không khớp");
        }

        User user = findUserByDetails(userDetails);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("Mật khẩu hiện tại không chính xác");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException("Mật khẩu mới không được trùng với mật khẩu hiện tại");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Đổi mật khẩu thành công cho tài khoản: {}", user.getEmail());
    }

    // ---------- Private helpers ----------

    /**
     * Tìm User entity từ email trong UserDetails (lấy từ SecurityContext).
     * Ném ResourceNotFoundException nếu không tìm thấy.
     */
    private User findUserByDetails(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Người dùng không tồn tại: " + userDetails.getUsername()
                ));
    }
}
