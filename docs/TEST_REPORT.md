# BÁO CÁO KẾT QUẢ KIỂM THỬ VÀ ĐÁP ỨNG YÊU CẦU PHI CHỨC NĂNG
## DỰ ÁN: RIKKEI ECOMMERCE PLATFORM

> **Thời điểm ghi nhận báo cáo:** 21/10/2026, 16:22:42  
> **Môi trường thực thi:** Gradle 8.10.2, Java 21  
> **Tổng kết kiểm thử tự động (Automated Test Suite):**
> - **Tổng số test cases:** **120**
> - **Passed:** **120 / 120 (100% Pass Rate)**
> - **Failed:** **0**
> - **Ignored / Skipped:** **0**
> - **Thời gian thực thi:** **4.011 giây**
> 
> **Tổng quan độ bao phủ mã nguồn (JaCoCo Coverage Summary):**
> - **Instruction Coverage:** **89%** (2.416 / 2.690 instructions được bao phủ, 274 missed)
> - **Branch Coverage:** **76%** (132 / 172 branches được bao phủ, 40 missed)
> - **Complexity Coverage:** **79%** (145 / 183 complexity, 38 missed)
> - **Line Coverage:** **88%** (577 / 649 lines được bao phủ, 72 missed)
> - **Method Coverage:** **94%** (92 / 97 methods được bao phủ, 5 missed)
> - **Class Coverage:** **100%** (22 / 22 classes)

---

## 1. TỔNG QUAN CHIẾN LƯỢC KIỂM THỬ (TESTING STRATEGY)

Hệ thống được tổ chức kiểm thử tự động theo mô hình phân tầng kim tự tháp (Testing Pyramid), bao gồm:

1. **Unit Tests (Kiểm thử đơn vị):**
   - Sử dụng **JUnit 5 (Jupiter)** và **Mockito** để cô lập hoàn toàn các phụ thuộc ngoại vi (Database, Redis, Network).
   - Kiểm thử toàn bộ các nhánh logic nghiệp vụ, điều kiện biên, xử lý ngoại lệ trong `AuthService`, `RefreshTokenService`, `UserService`, `JwtUtils`, `CustomUserDetailsService`, `JwtAuthenticationFilter`, `CookieUtils` và `GlobalExceptionHandler`.
   - **RefreshTokenServiceTest** kiểm tra chuyên sâu cơ chế bảo mật Opaque Refresh Token: sinh token ngẫu nhiên an toàn (CSPRNG), băm SHA-256 lưu Redis, xoay vòng nguyên tử (Atomic Token Rotation) qua Redis Lua script, xử lý Grace Period (10s), phát hiện tái sử dụng token (Reuse Detection), phòng ngừa va chạm khóa Redis và thu hồi phiên người dùng.
2. **Integration Tests & Security Tests (Kiểm thử tích hợp & Bảo mật Web Layer):**
   - Sử dụng `@SpringBootTest` kết hợp `@AutoConfigureMockMvc` và Spring Security Test (`@WithMockUser`).
   - Sử dụng **H2 In-Memory Database** với profile `test`, giả lập môi trường runtime mà không ảnh hưởng tới CSDL thực.
   - Kiểm tra chuỗi Filter Security, RBAC (Role-Based Access Control), Bean Validation (`@Valid`), và chuẩn hóa phản hồi qua `AdminUserControllerTest`, `AuthControllerTest`, `UserControllerTest`, `HealthControllerTest`.
3. **Đo lường độ bao phủ mã nguồn (Code Coverage):**
   - Sử dụng **JaCoCo (Java Code Coverage)** phiên bản **0.8.12**.
   - Đo lường chi tiết cả Instruction Coverage, Branch Coverage, Complexity, Line và Method Coverage cho toàn bộ các gói mã nguồn backend.

---

## 2. MA TRẬN ĐÁP ỨNG YÊU CẦU PHI CHỨC NĂNG (NFR MATRIX)

Bảng ma trận phân định rõ ràng giữa các yêu cầu đã được xác minh qua kiểm thử tự động, các hạng mục mới kiểm tra thủ công, và các hạng mục cần kiểm thử tải/môi trường thực tế:

| Mã NFR | Nhóm yêu cầu | Nội dung đặc tả yêu cầu phi chức năng | Cơ chế hiện thực trong hệ thống | Trạng thái xác minh | Nguồn bằng chứng |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **NFR-SEC-01** | Bảo mật | **Băm mật khẩu an toàn:** Không lưu plain-text mật khẩu trong CSDL. | Sử dụng `BCryptPasswordEncoder` với cost factor = 12. | **ĐÃ XÁC MINH** | Unit Test (`AuthServiceTest`, `UserServiceTest`) |
| **NFR-SEC-02** | Bảo mật | **Xác thực JWT Stateless:** Token mã hóa bằng thuật toán HMAC-SHA256 với secret key an toàn (≥ 256-bit). | Thư viện JJWT 0.12.6, cấu hình thời hạn token 15 phút và ký số an toàn. | **ĐÃ XÁC MINH** | Unit Test (`JwtUtilsTest`) |
| **NFR-SEC-03** | Bảo mật | **Phân quyền truy cập RBAC:** Endpoint quản trị chỉ dành riêng cho Admin; chặn Customer và Guest. | `@PreAuthorize("hasRole('ADMIN')")` và cấu hình phân quyền trong `SecurityConfig`. | **ĐÃ XÁC MINH** | Integration Test (`AdminUserControllerTest` MockMvc) |
| **NFR-SEC-04** | Bảo mật | **Khóa tài khoản và chặn phiên tức thì:** Tài khoản bị khóa không thể đăng nhập hoặc dùng JWT cũ. | Bắt `LockedAccountException` tại `AuthService`, set `disabled=true` trong `CustomUserDetailsService` và chặn trong `JwtAuthenticationFilter`. | **ĐÃ XÁC MINH** | Unit Test (`AuthServiceTest`, `JwtAuthenticationFilterTest`) |
| **NFR-SEC-05** | Bảo mật | **Ngăn ngừa tự khóa tài khoản:** Admin không thể tự khóa chính tài khoản của mình. | Kiểm tra email từ SecurityContext (`UserDetails`) và chặn ném `BusinessException`. | **ĐÃ XÁC MINH** | Unit Test (`UserServiceTest`) |
| **NFR-SEC-RT-01** | Bảo mật RT | **Sinh Refresh Token an toàn & Băm SHA-256:** Opaque token từ 64 byte ngẫu nhiên mật mã (CSPRNG), chỉ lưu hash SHA-256 vào Redis. | `SecureRandom`, Base64 URL-safe, băm SHA-256 qua `MessageDigest` trước khi ghi nhận Redis. | **ĐÃ XÁC MINH (Logic/Mock)** | Unit Test (`RefreshTokenServiceTest`), Kiểm tra thủ công Redis |
| **NFR-SEC-RT-02** | Bảo mật RT | **Xoay vòng Refresh Token nguyên tử (Atomic Rotation):** Đổi token cũ lấy token mới nguyên tử, tránh race condition. | Redis Lua Script thực thi nguyên tử: kiểm tra trạng thái, ghi nhận token mới, chuyển token cũ sang TOMBSTONE. | **ĐÃ XÁC MINH (Logic/Mock)** | Unit Test (`RefreshTokenServiceTest`), Cần kiểm thử tích hợp Redis |
| **NFR-SEC-RT-03** | Bảo mật RT | **Chống token hết hạn / không tồn tại:** Từ chối yêu cầu refresh nếu token không có trong Redis. | Truy vấn khóa `auth:refresh:<hash>`, ném `BadCredentialsException` nếu không tìm thấy. | **ĐÃ XÁC MINH** | Unit Test (`RefreshTokenServiceTest`) |
| **NFR-SEC-RT-04** | Bảo mật RT | **Phát hiện tái sử dụng Token (Reuse Detection):** Nếu token đã xoay vòng bị dùng lại ngoài Grace Period (10s), lập tức thu hồi toàn bộ phiên của tài khoản. | Kiểm tra trạng thái `ROTATED`, tính `rotatedAt`. Nếu vượt quá 10s, gọi Lua script xóa sạch mọi refresh token trong ZSET `auth:user_sessions:<userId>`. | **ĐÃ XÁC MINH (Logic/Mock)** | Unit Test (`RefreshTokenServiceTest`), Cần kiểm thử tích hợp Redis |
| **NFR-SEC-RT-05** | Bảo mật RT | **Thu hồi đơn phiên khi Đăng xuất:** Xóa đúng refresh token hiện tại khỏi Redis và ZSET. | Xóa khóa `auth:refresh:<hash>` và loại bỏ khỏi ZSET `auth:user_sessions:<userId>`. | **ĐÃ XÁC MINH (Logic/Mock)** | Unit Test (`RefreshTokenServiceTest`, `AuthServiceTest`) |
| **NFR-SEC-RT-06** | Bảo mật RT | **Thu hồi toàn bộ phiên khi Đổi mật khẩu:** Khi user đổi mật khẩu, hủy toàn bộ phiên đang đăng nhập trên mọi thiết bị. | `UserService.changePassword()` gọi `refreshTokenService.revokeAllUserSessions(userId)`. | **ĐÃ XÁC MINH** | Unit Test (`UserServiceTest`, `RefreshTokenServiceTest`) |
| **NFR-DATA-01** | Toàn vẹn dữ liệu | **Email duy nhất:** Không cho phép đăng ký tài khoản có email trùng lặp. | Kiểm tra `userRepository.existsByEmail()` và ràng buộc duy nhất trong CSDL. | **ĐÃ XÁC MINH** | Unit Test (`AuthServiceTest`) |
| **NFR-DATA-02** | Toàn vẹn dữ liệu | **Chuẩn hóa dữ liệu đầu vào:** Tự động chuẩn hóa email (lowercase, trim khoảng trắng). | Xử lý `request.getEmail().toLowerCase().trim()` trước khi query và lưu trữ. | **ĐÃ XÁC MINH** | Unit Test (`AuthServiceTest`) |
| **NFR-DATA-03** | Toàn vẹn dữ liệu | **Xác thực ràng buộc nghiệp vụ (Bean Validation):** Kiểm tra email hợp lệ, độ dài mật khẩu (≥ 6), lý do khóa bắt buộc (1–500 ký tự). | Sử dụng Hibernate Validator (`@NotBlank`, `@Email`, `@Size`, `@Pattern`). | **ĐÃ XÁC MINH** | Integration Test (`AuthControllerTest`, `AdminUserControllerTest`) |
| **NFR-REL-01** | Tin cậy & Lỗi | **Chuẩn hóa phản hồi API:** Tất cả response (thành công hoặc lỗi) tuân thủ cấu trúc chuẩn `ApiResponse`. | `GlobalExceptionHandler` kết hợp cùng `ApiResponse<T>` chuẩn hóa. | **ĐÃ XÁC MINH** | Unit Test (`GlobalExceptionHandlerTest`) |
| **NFR-REL-02** | Tin cậy & Lỗi | **Bảo vệ thông tin đăng nhập:** Không tiết lộ lý do cụ thể khi đăng nhập sai (tránh User Enumeration attack). | Ném `BadCredentialsException` trả 401 với thông điệp chung "Email hoặc mật khẩu không đúng". | **ĐÃ XÁC MINH** | Unit Test (`AuthServiceTest`) |
| **NFR-REL-03** | Tin cậy & Lỗi | **Xử lý sự cố kết nối Redis:** Khi Redis gián đoạn, phản hồi 503 Service Unavailable chuẩn mực thay vì sập 500. | `GlobalExceptionHandler` xử lý `RedisConnectionFailureException`. | **ĐÃ XÁC MINH** | Unit Test (`GlobalExceptionHandlerTest`) |
| **NFR-PERF-01**| Hiệu năng | **Phân trang bảo vệ tài nguyên (Pagination Safeguard):** Giới hạn `MAX_PAGE_SIZE = 100`, chuẩn hóa `pageIndex >= 0`. | Cắt tỉa tham số `size` và `page` trong `AdminUserController`, mặc định 10, tối đa 100. | **ĐÃ XÁC MINH (Logic)** | Unit & MockMvc Test. *Chưa kiểm thử tải (Load Test)* |
| **NFR-PERF-02**| Hiệu năng & Bảo mật | **Chống rò rỉ dữ liệu nhạy cảm (Data Exposure Prevention):** Không bao giờ trả mật khẩu hash trong API. | Chuyển đổi User Entity sang `UserResponse` DTO loại bỏ hoàn toàn trường password. | **ĐÃ XÁC MINH** | Unit Test (`UserControllerTest`) |

---

## 3. BẢNG ĐỐI CHIẾU KẾT QUẢ KIỂM THỬ TRƯỚC VÀ SAU KHI TÍCH HỢP REFRESH TOKEN

| Chỉ số kiểm thử | Giai đoạn trước (Chưa tích hợp RT) | Giai đoạn hiện tại (Đã tích hợp RT & Redis) | Thay đổi thực tế |
| :--- | :---: | :---: | :---: |
| **Tổng số test cases** | 100 | **120** | +20 test cases (+20%) |
| **Số test PASS** | 100 | **120** | **100% tỷ lệ thành công** |
| **Số test FAIL / IGNORED** | 0 | **0** | Không có test lỗi |
| **Thời gian thực thi** | 7.2 giây | **4.011 giây** | Tối ưu hóa Mock/Context |
| **Instruction Coverage** | 99% (1.528 / 1.540) | **89% (2.416 / 2.690)** | Quy mô codebase tăng (+1.150 instructions) |
| **Branch Coverage** | 97% (84 / 86) | **76% (132 / 172)** | Codebase mở rộng (+86 nhánh logic mới) |
| **Line Coverage** | 99% (372 / 376) | **88% (577 / 649)** | +273 dòng code mới được bổ sung |
| **Method Coverage** | 99% (77 / 78) | **94% (92 / 97)** | +19 phương thức mới được thêm |
| **Class Coverage** | 100% (22 / 22) | **100% (22 / 22)** | 22/22 classes đều có test |

> [!NOTE]
> Sự thay đổi về tỷ lệ phần trăm độ bao phủ (Coverage) từ 97% branch xuống 76% branch là do trong Giai đoạn 2, dự án đã bổ sung toàn bộ phân hệ **Refresh Token & Redis Session Management** (`RefreshTokenService`, các endpoint `/refresh`, `/logout` trong `AuthController`, tiện ích cookie `CookieUtils`), làm tổng số nhánh rẽ tăng gấp đôi từ 86 lên 172 nhánh. Các nhánh rẽ chưa được bao phủ tập trung ở tầng Controller HTTP và CookieUtils (xem phân tích chi tiết tại Mục 7).

---

## 4. BÁO CÁO ĐỘ BAO PHỦ CHI TIẾT THEO GÓI MÃ NGUỒN (JACOCO REPORT)

Số liệu được trích xuất trực tiếp từ báo cáo JaCoCo tại `backend/build/reports/jacoco/test/html/index.html`:

| Gói mã nguồn (Package) | Dòng mã nguồn (Lines)<br>Missed / Total (Cov%) | Phương thức (Methods)<br>Missed / Total (Cov%) | Bao phủ câu lệnh (Instructions)<br>Missed / Total (Cov%) | Bao phủ nhánh rẽ (Branches)<br>Missed / Total (Cov%) | Độ phức tạp (Complexity)<br>Missed / Total (Cov%) | Đánh giá |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| **`com.example.ecommerce.service`** | 42 / 339 (87%) | 0 / 28 (100%) | 153 / 1.481 (89%) | 24 / 116 (79%) | 23 / 86 (73%) | Tốt |
| **`com.example.ecommerce.security`** | 2 / 77 (97%) | 0 / 15 (100%) | 7 / 308 (97%) | 2 / 28 (92%) | 2 / 29 (93%) | Vượt trội |
| **`com.example.ecommerce.controller`** | 19 / 57 (66%) | 3 / 14 (78%) | 89 / 271 (67%) | 11 / 14 (21%) | 9 / 21 (57%) | Cần bổ sung test |
| **`com.example.ecommerce.exception`** | 0 / 68 (100%) | 0 / 17 (100%) | 0 / 232 (100%) | 0 / 10 (100%) | 0 / 22 (100%) | Tuyệt đối |
| **`com.example.ecommerce.config`** | 0 / 54 (100%) | 0 / 12 (100%) | 0 / 262 (100%) | 0 / 0 (n/a) | 0 / 12 (100%) | Tuyệt đối |
| **`com.example.ecommerce.dto.response`**| 0 / 28 (100%) | 0 / 4 (100%) | 0 / 62 (100%) | 0 / 0 (n/a) | 0 / 4 (100%) | Tuyệt đối |
| **`com.example.ecommerce.util`** | 7 / 14 (50%) | 1 / 2 (50%) | 20 / 38 (47%) | 3 / 4 (25%) | 3 / 4 (25%) | Cần bổ sung test |
| **`com.example.ecommerce.entity`** | 0 / 9 (100%) | 0 / 3 (100%) | 0 / 28 (100%) | 0 / 0 (n/a) | 0 / 3 (100%) | Tuyệt đối |
| **`com.example.ecommerce` (Main)** | 2 / 3 (33%) | 1 / 2 (50%) | 5 / 8 (37%) | 0 / 0 (n/a) | 1 / 2 (50%) | Khởi động Main |
| **TỔNG TOÀN BỘ HỆ THỐNG** | **72 / 649 (88%)** | **5 / 97 (94%)** | **274 / 2.690 (89%)** | **40 / 172 (76%)** | **38 / 183 (79%)** | **ĐẠT (Mục tiêu 80% Branch cần tối ưu thêm)** |

### Chi tiết các lớp trong gói cốt lõi:
- **`com.example.ecommerce.service`:**
  - `UserService`: **100% Branch Coverage** (32 / 32 branches), **97% Instruction Coverage** (327 / 335), 0 methods missed.
  - `AuthService`: **88% Branch Coverage** (23 / 26 branches), **99% Instruction Coverage** (345 / 345), 0 methods missed.
  - `RefreshTokenService`: **63% Branch Coverage** (37 / 58 branches), **82% Instruction Coverage** (656 / 800), 0 methods missed.
- **`com.example.ecommerce.security`:**
  - `CustomUserDetailsService`: **100% Branch Coverage** (4 / 4), **100% Instruction Coverage** (57 / 57).
  - `JwtAuthenticationFilter`: **95% Branch Coverage** (19 / 20), **100% Instruction Coverage** (122 / 122).
  - `JwtUtils`: **75% Branch Coverage** (3 / 4), **94% Instruction Coverage** (122 / 129).
- **`com.example.ecommerce.controller`:**
  - `AdminUserController`: **100% Branch Coverage** (2 / 2), **100% Instruction Coverage** (56 / 56).
  - `UserController`: n/a branches, **100% Instruction Coverage** (47 / 47).
  - `HealthController`: n/a branches, **100% Instruction Coverage** (32 / 32).
  - `AuthController`: **8% Branch Coverage** (1 / 12 branches), **34% Instruction Coverage** (47 / 136).

---

## 5. DANH SÁCH CHI TIẾT 120 TEST CASES THEO PACKAGE VÀ TEST CLASS

Tất cả 120 test cases dưới đây được đối chiếu và xác minh từ các file kết quả kiểm thử XML tại `backend/build/test-results/test/`:

### 5.1. Nhóm Dịch vụ (`com.example.ecommerce.service` — 55 Test Cases)

#### A. `AuthServiceTest` (20 Test Cases)
1. `TC-AUTH-01`: NFR-SEC-01 & FR-AUTH-01: Đăng ký tài khoản thành công - Mật khẩu được băm BCrypt và role mặc định CUSTOMER.
2. `TC-AUTH-02`: NFR-DATA-01: Đăng ký thất bại khi email đã tồn tại trong hệ thống.
3. `TC-AUTH-03`: NFR-DATA-02: Email được chuẩn hóa tự động (lowercase và trim khoảng trắng).
4. `TC-AUTH-04`: FR-AUTH-02 & NFR-SEC-02: Đăng nhập thành công với tài khoản hợp lệ, nhận Access Token Bearer.
5. `TC-AUTH-05`: NFR-SEC-03: Đăng nhập thất bại khi tài khoản bị khóa - Bắn LockedAccountException kèm lý do.
6. `TC-AUTH-06`: NFR-SEC-04: Đăng nhập tài khoản bị DisabledException từ Spring Security chuyển thành LockedAccountException.
7. `TC-AUTH-07`: NFR-SEC-05: Đăng nhập sai mật khẩu ném BadCredentialsException.
8. `TC-AUTH-08`: Branch: Đăng nhập tài khoản bị khóa nhưng không có lockReason (lockReason = null).
9. `TC-AUTH-09`: Branch: User không tồn tại trong DB trước khi authenticate -> ném BadCredentialsException.
10. `TC-AUTH-10`: Branch: User không tồn tại trong DB trước khi authenticate nhưng ném DisabledException.
11. `TC-AUTH-11`: Branch: User có lockReason khi ném DisabledException -> đưa lý do vào LockedAccountException.
12. `TC-AUTH-12`: Branch: User null trước auth nhưng sau auth tìm thấy user trong DB -> Đăng nhập thành công.
13. `TC-AUTH-13`: Branch: User null trước auth và sau auth vẫn không tìm thấy trong DB -> ném BusinessException.
14. `TC-AUTH-14`: BUG-02 & FR-AUTH-03: Làm mới token thành công khi token metadata hợp lệ, user active và password hash khớp.
15. `TC-AUTH-15`: BUG-02: Làm mới token thất bại khi token rỗng -> ném BadCredentialsException không gọi DB/Redis.
16. `TC-AUTH-16`: BUG-02: User không tồn tại trong DB -> ném BusinessException và KHÔNG xoay vòng token trong Redis.
17. `TC-AUTH-17`: BUG-02: User bị khóa tài khoản -> ném LockedAccountException và KHÔNG xoay vòng token trong Redis.
18. `TC-AUTH-18`: BUG-02: Mật khẩu đã thay đổi sau khi cấp token -> ném BadCredentialsException và KHÔNG xoay vòng token.
19. `TC-AUTH-19`: BUG-02: Lỗi truy vấn CSDL (Database timeout/failure) -> ném ngoại lệ và KHÔNG làm mất/xoay token của client.
20. `TC-AUTH-20`: NFR-SEC-RT-05: Đăng xuất tài khoản gọi thu hồi Refresh Token trong Redis.

#### B. `RefreshTokenServiceTest` (12 Test Cases)
21. `TC-RTS-01`: BUG-03 & NFR-SEC-RT-01: Tạo Refresh Token mới thành công qua Lua script nguyên tử.
22. `TC-RTS-02`: SEC-02: Tạo token thất bại khi khóa token ngẫu nhiên bị va chạm (KEY_EXISTS).
23. `TC-RTS-03`: NFR-SEC-RT-02: Xoay vòng Refresh Token thành công qua Redis Lua script nguyên tử.
24. `TC-RTS-04`: NFR-SEC-RT-03: Xoay vòng token thất bại khi token không tồn tại trong Redis.
25. `TC-RTS-05`: BUG-01: Token đã xoay vòng nhưng TRONG grace period (10s) -> Báo phiên đang cập nhật, KHÔNG thu hồi toàn bộ.
26. `TC-RTS-06`: NFR-SEC-RT-04: Phát hiện Refresh Token bị tái sử dụng ngoài grace period -> Thu hồi toàn bộ phiên qua Lua script.
27. `TC-RTS-07`: SEC-02: Xoay vòng thất bại khi token mới bị va chạm khóa trong Redis (NEW_TOKEN_COLLISION).
28. `TC-RTS-08`: BUG-02 & FR-AUTH: Đọc metadata phiên hợp lệ bằng getTokenData thành công.
29. `TC-RTS-09`: BUG-02: getTokenData ném BadCredentialsException khi token không tồn tại.
30. `TC-RTS-10`: BUG-02: getTokenData phát hiện token ROTATED trong grace period -> Báo cập nhật từ thiết bị khác.
31. `TC-RTS-11`: NFR-SEC-RT-05: Đăng xuất (Logout) thu hồi đơn phiên Refresh Token.
32. `TC-RTS-12`: BUG-04 & NFR-SEC-RT-06: Thu hồi toàn bộ phiên (Revoke All Sessions) qua Lua script nguyên tử.

#### C. `UserServiceTest` (23 Test Cases)
33. `TC-USR-01`: FR-USER-01: Lấy hồ sơ người dùng đang đăng nhập thành công.
34. `TC-USR-02`: NFR-REL-01: Lấy hồ sơ người dùng ném ResourceNotFoundException nếu không tìm thấy trong DB.
35. `TC-USR-03`: NFR-PERF-01 & FR-ADMIN-01: Lấy danh sách toàn bộ người dùng có phân trang (Pagination).
36. `TC-USR-04`: FR-ADMIN-02 & NFR-SEC-06: Khóa tài khoản thành công - Lưu lý do và thời điểm khóa.
37. `TC-USR-05`: NFR-SEC-07: Admin không thể tự khóa tài khoản của chính mình (Self-Locking Prevention).
38. `TC-USR-06`: NFR-SEC-08: Không thể khóa tài khoản đã bị khóa từ trước.
39. `TC-USR-07`: NFR-DATA-03: Khóa tài khoản thất bại nếu lý do khóa rỗng hoặc null.
40. `TC-USR-08`: NFR-DATA-04: Khóa tài khoản thất bại nếu lý do khóa vượt quá 500 ký tự.
41. `TC-USR-09`: NFR-REL-02: Khóa tài khoản ném ResourceNotFoundException nếu ID người dùng không tồn tại.
42. `TC-USR-10`: FR-ADMIN-03 & NFR-SEC-09: Mở khóa tài khoản thành công - Xóa lý do và thời điểm khóa.
43. `TC-USR-11`: NFR-SEC-10: Mở khóa tài khoản thất bại nếu tài khoản đang hoạt động bình thường.
44. `TC-USR-12`: NFR-REL-03: Mở khóa tài khoản ném ResourceNotFoundException nếu ID không tồn tại.
45. `TC-USR-13`: FR-USER-02 & NFR-DATA-05: Cập nhật hồ sơ người dùng (Partial Update) thành công.
46. `TC-USR-14`: FR-USER-03 & NFR-SEC-11: Đổi mật khẩu thành công - Băm mật khẩu mới bằng BCrypt.
47. `TC-USR-15`: NFR-SEC-12: Đổi mật khẩu thất bại khi mật khẩu xác nhận không khớp.
48. `TC-USR-16`: NFR-SEC-13: Đổi mật khẩu thất bại khi mật khẩu hiện tại không đúng.
49. `TC-USR-17`: NFR-SEC-14: Đổi mật khẩu thất bại khi mật khẩu mới trùng với mật khẩu cũ.
50. `TC-USR-18`: Branch: Khóa tài khoản thành công khi adminUserDetails là null (log UNKNOWN).
51. `TC-USR-19`: Branch: Khóa tài khoản với request là null -> ném BusinessException lý do trống.
52. `TC-USR-20`: Branch: Khóa tài khoản với request.getReason() là null -> ném BusinessException lý do trống.
53. `TC-USR-21`: Branch: Mở khóa tài khoản khi adminUserDetails là null (log UNKNOWN).
54. `TC-USR-22`: Branch: Cập nhật hồ sơ với tất cả các trường null -> giữ nguyên toàn bộ giá trị cũ.
55. `TC-USR-23`: Branch: Cập nhật hồ sơ với fullName là khoảng trắng -> giữ nguyên fullName cũ.

---

### 5.2. Nhóm Bảo mật & JWT (`com.example.ecommerce.security` — 25 Test Cases)

#### A. `JwtAuthenticationFilterTest` (13 Test Cases)
56. `TC-FLT-01`: Branch: Header Authorization không tồn tại -> Bỏ qua xác thực, tiếp tục filter chain.
57. `TC-FLT-02`: Branch: Header Authorization rỗng hoặc chỉ chứa khoảng trắng -> Bỏ qua xác thực.
58. `TC-FLT-03`: Branch: Header Authorization không bắt đầu bằng Bearer -> Bỏ qua xác thực.
59. `TC-FLT-04`: Branch: Header Bearer nhưng chuỗi token phía sau rỗng -> Bỏ qua xác thực.
60. `TC-FLT-05`: Branch: Token hợp lệ đầy đủ điều kiện -> Thiết lập Authentication thành công vào SecurityContext.
61. `TC-FLT-06`: Branch: Token hợp lệ nhưng SecurityContext đã có Authentication từ trước -> Không ghi đè.
62. `TC-FLT-07`: Branch: Token có subject (email) rỗng -> Bỏ qua không xác thực.
63. `TC-FLT-08`: Branch: UserDetails bị disabled (tài khoản bị khóa) -> Không thiết lập Authentication.
64. `TC-FLT-09`: Branch: Email trong claims không khớp với username trong UserDetails -> Không thiết lập Authentication.
65. `TC-FLT-10`: Branch: Claims có expiration là null hoặc đã quá hạn -> Không thiết lập Authentication.
66. `TC-FLT-11`: Branch: Token hết hạn ném ExpiredJwtException -> Bắt ngoại lệ và tiếp tục filter chain.
67. `TC-FLT-12`: Branch: Token lỗi định dạng ném MalformedJwtException -> Bắt ngoại lệ và tiếp tục filter chain.
68. `TC-FLT-13`: Branch: Lỗi không xác định khi parse token -> Bắt Exception chung và tiếp tục filter chain.

#### B. `JwtUtilsTest` (9 Test Cases)
69. `TC-JWT-01`: NFR-SEC-15: Sinh token JWT hợp lệ với email làm subject.
70. `TC-JWT-02`: NFR-SEC-16: Xác thực token hợp lệ cho đúng người dùng.
71. `TC-JWT-03`: NFR-SEC-17: Từ chối token nếu subject không khớp với UserDetails.
72. `TC-JWT-04`: NFR-SEC-18: Từ chối token bị sửa đổi hoặc sai chữ ký (Invalid Signature).
73. `TC-JWT-05`: NFR-SEC-19: Từ chối token đã hết hạn (Expired JWT).
74. `TC-JWT-06`: NFR-SEC-19B: Từ chối token có định dạng malformed (MalformedJwtException).
75. `TC-JWT-07`: NFR-SEC-19C: Từ chối token rỗng hoặc null (IllegalArgumentException).
76. `TC-JWT-08`: NFR-SEC-19D: Kiểm tra token còn hạn bằng isTokenExpired trả về false.
77. `TC-JWT-09`: NFR-SEC-19E: Kiểm tra token hết hạn bằng isTokenExpired ném ExpiredJwtException do JJWT xác thực hạn.

#### C. `CustomUserDetailsServiceTest` (3 Test Cases)
78. `TC-UDS-01`: FR-SEC-01: Tải UserDetails thành công cho tài khoản đang hoạt động.
79. `TC-UDS-02`: NFR-SEC-20: Tài khoản bị khóa có trạng thái isEnabled = false trong UserDetails.
80. `TC-UDS-03`: NFR-SEC-21: Người dùng không tồn tại ném UsernameNotFoundException bảo mật.

---

### 5.3. Nhóm Controller (`com.example.ecommerce.controller` — 24 Test Cases)

#### A. `AdminUserControllerTest` (10 Test Cases)
81. `TC-ADM-01`: NFR-SEC-22 & FR-ADMIN-04: Admin truy cập GET /api/v1/admin/users thành công trả 200 OK.
82. `TC-ADM-02`: NFR-SEC-23: Khách hàng (CUSTOMER) truy cập GET /api/v1/admin/users bị từ chối 403 FORBIDDEN.
83. `TC-ADM-03`: NFR-SEC-24: Người dùng chưa đăng nhập truy cập GET /api/v1/admin/users bị từ chối 401 UNAUTHORIZED.
84. `TC-ADM-04`: FR-ADMIN-05 & NFR-SEC-25: Admin khóa tài khoản thành công PATCH /api/v1/admin/users/{id}/lock trả 200 OK.
85. `TC-ADM-05`: NFR-DATA-06: Admin khóa tài khoản với lý do trống bị Bean Validation trả 400 BAD REQUEST.
86. `TC-ADM-06`: NFR-SEC-26: Khách hàng (CUSTOMER) gọi PATCH /api/v1/admin/users/{id}/lock bị chặn 403 FORBIDDEN.
87. `TC-ADM-07`: FR-ADMIN-06 & NFR-SEC-27: Admin mở khóa tài khoản thành công PATCH /api/v1/admin/users/{id}/unlock trả 200 OK.
88. `TC-ADM-08`: NFR-SEC-28: Khách hàng (CUSTOMER) gọi PATCH /api/v1/admin/users/{id}/unlock bị chặn 403 FORBIDDEN.
89. `TC-ADM-09`: Branch: Admin gọi getUsers với page âm và size <= 0 -> chuẩn hóa về page=0 và size=DEFAULT_PAGE_SIZE(10).
90. `TC-ADM-10`: Branch: Admin gọi getUsers với size vượt quá MAX_PAGE_SIZE(100) -> giới hạn về MAX_PAGE_SIZE.

#### B. `AuthControllerTest` (6 Test Cases)
91. `TC-ATC-01`: FR-AUTH-03: Đăng ký tài khoản với dữ liệu hợp lệ trả 201 CREATED.
92. `TC-ATC-02`: NFR-DATA-07: Đăng ký với email sai định dạng bị Bean Validation trả 400 BAD REQUEST.
93. `TC-ATC-03`: NFR-DATA-08: Đăng ký với mật khẩu ngắn hơn 6 ký tự bị Bean Validation trả 400 BAD REQUEST.
94. `TC-ATC-04`: FR-AUTH-04: Đăng nhập thành công trả 200 OK kèm theo Bearer JWT.
95. `TC-ATC-05`: NFR-SEC-29: Đăng nhập với tài khoản bị khóa trả 403 FORBIDDEN kèm lý do khóa.
96. `TC-ATC-06`: NFR-SEC-30: Đăng nhập sai email hoặc mật khẩu trả 401 UNAUTHORIZED.

#### C. `UserControllerTest` (6 Test Cases)
97. `TC-UCT-01`: FR-USER-01: Lấy thông tin cá nhân hiện tại GET /api/v1/users/me thành công trả 200 OK.
98. `TC-UCT-02`: NFR-SEC: Truy cập GET /api/v1/users/me khi chưa đăng nhập trả 401 UNAUTHORIZED.
99. `TC-UCT-03`: FR-USER-02: Cập nhật thông tin cá nhân PUT /api/v1/users/me thành công trả 200 OK.
100. `TC-UCT-04`: NFR-DATA: Cập nhật thông tin với số điện thoại sai định dạng trả 400 BAD REQUEST.
101. `TC-UCT-05`: FR-USER-03: Đổi mật khẩu PUT /api/v1/users/me/password thành công trả 200 OK.
102. `TC-UCT-06`: NFR-DATA: Đổi mật khẩu với mật khẩu mới rỗng trả 400 BAD REQUEST.

#### D. `HealthControllerTest` (2 Test Cases)
103. `TC-HLT-01`: Health Check: GET /api/v1/health trả 200 OK với status UP.
104. `TC-HLT-02`: Health Check: GET /api/health trả 200 OK với status UP.

---

### 5.4. Nhóm Xử lý Ngoại lệ (`com.example.ecommerce.exception` — 13 Test Cases)

#### `GlobalExceptionHandlerTest` (13 Test Cases)
105. `TC-EXC-01`: NFR-ERR-01: NoResourceFoundException trả về 404 NOT_FOUND.
106. `TC-EXC-02`: NFR-ERR-02: ResourceNotFoundException trả về 404 NOT_FOUND.
107. `TC-EXC-03`: NFR-ERR-02: RedisConnectionFailureException trả về 503 SERVICE_UNAVAILABLE.
108. `TC-EXC-04`: NFR-ERR-03: BusinessException trả về 400 BAD_REQUEST.
109. `TC-EXC-05`: NFR-ERR-04: BadCredentialsException trả về 401 UNAUTHORIZED.
110. `TC-EXC-06`: NFR-ERR-05: LockedAccountException trả về 403 FORBIDDEN kèm lý do khóa.
111. `TC-EXC-07`: NFR-ERR-06: DisabledException trả về 403 FORBIDDEN.
112. `TC-EXC-08`: NFR-ERR-07: AccessDeniedException trả về 403 FORBIDDEN.
113. `TC-EXC-09`: NFR-ERR-08: General Exception trả về 500 INTERNAL_SERVER_ERROR.
114. `TC-EXC-10`: Branch: LockedAccountException không có reason -> data trả về null.
115. `TC-EXC-11`: Branch: DisabledException với lý do tùy biến -> data trả về reason và lockReason.
116. `TC-EXC-12`: Branch: DisabledException với message rỗng -> data trả về null.
117. `TC-EXC-13`: Branch: ResourceNotFoundException với resource name và id.

---

### 5.5. Nhóm Entity & Application Lifecycle (3 Test Cases)

#### A. `UserTest` (`com.example.ecommerce.entity` — 2 Test Cases)
118. `TC-ENT-01`: User Entity: Kiểm tra onCreate thiết lập createdAt và updatedAt.
119. `TC-ENT-02`: User Entity: Kiểm tra onUpdate cập nhật updatedAt.

#### B. `EcommerceApplicationTests` (`com.example.ecommerce` — 1 Test Case)
120. `TC-APP-01`: contextLoads() - Khởi động thành công toàn bộ Spring Boot Application Context với profile `test`.

---

## 6. KẾT QUẢ KIỂM TRA THỦ CÔNG REDIS (MANUAL VERIFICATION)

> [!IMPORTANT]
> **Lưu ý về nguồn gốc dữ liệu:** Các mục dưới đây là số liệu kiểm tra thủ công đã được cung cấp từ môi trường kiểm thử độc lập trước đó, **không phải kết quả vừa chạy lại trong phiên hiện tại**. Không có lệnh runtime hay terminal nào được thực thi trong lần cập nhật này.

### 6.1. Dữ liệu ghi nhận từ Redis CLI
- **Kết nối Redis:** Lệnh `PING` phản hồi `PONG`.
- **Thông tin phiên bản & chế độ:** Redis server phiên bản `7.4.11`, chế độ hoạt động `standalone`, cổng mặc định `6379`.
- **Kích thước CSDL:** Lệnh `DBSIZE` phản hồi `2` keys đang lưu trữ.
- **Trạng thái lưu trữ Refresh Token:**
  - Key dạng `auth:refresh:<hash>` với type là `string`, TTL còn lại ghi nhận khoảng `604707` giây (~7 ngày).
  - Key dạng `auth:user_sessions:5` với type là `zset`, TTL còn lại ghi nhận khoảng `604706` giây.
- **Phản hồi API Đăng nhập:** Endpoint trả về định dạng JSON gồm `success: true`, chuỗi `accessToken` và `expiresIn: 900` (15 phút).

### 6.2. Giới hạn đánh giá và ranh giới bảo mật
- **Không suy diễn quá phạm vi:** Kết quả kiểm tra trên chỉ xác nhận Redis đang phản hồi và key phiên cơ bản đã được tạo sau một lượt đăng nhập đơn lẻ. **Không thể kết luận toàn bộ luồng xoay vòng token đồng thời (Concurrent Rotation), phát hiện tái sử dụng ngoài Grace Period, hay thu hồi khi đăng xuất đã hoạt động hoàn hảo trên môi trường thực tế** nếu chưa có bộ Integration Test tự động kết nối Redis thật.
- **Cookie Refresh Token:** Chưa có bằng chứng xác minh trực tiếp header `Set-Cookie` trên HTTP client hoặc cookie container của trình duyệt; do đó không khẳng định cookie `HttpOnly` đã được thiết lập đúng cấu hình browser chỉ qua dữ liệu Redis CLI.
- **Bảo vệ thông tin nhạy cảm:** Báo cáo tuân thủ nguyên tắc an ninh, lược bỏ toàn bộ giá trị hash token thực tế và secret key khỏi tài liệu.

---

## 7. ĐÁNH GIÁ MỤC TIÊU BRANCH COVERAGE VÀ KHUYẾN NGHỊ

### 7.1. Phân tích khoảng cách Branch Coverage (76% so với mục tiêu ≥ 80%)
Hiện tại, tổng số nhánh rẽ là **172**, trong đó đã bao phủ **132 nhánh (76%)**, còn thiếu **40 nhánh** để đạt 100% và thiếu **6 nhánh** để vượt ngưỡng tối thiểu **80% (138 / 172 = 80.2%)**.

Phân bổ 40 nhánh chưa được bao phủ:
1. **`com.example.ecommerce.controller` (11 branches missed / 14 total):**
   - Lớp `AuthController` chỉ mới đạt **8% Branch Coverage (1 / 12 branches)**. Nguyên nhân: `AuthControllerTest` hiện tại chỉ kiểm thử MockMvc cho 2 endpoint ban đầu là `/register` và `/login`. Hai endpoint mới được thêm trong Giai đoạn 2 là `POST /api/v1/auth/refresh` và `POST /api/v1/auth/logout` chưa có test MockMvc nào tại tầng Controller.
2. **`com.example.ecommerce.util` (3 branches missed / 4 total):**
   - Lớp `CookieUtils` mới đạt **25% Branch Coverage (1 / 4 branches)** do thiếu unit test trực tiếp cho các phương thức trích xuất và tạo cookie phản hồi.
3. **`com.example.ecommerce.service` (24 branches missed / 116 total):**
   - Lớp `RefreshTokenService` có 21 nhánh chưa được bao phủ (chủ yếu là các nhánh xử lý ngoại lệ Redis kết nối gián đoạn, phản hồi lỗi từ Lua script, và kiểm tra điều kiện biên JSON).
   - Lớp `AuthService` có 3 nhánh chưa được bao phủ (các trường hợp phân nhánh liên quan đến kiểm tra mật khẩu hash null/mismatch).
4. **`com.example.ecommerce.security` (2 branches missed / 28 total):**
   - Lớp `JwtUtils`: 1 nhánh biên tại `validateToken` do JJWT ném `ExpiredJwtException` trước khi so sánh logic.
   - Lớp `JwtAuthenticationFilter`: 1 nhánh biên khi kiểm tra `StringUtils.hasText`.

### 7.2. Kế hoạch và khuyến nghị hành động để đạt ≥ 80% Branch Coverage
Để nâng Branch Coverage từ **76% lên trên 85%**, các bước tối ưu cần thực hiện bao gồm:

1. **Bổ sung MockMvc Test cho `AuthController` (Ưu tiên cao nhất — cải thiện ngay +14 branches):**
   - Viết test cho `POST /api/v1/auth/refresh`:
     + Request không có cookie `refresh_token` -> Trả 401 Unauthorized.
     + Request có cookie rỗng -> Trả 401 Unauthorized.
     + Request có cookie hợp lệ -> Trả 200 OK kèm access token mới và header `Set-Cookie` mới.
   - Viết test cho `POST /api/v1/auth/logout`:
     + Đăng xuất có cookie -> Gọi revoke, xóa cookie (`Max-Age=0`), trả 200 OK.
     + Đăng xuất không có cookie -> Vẫn xóa cookie trên trình duyệt an toàn, trả 200 OK.
   - *Hiệu quả:* Bao phủ ngay 11 nhánh trong `AuthController` và 3 nhánh trong `CookieUtils`, đưa Branch Coverage tổng thể từ 76% lên **84.3% (145 / 172 branches)**, vượt mục tiêu 80%.

2. **Bổ sung Unit Test cho `CookieUtils`:**
   - Kiểm tra `extractRefreshToken` khi request không có cookie nào (`request.getCookies() == null`).
   - Kiểm tra khi danh sách cookies không chứa cookie mang tên `refresh_token`.

3. **Bổ sung Unit Test cho các nhánh biên của `RefreshTokenService`:**
   - Giả lập Redis ném ngoại lệ khi execute Lua script.
   - Giả lập dữ liệu JSON trong Redis bị hỏng cấu trúc (JSON parse error).

---

## 8. HƯỚNG DẪN THỰC THI VÀ XEM BÁO CÁO

### 8.1. Lệnh kiểm thử tự động
```powershell
cd backend
.\gradlew clean test jacocoTestReport
```

### 8.2. Đường dẫn các báo cáo trực quan trong dự án
- **Báo cáo kết quả kiểm thử JUnit:**  
  `backend/build/reports/tests/test/index.html` (120 tests, 0 failures, 100% success rate, duration: 4.011s)
- **Báo cáo độ bao phủ JaCoCo:**  
  `backend/build/reports/jacoco/test/html/index.html` (89% instruction coverage, 76% branch coverage)
- **File chi tiết kết quả từng Test Class (XML):**  
  `backend/build/test-results/test/TEST-*.xml` (13 test suites tương ứng 120 test cases)

---

## 9. BÁO CÁO KIỂM THỬ TÍCH HỢP FRONTEND & HỆ THỐNG DEVOPS (GIAI ĐOẠN 3 & 4)

> **Người thực hiện:** Đỗ Hoàng Anh — Senior Frontend & DevOps Engineer (`feature/frontend-devops`)  
> **Thời điểm xác minh:** 23/10/2026, 02:30:00 (GMT+7)  
> **Môi trường xác minh:** Windows 11 + Docker Desktop (WSL2 engine) + Nginx 1.27 + Spring Boot 3.3.5 (Java 21) + MySQL 8.0 + Redis 7

### 9.1. Kết quả kiểm tra Build mã nguồn Frontend (React + Vite)
- **Công cụ build:** Vite 5.4.21, React 18, TailwindCSS.
- **Lệnh thực thi:** `npm install --no-audit --no-fund && npm run build`
- **Kết quả:** **BUILD THÀNH CÔNG (0 Lỗi, 0 Warning)**
- **Tối ưu hóa Rollup Chunks (`vite.config.js`):**
  - Đã cấu hình `manualChunks` phân tách rõ ràng:
    + `dist/assets/vendor-*.js`: Thư viện lõi runtime.
    + `dist/assets/icons-*.js` (34.76 kB / gzip 7.62 kB): Bộ icon `lucide-react`.
    + `dist/assets/store-*.js` (52.10 kB / gzip 19.78 kB): Quản lý state `zustand` + `axios`.
    + `dist/assets/router-*.js` (162.87 kB / gzip 53.15 kB): Hệ thống định tuyến `react-router-dom`.
    + `dist/assets/index-*.js` (270.03 kB / gzip 50.75 kB): Giao diện các trang và components.
  - Loại bỏ hoàn toàn cảnh báo vượt kích thước bundle chunk (>500 kB).

### 9.2. Rà soát Giao diện, Phân quyền & Ràng buộc Form (Frontend Validation)
1. **Giao diện dùng chung & Admin Layout:**
   - Hoàn thiện `CustomerLayout`, `Navbar`, `Footer`, `AdminLayout`, `AdminHeader`, `AdminSidebar`.
   - Đáp ứng Responsive 100% trên cả 3 kích thước màn hình Mobile (< 768px), Tablet (768px – 1024px) và Desktop (> 1024px).
   - Badge hiển thị giỏ hàng trên Header tự động tính toán tổng số lượng sản phẩm từ `useCartStore`.
   - Hiển thị menu phù hợp theo vai trò: Khách (Guest), Khách hàng (`CUSTOMER`) và Quản trị viên (`ADMIN`).
2. **Hệ thống Routing & Route Guards:**
   - `ProtectedRoute.jsx`: Bảo vệ các trang yêu cầu đăng nhập (Profile, Checkout, Orders).
   - `GuestRoute.jsx`: Chuyển hướng người dùng đã đăng nhập khi truy cập `/login`, `/register`.
   - `RoleRoute.jsx`: Kiểm soát phân quyền nghiêm ngặt các trang quản trị `/admin/*` (chỉ cho phép `ROLE_ADMIN`).
   - `NotFoundPage.jsx` (404) & `ForbiddenPage.jsx` (403): Thiết kế trực quan, có nút quay về trang chủ.
3. **Kiểm tra và Ràng buộc Form (Form Validation):**
   - `LoginPage.jsx` & `RegisterPage.jsx`: Kiểm tra hợp lệ email (regex RFC 5322), độ dài mật khẩu (>= 6 ký tự), xác nhận mật khẩu khớp nhau, họ tên và số điện thoại.
   - `ProfilePage.jsx`: Ràng buộc cập nhật thông tin cá nhân và quy trình đổi mật khẩu 3 bước (mật khẩu hiện tại, mật khẩu mới, xác nhận mật khẩu).
   - `CartPage.jsx` & `CheckoutPage.jsx`:
     + Khống chế số lượng mua `1 <= quantity <= stockQuantity`, ngăn chặn tràn số lượng vượt tồn kho hoặc nhập số âm.
     + Ràng buộc số điện thoại Việt Nam chuẩn định dạng (`/^(0|\+84)[0-9]{9}$/`).
     + Ràng buộc địa chỉ giao hàng chi tiết và phương thức thanh toán hợp lệ (COD / VNPAY / Momo).
   - Các trang Quản trị (`AdminProductsPage.jsx`, `AdminCategoriesPage.jsx`, `AdminOrdersPage.jsx`, `AdminInventoryPage.jsx`, `UserManagementPage.jsx`): Kiểm soát dữ liệu đầu vào khi tạo/sửa sản phẩm (giá > 0, tồn kho >= 0, chọn đúng danh mục), thêm danh mục, điều chỉnh kho và cập nhật trạng thái đơn hàng.
4. **Xử lý sự cố giao diện & Trải nghiệm người dùng:**
   - Tích hợp fallback ảnh tự động (`onError`) khi link hình ảnh sản phẩm hỏng hoặc không tải được.
   - Thông báo lỗi bằng tiếng Việt dễ hiểu trên toàn bộ các thao tác qua Toast thông báo.
   - Hiển thị Empty State và Loading Skeleton khi đang tải dữ liệu hoặc danh sách trống.

### 9.3. Kết quả Khởi chạy & Kiểm thử Hệ thống Container (Docker Compose)
Toàn bộ hệ sinh thái container được điều phối qua `docker-compose.yml` và đã khởi chạy thành công 100%:

| Tên Container | Dịch vụ | Image | Trạng thái (Health Status) | Cổng Host -> Container | Chức năng xác minh |
| :--- | :--- | :--- | :---: | :---: | :--- |
| **`ecommerce-mysql`** | `mysql` | `mysql:8.0` | **Up (healthy)** | `3307:3306` | CSDL quan hệ chính, khởi tạo schema và seed data ban đầu thành công. |
| **`ecommerce-redis`** | `redis` | `redis:7-alpine` | **Up (healthy)** | `6379:6379` | Quản lý phiên làm việc Opaque Refresh Token, ZSET và bộ nhớ đệm cache. |
| **`ecommerce-backend`** | `backend` | `project-rikkei-ecommerce-develop-backend:latest` | **Up (healthy)** | `8080:8080` | Spring Boot 3.3.5 / Java 21, multi-stage build, non-root user `spring`. |
| **`ecommerce-frontend`** | `frontend` | `project-rikkei-ecommerce-develop-frontend:latest` | **Up (healthy)** | `3000:80` | Nginx 1.27-Alpine phục vụ SPA bundle và reverse proxy `/api` sang backend. |

### 9.4. Kết quả Kiểm tra Trực tiếp các Endpoint (Smoke Test & E2E Verification)

```bash
# 1. Kiểm tra Frontend Web (Nginx SPA)
$ curl.exe -i http://localhost:3000/
HTTP/1.1 200 OK
Server: nginx/1.27.5
Content-Type: text/html
-> Phản hồi mã nguồn HTML của ứng dụng React thành công.

# 2. Kiểm tra Nginx Reverse Proxy từ Frontend sang Backend
$ curl.exe -i http://localhost:3000/api/v1/products
HTTP/1.1 200 OK
Server: nginx/1.27.5
Content-Type: application/json
-> Nginx chuyển hướng `/api` sang container `backend:8080` thông suốt.

# 3. Kiểm tra Spring Boot Actuator Health Check
$ curl.exe -s http://localhost:8080/actuator/health
{
  "status": "UP",
  "components": {
    "db": { "status": "UP", "details": { "database": "MySQL", "validationQuery": "isValid()" } },
    "redis": { "status": "UP", "details": { "version": "7.4.11" } },
    "ping": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
-> Toàn bộ kết nối cơ sở dữ liệu MySQL và Redis hoạt động hoàn hảo.

# 4. Kiểm tra Backend Products API
$ curl.exe -s http://localhost:8080/api/v1/products | jq .data.totalElements
14
-> Trả về đúng 14 sản phẩm mẫu theo chuẩn cấu trúc ApiResponse<Page<ProductResponse>>.

# 5. Kiểm tra Backend Categories API
$ curl.exe -s http://localhost:8080/api/v1/categories | jq .data.[].name
"Điện tử"
"Gia dụng"
"Sách"
"Thể thao"
"Thời trang"
-> Danh mục sản phẩm hoạt động chuẩn xác.
```

### 9.5. Tối ưu hóa CI/CD (.github/workflows/ci.yml)
- Bổ sung theo dõi nhánh `develop` và các nhánh tính năng `feature/**`.
- Tách bạch và tự động hóa 3 jobs chính:
  1. `backend-test`: Kiểm tra build Gradle, chạy trọn vẹn 120 automated tests và xuất báo cáo JaCoCo coverage.
  2. `frontend-build`: Thiết lập Node.js 20, cài đặt phụ thuộc `npm ci`, kiểm tra linting và chạy `npm run build`.
  3. `docker-build`: Kiểm tra tính khả thi khi build container images cho cả Backend và Frontend trên runner Ubuntu.

