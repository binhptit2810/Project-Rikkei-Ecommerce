package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.ChangePasswordRequest;
import com.example.ecommerce.dto.request.LockUserRequest;
import com.example.ecommerce.dto.request.UpdateProfileRequest;
import com.example.ecommerce.dto.response.UserResponse;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

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
    private final RefreshTokenService refreshTokenService;

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
     * Lấy danh sách toàn bộ người dùng có phân trang (dành cho Admin).
     *
     * @param pageable thông tin phân trang (page, size, sort)
     * @return Page<UserResponse> danh sách người dùng được convert sang DTO an toàn (không chứa password)
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(UserResponse::fromEntity);
    }

    /**
     * Khóa tài khoản người dùng kèm lý do và thời điểm khóa (dành cho Admin).
     *
     * @param id               ID người dùng cần khóa
     * @param request          chứa lý do khóa
     * @param adminUserDetails thông tin Admin đang thực hiện (từ SecurityContext)
     * @return UserResponse thông tin tài khoản sau khi khóa
     * @throws ResourceNotFoundException nếu không tìm thấy người dùng
     * @throws BusinessException        nếu admin tự khóa chính mình, tài khoản đã bị khóa, hoặc lý do không hợp lệ
     */
    @Transactional
    public UserResponse lockUser(Long id, LockUserRequest request, UserDetails adminUserDetails) {
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại với ID: " + id));

        // Không cho phép Admin tự khóa tài khoản đang sử dụng
        if (adminUserDetails != null && targetUser.getEmail().equalsIgnoreCase(adminUserDetails.getUsername())) {
            throw new BusinessException("Bạn không thể tự khóa tài khoản của chính mình");
        }

        // Kiểm tra nếu tài khoản đã bị khóa trước đó
        if (Boolean.FALSE.equals(targetUser.getActive())) {
            throw new BusinessException("Tài khoản này đã bị khóa từ trước");
        }

        String reason = (request != null && request.getReason() != null) ? request.getReason().trim() : "";
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException("Lý do khóa tài khoản không được để trống");
        }
        if (reason.length() > 500) {
            throw new BusinessException("Lý do khóa tài khoản không được vượt quá 500 ký tự");
        }

        targetUser.setActive(false);
        targetUser.setLockReason(reason);
        targetUser.setLockedAt(LocalDateTime.now());

        targetUser = userRepository.save(targetUser);
        log.info("Admin [{}] đã khóa tài khoản ID={} ({}), lý do: {}",
                adminUserDetails != null ? adminUserDetails.getUsername() : "UNKNOWN",
                targetUser.getId(), targetUser.getEmail(), reason);

        return UserResponse.fromEntity(targetUser);
    }

    /**
     * Mở khóa tài khoản người dùng (dành cho Admin).
     *
     * @param id               ID người dùng cần mở khóa
     * @param adminUserDetails thông tin Admin đang thực hiện (từ SecurityContext)
     * @return UserResponse thông tin tài khoản sau khi mở khóa
     * @throws ResourceNotFoundException nếu không tìm thấy người dùng
     * @throws BusinessException        nếu tài khoản đang hoạt động
     */
    @Transactional
    public UserResponse unlockUser(Long id, UserDetails adminUserDetails) {
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại với ID: " + id));

        // Kiểm tra nếu tài khoản đang hoạt động
        if (Boolean.TRUE.equals(targetUser.getActive())) {
            throw new BusinessException("Tài khoản này đang hoạt động bình thường, không cần mở khóa");
        }

        targetUser.setActive(true);
        targetUser.setLockReason(null);
        targetUser.setLockedAt(null);

        targetUser = userRepository.save(targetUser);
        log.info("Admin [{}] đã mở khóa tài khoản ID={} ({})",
                adminUserDetails != null ? adminUserDetails.getUsername() : "UNKNOWN",
                targetUser.getId(), targetUser.getEmail());

        return UserResponse.fromEntity(targetUser);
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
        log.info("Đổi mật khẩu thành công trong CSDL cho tài khoản: {}", user.getEmail());

        // Thu hồi toàn bộ phiên Refresh Token hiện có trong Redis
        try {
            refreshTokenService.revokeAllUserSessions(user.getId());
            log.info("Đã thu hồi tất cả phiên đăng nhập Redis cho user ID={}", user.getId());
        } catch (Exception ex) {
            log.error("Cảnh báo: Đã lưu mật khẩu mới vào DB nhưng gặp lỗi thu hồi phiên Redis cho user ID={}: {}",
                    user.getId(), ex.getMessage());
            // Lưu ý: Không che giấu lỗi, nhưng đảm bảo tính nhất quán qua cơ chế hòa giải trạng thái
            // (AuthService.refreshToken sẽ tự động từ chối mọi token cũ dựa trên hash mật khẩu đã thay đổi).
        }
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
