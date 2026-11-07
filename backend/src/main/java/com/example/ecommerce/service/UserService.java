package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.ChangePasswordRequest;
import com.example.ecommerce.dto.request.LockUserRequest;
import com.example.ecommerce.dto.request.UpdateProfileRequest;
import com.example.ecommerce.dto.response.UserResponse;
import com.example.ecommerce.entity.Role;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.example.ecommerce.security.CustomUserDetails;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * Nghiệp vụ quản lý hồ sơ người dùng.
 * Phụ trách: Cao Chí Thiện
 * Task ID: TSK-AUTH-00 - Hoàn thiện xác thực JWT, Opaque Token và RBAC
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
// [TSK-D02-CCT]
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;

    /**
     * Lấy thông tin hồ sơ người dùng đang đăng nhập.
     *
     * @param userDetails từ @AuthenticationPrincipal trong Controller
     * @return UserResponse không chứa password
     * @throws ResourceNotFoundException nếu user không tồn tại trong DB
     */
    // [TSK-D04-CCT]
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
     * Lấy danh sách khách hàng (role = CUSTOMER) có phân trang và tìm kiếm (dành cho User area).
     * Tuyệt đối không trả về tài khoản ADMIN.
     *
     * @param pageable thông tin phân trang (page, size, sort)
     * @param keyword  từ khóa tìm kiếm theo tên, email, sđt (chỉ tìm trong phạm vi CUSTOMER)
     * @return Page<UserResponse> danh sách khách hàng
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> getCustomers(Pageable pageable, String keyword) {
        Page<User> userPage;
        if (StringUtils.hasText(keyword)) {
            userPage = userRepository.searchByRoleAndKeyword(Role.CUSTOMER, keyword.trim(), pageable);
        } else {
            userPage = userRepository.findByRole(Role.CUSTOMER, pageable);
        }
        return userPage.map(UserResponse::fromEntity);
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
        Long actorUserId = resolveActorUserId(adminUserDetails);

        User targetUser;
        try {
            targetUser = userRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại với ID: " + id));
        } catch (ResourceNotFoundException e) {
            log.warn("userId={} | action=LOCK_USER | result=FAILURE | targetUserId={} | reason={}",
                    actorUserId, id, sanitize(e.getMessage()));
            throw e;
        }

        // [TSK-D04-CCT] - BR-AUTH-04: Ngăn chặn Admin tự khóa tài khoản của chính mình dựa trên ID xác thực phía server
        if (adminUserDetails != null) {
            boolean isSelf = false;
            if (adminUserDetails instanceof com.example.ecommerce.security.CustomUserDetails customUserDetails
                    && customUserDetails.getUserId() != null) {
                isSelf = targetUser.getId().equals(customUserDetails.getUserId());
            }
            // Điều kiện tương thích phụ khi principal dùng username/email (ví dụ WithMockUser trong test)
            if (!isSelf && StringUtils.hasText(adminUserDetails.getUsername())) {
                isSelf = targetUser.getEmail().equalsIgnoreCase(adminUserDetails.getUsername().trim());
            }
            if (isSelf) {
                log.warn("userId={} | action=LOCK_USER | result=FAILURE | targetUserId={} | reason={}",
                        actorUserId, id, "Bạn không thể tự khóa tài khoản của chính mình");
                throw new BusinessException("Bạn không thể tự khóa tài khoản của chính mình");
            }
        }

        // Kiểm tra nếu tài khoản đã bị khóa trước đó
        if (Boolean.FALSE.equals(targetUser.getActive())) {
            log.warn("userId={} | action=LOCK_USER | result=FAILURE | targetUserId={} | reason={}",
                    actorUserId, id, "Tài khoản này đã bị khóa từ trước");
            throw new BusinessException("Tài khoản này đã bị khóa từ trước");
        }

        String reason = (request != null && request.getReason() != null) ? request.getReason().trim() : "";
        if (!StringUtils.hasText(reason)) {
            log.warn("userId={} | action=LOCK_USER | result=FAILURE | targetUserId={} | reason={}",
                    actorUserId, id, "Lý do khóa tài khoản không được để trống");
            throw new BusinessException("Lý do khóa tài khoản không được để trống");
        }
        if (reason.length() > 500) {
            log.warn("userId={} | action=LOCK_USER | result=FAILURE | targetUserId={} | reason={}",
                    actorUserId, id, "Lý do khóa tài khoản không được vượt quá 500 ký tự");
            throw new BusinessException("Lý do khóa tài khoản không được vượt quá 500 ký tự");
        }

        targetUser.setActive(false);
        targetUser.setLockReason(reason);
        targetUser.setLockedAt(LocalDateTime.now());

        targetUser = userRepository.save(targetUser);

        Long targetId = targetUser.getId();
        String cleanReason = sanitize(reason);

        logAfterCommitOrNow(() ->
            log.info("userId={} | action=LOCK_USER | result=SUCCESS | targetUserId={} | reason={}",
                    actorUserId, targetId, cleanReason)
        );

        // Thu hồi toàn bộ phiên đăng nhập / refresh tokens hiện có trong Redis cho tài khoản bị khóa
        try {
            refreshTokenService.revokeAllUserSessions(targetUser.getId());
            log.info("Đã thu hồi tất cả phiên đăng nhập Redis cho tài khoản bị khóa ID={}", targetUser.getId());
        } catch (Exception ex) {
            log.warn("Cảnh báo: Đã lưu trạng thái khóa vào DB nhưng gặp lỗi thu hồi phiên Redis cho user ID={}: {}",
                    targetUser.getId(), ex.getMessage());
            // Lưu ý: Không che giấu lỗi, nhưng đảm bảo tính nhất quán qua cơ chế hòa giải trạng thái fail-closed:
            // AuthService.refreshToken luôn kiểm tra user.getActive() từ MySQL trước khi cấp token mới.
        }

        // Gửi email thông báo tài khoản bị khóa
        try {
            emailService.sendAccountStatusEmail(
                    targetUser.getEmail(),
                    targetUser.getFullName(),
                    "Tài khoản của bạn đã bị khóa",
                    "Tài khoản Rikkei Store của bạn đã bị tạm thời khóa bởi Quản trị viên.",
                    cleanReason
            );
        } catch (Exception mailEx) {
            log.warn("Không thể gửi email thông báo khóa tài khoản cho {}: {}", targetUser.getEmail(), mailEx.getMessage());
        }

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
    // [TSK-D04-CCT]
    @Transactional
    public UserResponse unlockUser(Long id, UserDetails adminUserDetails) {
        Long actorUserId = resolveActorUserId(adminUserDetails);

        User targetUser;
        try {
            targetUser = userRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại với ID: " + id));
        } catch (ResourceNotFoundException e) {
            log.warn("userId={} | action=UNLOCK_USER | result=FAILURE | targetUserId={} | reason={}",
                    actorUserId, id, sanitize(e.getMessage()));
            throw e;
        }

        // Kiểm tra nếu tài khoản đang hoạt động
        if (Boolean.TRUE.equals(targetUser.getActive())) {
            log.warn("userId={} | action=UNLOCK_USER | result=FAILURE | targetUserId={} | reason={}",
                    actorUserId, id, "Tài khoản này đang hoạt động bình thường, không cần mở khóa");
            throw new BusinessException("Tài khoản này đang hoạt động bình thường, không cần mở khóa");
        }

        targetUser.setActive(true);
        targetUser.setLockReason(null);
        targetUser.setLockedAt(null);

        targetUser = userRepository.save(targetUser);

        Long targetId = targetUser.getId();

        logAfterCommitOrNow(() ->
            log.info("userId={} | action=UNLOCK_USER | result=SUCCESS | targetUserId={}",
                    actorUserId, targetId)
        );

        // Gửi email thông báo mở khóa tài khoản
        try {
            emailService.sendAccountStatusEmail(
                    targetUser.getEmail(),
                    targetUser.getFullName(),
                    "Tài khoản của bạn đã được mở khóa",
                    "Tài khoản Rikkei Store của bạn đã được mở khóa và có thể đăng nhập, sử dụng bình thường.",
                    null
            );
        } catch (Exception mailEx) {
            log.warn("Không thể gửi email thông báo mở khóa tài khoản cho {}: {}", targetUser.getEmail(), mailEx.getMessage());
        }

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
    // [TSK-D04-CCT]
    @Transactional
    public UserResponse updateProfile(UserDetails userDetails, UpdateProfileRequest request) {
        User user = findUserByDetails(userDetails);
        final Long currentUserId = user.getId();

        // Partial update: chỉ cập nhật trường không null
        if (StringUtils.hasText(request.getFullName())) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getPhone() != null) {
            String newPhone = request.getPhone().trim();
            if (StringUtils.hasText(newPhone)) {
                userRepository.findByPhone(newPhone).ifPresent(otherUser -> {
                    if (!otherUser.getId().equals(currentUserId)) {
                        throw new BusinessException("Số điện thoại này đã được sử dụng bởi một tài khoản khác: " + newPhone);
                    }
                });
                user.setPhone(newPhone);
            } else {
                user.setPhone(null);
            }
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }

        user = userRepository.save(user);
        log.info("Cập nhật hồ sơ người dùng: {}", user.getEmail());

        // Gửi email cảnh báo bảo mật cập nhật thông tin hồ sơ
        try {
            emailService.sendSecurityAlertEmail(
                    user.getEmail(),
                    user.getFullName(),
                    "Cập nhật thông tin hồ sơ",
                    "Thông tin tài khoản cá nhân của bạn tại Rikkei Store vừa được cập nhật thành công."
            );
        } catch (Exception mailEx) {
            log.warn("Không thể gửi email thông báo cập nhật hồ sơ cho {}: {}", user.getEmail(), mailEx.getMessage());
        }

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
    // [TSK-D04-CCT]
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

        // Gửi email cảnh báo bảo mật đổi mật khẩu
        try {
            emailService.sendSecurityAlertEmail(
                    user.getEmail(),
                    user.getFullName(),
                    "Đổi mật khẩu tài khoản thành công",
                    "Mật khẩu tài khoản Rikkei Store của bạn vừa được thay đổi thành công vào lúc "
                            + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy"))
                            + ". Nếu bạn không thực hiện thay đổi này, vui lòng liên hệ ngay với CSKH Rikkei Store (1900 6868) để khóa tài khoản khẩn cấp."
            );
        } catch (Exception mailEx) {
            log.warn("Không thể gửi email thông báo đổi mật khẩu cho {}: {}", user.getEmail(), mailEx.getMessage());
        }
    }

    /**
     * Xóa tài khoản người dùng khỏi hệ thống (dành cho Admin).
     *
     * @param id               ID người dùng cần xóa
     * @param adminUserDetails thông tin Admin đang thực hiện (từ SecurityContext)
     */
    @Transactional
    public void deleteUser(Long id, UserDetails adminUserDetails) {
        Long actorUserId = resolveActorUserId(adminUserDetails);

        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại với ID: " + id));

        if (adminUserDetails != null) {
            boolean isSelf = false;
            if (adminUserDetails instanceof CustomUserDetails customUserDetails && customUserDetails.getUserId() != null) {
                isSelf = targetUser.getId().equals(customUserDetails.getUserId());
            }
            if (!isSelf && StringUtils.hasText(adminUserDetails.getUsername())) {
                isSelf = targetUser.getEmail().equalsIgnoreCase(adminUserDetails.getUsername().trim());
            }
            if (isSelf) {
                throw new BusinessException("Bạn không thể tự xóa tài khoản của chính mình");
            }
        }

        String targetEmail = targetUser.getEmail();
        String targetFullName = targetUser.getFullName();

        try {
            refreshTokenService.revokeAllUserSessions(targetUser.getId());
        } catch (Exception ex) {
            log.warn("Lỗi thu hồi phiên đăng nhập khi xóa user ID={}: {}", targetUser.getId(), ex.getMessage());
        }

        try {
            emailService.sendAccountStatusEmail(
                    targetEmail,
                    targetFullName,
                    "Tài khoản đã bị xóa khỏi hệ thống",
                    "Tài khoản của bạn tại hệ thống Rikkei Store đã bị xóa bởi Quản trị viên.",
                    null
            );
        } catch (Exception mailEx) {
            log.warn("Không thể gửi email thông báo xóa tài khoản cho {}: {}", targetEmail, mailEx.getMessage());
        }

        userRepository.delete(targetUser);
        log.info("userId={} | action=DELETE_USER | result=SUCCESS | targetUserId={}", actorUserId, id);
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

    private void logAfterCommitOrNow(Runnable logAction) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    logAction.run();
                }
            });
        } else {
            logAction.run();
        }
    }

    private static String sanitize(String input) {
        if (input == null) {
            return "";
        }
        return input.replaceAll("[\\r\\n]+", " ").trim();
    }

    private Long resolveActorUserId(UserDetails userDetails) {
        if (userDetails instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUserId();
        }
        return null;
    }
}
