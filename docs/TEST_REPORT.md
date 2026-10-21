# BÁO CÁO KẾT QUẢ KIỂM THỬ VÀ ĐÁP ỨNG YÊU CẦU PHI CHỨC NĂNG
## DỰ ÁN: RIKKEI ECOMMERCE PLATFORM

> **Tiêu chí đánh giá:** Hoàn thành các yêu cầu phi chức năng (NFR) và có các test case, test result cụ thể đạt ở ngưỡng **≥ 80%**.  
> **Kết quả thực tế sau tối ưu hóa:**
> - **Tỷ lệ vượt qua kiểm thử (Pass Rate):** **100% (100/100 Test Cases PASSED)**
> - **Độ bao phủ câu lệnh (Instruction Coverage):** **99%** (1.528 / 1.540 instructions)
> - **Độ bao phủ nhánh rẽ (Branch Coverage):** **97%** (84 / 86 branches)
> - **Thời gian thực thi:** 7.2 giây

---

## 1. TỔNG QUAN CHIẾN LƯỢC KIỂM THỬ (TESTING STRATEGY)

Để đảm bảo hệ thống thương mại điện tử hoạt động tin cậy, an toàn và đạt chuẩn công nghiệp, chiến lược kiểm thử được thiết lập theo mô hình kim tự tháp (Testing Pyramid):

1. **Unit Tests (Kiểm thử đơn vị):**
   - Sử dụng **JUnit 5 (Jupiter)** và **Mockito** để cô lập hoàn toàn các phụ thuộc ngoại vi (Database, Network, External Services).
   - Kiểm thử toàn bộ các nhánh điều kiện biên, kiểm tra giá trị null/empty, ngoại lệ runtime trong `AuthService`, `UserService`, `JwtUtils`, `CustomUserDetailsService`, `JwtAuthenticationFilter` và `GlobalExceptionHandler`.
2. **Integration Tests & Security Tests (Kiểm thử tích hợp & Bảo mật):**
   - Sử dụng `@SpringBootTest` kết hợp `@AutoConfigureMockMvc` và Spring Security Test (`@WithMockUser`).
   - Sử dụng **H2 In-Memory Database** với profile `test`, giả lập chính xác môi trường runtime mà không ảnh hưởng tới CSDL thực.
   - Kiểm tra toàn bộ chuỗi Filter Security, RBAC (Role-Based Access Control), Bean Validation (`@Valid`), và xử lý lỗi tập trung qua `AdminUserControllerTest`, `AuthControllerTest`, `UserControllerTest`, `HealthControllerTest`.
3. **Đo lường độ bao phủ (Code Coverage):**
   - Tích hợp công cụ **JaCoCo (Java Code Coverage)** phiên bản 0.8.12.
   - Đo lường chi tiết cả Instruction Coverage và Branch Coverage.

---

## 2. MA TRẬN ĐÁP ỨNG YÊU CẦU PHI CHỨC NĂNG (NFR MATRIX)

Hệ thống được thiết kế và kiểm thử nghiêm ngặt để thỏa mãn 4 nhóm Yêu cầu Phi Chức Năng (NFR) cốt lõi:

| Mã NFR | Nhóm yêu cầu | Nội dung đặc tả yêu cầu phi chức năng | Cơ chế hiện thực trong hệ thống | Trạng thái kiểm thử |
| :--- | :--- | :--- | :--- | :---: |
| **NFR-SEC-01** | Bảo mật | **Băm mật khẩu an toàn:** Không lưu plain-text mật khẩu trong CSDL. | Sử dụng `BCryptPasswordEncoder` với cost factor = 12. | **PASSED** (100%) |
| **NFR-SEC-02** | Bảo mật | **Xác thực JWT Stateless:** Token mã hóa bằng thuật toán HMAC-SHA256 với secret key an toàn (≥ 256-bit). | Sử dụng thư viện JJWT 0.12.6, cấu hình thời hạn token và ký số an toàn. | **PASSED** (100%) |
| **NFR-SEC-03** | Bảo mật | **Phân quyền truy cập RBAC:** Endpoint quản trị chỉ dành riêng cho Admin; chặn Customer và Guest. | `@PreAuthorize("hasRole('ADMIN')")` và cấu hình phân quyền trong `SecurityConfig`. | **PASSED** (100%) |
| **NFR-SEC-04** | Bảo mật | **Khóa tài khoản và thu hồi quyền tức thì:** Tài khoản bị Admin khóa không thể đăng nhập hoặc dùng JWT cũ. | Bắt `LockedAccountException` tại `AuthService`, set `disabled=true` trong `CustomUserDetailsService` và chặn trong `JwtAuthenticationFilter`. | **PASSED** (100%) |
| **NFR-SEC-05** | Bảo mật | **Ngăn ngừa tự khóa tài khoản (Self-Locking Prevention):** Admin không thể tự khóa chính mình. | Kiểm tra email từ SecurityContext (`UserDetails`) và chặn ném `BusinessException`. | **PASSED** (100%) |
| **NFR-DATA-01** | Toàn vẹn dữ liệu | **Email duy nhất:** Không cho phép đăng ký tài khoản có email trùng lặp. | Kiểm tra `userRepository.existsByEmail()` và ràng buộc duy nhất trong CSDL. | **PASSED** (100%) |
| **NFR-DATA-02** | Toàn vẹn dữ liệu | **Chuẩn hóa dữ liệu đầu vào:** Tự động chuẩn hóa email (lowercase, trim khoảng trắng). | Xử lý `request.getEmail().toLowerCase().trim()` trước khi query và lưu trữ. | **PASSED** (100%) |
| **NFR-DATA-03** | Toàn vẹn dữ liệu | **Xác thực ràng buộc nghiệp vụ (Bean Validation):** Kiểm tra email hợp lệ, độ dài mật khẩu (≥ 6), lý do khóa bắt buộc (1–500 ký tự). | Sử dụng Hibernate Validator (`@NotBlank`, `@Email`, `@Size`, `@Pattern`). | **PASSED** (100%) |
| **NFR-REL-01** | Tin cậy & Xử lý lỗi | **Chuẩn hóa phản hồi API:** Tất cả response (thành công hoặc lỗi) tuân thủ cấu trúc chuẩn `ApiResponse`. | `GlobalExceptionHandler` kết hợp cùng `ApiResponse<T>` chuẩn hóa. | **PASSED** (100%) |
| **NFR-REL-02** | Tin cậy & Xử lý lỗi | **Bảo vệ thông tin đăng nhập:** Không tiết lộ lý do cụ thể khi đăng nhập sai (tránh User Enumeration attack). | Ném `BadCredentialsException` trả 401 với thông điệp chung "Email hoặc mật khẩu không đúng". | **PASSED** (100%) |
| **NFR-PERF-01**| Hiệu năng & Mở rộng | **Phân trang bảo vệ tài nguyên (Pagination Safeguard):** Giới hạn `MAX_PAGE_SIZE = 100`, chuẩn hóa `pageIndex >= 0`. | Cắt tỉa tham số `size` và `page` trong `AdminUserController`, mặc định 10, tối đa 100. | **PASSED** (100%) |
| **NFR-PERF-02**| Hiệu năng & Bảo mật | **Chống rò rỉ dữ liệu nhạy cảm (Data Exposure Prevention):** Không bao giờ trả mật khẩu hash trong API. | Chuyển đổi User Entity sang `UserResponse` DTO loại bỏ hoàn toàn trường password. | **PASSED** (100%) |

---

## 3. BẢNG ĐỐI CHIẾU KẾT QUẢ TRƯỚC VÀ SAU KHI NÂNG BRANCH COVERAGE

| Chỉ số kiểm thử | Trước khi bổ sung | Sau khi bổ sung (Hiện tại) | Mức độ cải thiện |
| :--- | :---: | :---: | :---: |
| **Tổng số test cases** | 55 | **100** | +45 test cases (+81.8%) |
| **Số test PASS** | 55 | **100** | **100% tỷ lệ thành công** |
| **Số test FAIL / IGNORED** | 0 | **0** | Tuyệt đối an toàn |
| **Instruction Coverage (Bao phủ câu lệnh)** | 84% (1.294 / 1.540) | **99%** (1.528 / 1.540) | **+15%** |
| **Branch Coverage (Bao phủ nhánh rẽ)** | 55% (48 / 86) | **97%** (84 / 86) | **+42% (Vượt xa mục tiêu 80%)** |

### Chi tiết Branch Coverage theo từng Package:
- **`com.example.ecommerce.service`**: **100%** (46 / 46 branches) — *Trước: 71%*
- **`com.example.ecommerce.exception`**: **100%** (10 / 10 branches) — *Trước: 50%*
- **`com.example.ecommerce.controller`**: **100%** (2 / 2 branches) — *Trước: 50%*
- **`com.example.ecommerce.security`**: **92%** (26 / 28 branches) — *Trước: 32%*
- **`com.example.ecommerce.config`**: **100%** Instruction Coverage
- **`com.example.ecommerce.dto.response`**: **100%** Instruction Coverage
- **`com.example.ecommerce.entity`**: **100%** Instruction Coverage

---

## 4. DANH SÁCH CHI TIẾT 100 TEST CASES

### 4.1. Nhóm Xác thực Dịch vụ (`AuthServiceTest` - 13 Test Cases)
Thư mục: `backend/src/test/java/com/example/ecommerce/service/AuthServiceTest.java`

1. `TC-AUTH-01`: Đăng ký tài khoản mới thành công (Mật khẩu băm BCrypt, role mặc định CUSTOMER).
2. `TC-AUTH-02`: Đăng ký thất bại khi email đã tồn tại (ném BusinessException).
3. `TC-AUTH-03`: Tự động chuẩn hóa email khi đăng ký (lowercase và trim).
4. `TC-AUTH-04`: Đăng nhập thành công với tài khoản hợp lệ (nhận JWT Bearer).
5. `TC-AUTH-05`: Chặn đăng nhập tài khoản bị khóa (ném LockedAccountException kèm lý do).
6. `TC-AUTH-06`: Chuyển hóa DisabledException từ Security thành LockedAccountException.
7. `TC-AUTH-07`: Đăng nhập thất bại khi sai mật khẩu (ném BadCredentialsException).
8. `TC-AUTH-08`: Đăng nhập tài khoản bị khóa nhưng không có lockReason (ném LockedAccountException với reason null).
9. `TC-AUTH-09`: User không tồn tại trong DB trước khi authenticate (ném BadCredentialsException).
10. `TC-AUTH-10`: User không tồn tại trong DB trước auth nhưng ném DisabledException (ném LockedAccountException với reason null).
11. `TC-AUTH-11`: User có lockReason khi ném DisabledException (lấy lý do đưa vào exception).
12. `TC-AUTH-12`: User null trước auth nhưng sau auth tìm thấy trong DB (đăng nhập thành công).
13. `TC-AUTH-13`: User null trước auth và sau auth vẫn không tìm thấy trong DB (ném BusinessException).

### 4.2. Nhóm Quản lý Người dùng (`UserServiceTest` - 23 Test Cases)
Thư mục: `backend/src/test/java/com/example/ecommerce/service/UserServiceTest.java`

14. `TC-USER-01`: Lấy hồ sơ người dùng hiện tại thành công.
15. `TC-USER-02`: Lấy hồ sơ người dùng không tồn tại (ResourceNotFoundException).
16. `TC-USER-03`: Lấy danh sách toàn bộ người dùng phân trang (Pagination).
17. `TC-USER-04`: Khóa tài khoản người dùng thành công (lưu lý do và lockedAt).
18. `TC-USER-05`: Ngăn chặn Admin tự khóa tài khoản của mình.
19. `TC-USER-06`: Chặn khóa tài khoản đã bị khóa trước đó.
20. `TC-USER-07`: Chặn khóa tài khoản khi lý do rỗng.
21. `TC-USER-08`: Chặn khóa tài khoản khi lý do > 500 ký tự.
22. `TC-USER-09`: Khóa tài khoản thất bại do ID không tồn tại.
23. `TC-USER-10`: Mở khóa tài khoản thành công (reset lockReason và lockedAt về null).
24. `TC-USER-11`: Mở khóa tài khoản đang hoạt động bình thường (BusinessException).
25. `TC-USER-12`: Mở khóa tài khoản với ID không tồn tại.
26. `TC-USER-13`: Cập nhật hồ sơ cá nhân đầy đủ (Partial Update).
27. `TC-USER-14`: Đổi mật khẩu thành công (băm mật khẩu mới bằng BCrypt).
28. `TC-USER-15`: Đổi mật khẩu thất bại do xác nhận không khớp.
29. `TC-USER-16`: Đổi mật khẩu thất bại do sai mật khẩu cũ.
30. `TC-USER-17`: Đổi mật khẩu thất bại do trùng mật khẩu cũ.
31. `TC-USER-18`: Khóa tài khoản khi adminUserDetails là null (ghi log UNKNOWN an toàn).
32. `TC-USER-19`: Khóa tài khoản với request null (ném BusinessException).
33. `TC-USER-20`: Khóa tài khoản với reason null (ném BusinessException).
34. `TC-USER-21`: Mở khóa tài khoản khi adminUserDetails là null (ghi log UNKNOWN).
35. `TC-USER-22`: Cập nhật hồ sơ với tất cả các trường null (giữ nguyên toàn bộ giá trị cũ).
36. `TC-USER-23`: Cập nhật hồ sơ với fullName là khoảng trắng (giữ nguyên fullName cũ).

### 4.3. Nhóm Tiện ích JWT (`JwtUtilsTest` - 9 Test Cases)
Thư mục: `backend/src/test/java/com/example/ecommerce/security/JwtUtilsTest.java`

37. `TC-JWT-01`: Tạo chuỗi JWT Token hợp lệ với subject email.
38. `TC-JWT-02`: Xác thực token hợp lệ cho đúng người dùng.
39. `TC-JWT-03`: Từ chối token nếu subject không khớp với UserDetails.
40. `TC-JWT-04`: Từ chối token bị sửa đổi hoặc sai chữ ký (SignatureException).
41. `TC-JWT-05`: Từ chối token đã hết hạn (ExpiredJwtException).
42. `TC-JWT-06`: Từ chối token có định dạng malformed.
43. `TC-JWT-07`: Từ chối token rỗng hoặc null (IllegalArgumentException).
44. `TC-JWT-08`: Kiểm tra token còn hạn bằng isTokenExpired trả về false.
45. `TC-JWT-09`: Kiểm tra token hết hạn bằng isTokenExpired ném ExpiredJwtException.

### 4.4. Nhóm Bộ lọc Xác thực JWT (`JwtAuthenticationFilterTest` - 12 Test Cases)
Thư mục: `backend/src/test/java/com/example/ecommerce/security/JwtAuthenticationFilterTest.java`

46. `TC-FLT-01`: Request không có Authorization header -> bỏ qua xác thực, tiếp tục filterChain.
47. `TC-FLT-02`: Request có Authorization header rỗng -> bỏ qua xác thực.
48. `TC-FLT-03`: Request có Authorization header không bắt đầu bằng Bearer -> bỏ qua.
49. `TC-FLT-04`: Request có Authorization Bearer nhưng chuỗi token rỗng -> bỏ qua.
50. `TC-FLT-05`: Token hợp lệ đầy đủ điều kiện -> set Authentication vào SecurityContextHolder.
51. `TC-FLT-06`: Token hợp lệ nhưng SecurityContext đã có Authentication -> không ghi đè.
52. `TC-FLT-07`: Token có subject rỗng -> không xác thực.
53. `TC-FLT-08`: UserDetails bị disabled (tài khoản bị khóa) -> không set Authentication.
54. `TC-FLT-09`: Email trong token không khớp username trong UserDetails -> không set Authentication.
55. `TC-FLT-10`: Claims có expiration là null hoặc đã quá hạn -> không set Authentication.
56. `TC-FLT-11`: Token hết hạn ném ExpiredJwtException -> bắt ngoại lệ, tiếp tục filterChain an toàn.
57. `TC-FLT-12`: Token lỗi ném MalformedJwtException / RuntimeException -> bắt ngoại lệ an toàn.

### 4.5. Nhóm UserDetailsService (`CustomUserDetailsServiceTest` - 3 Test Cases)
Thư mục: `backend/src/test/java/com/example/ecommerce/security/CustomUserDetailsServiceTest.java`

58. `TC-UDS-01`: Tải UserDetails thành công cho tài khoản đang hoạt động (isEnabled=true).
59. `TC-UDS-02`: Tài khoản bị khóa có trạng thái isEnabled = false trong UserDetails.
60. `TC-UDS-03`: Người dùng không tồn tại ném UsernameNotFoundException bảo mật.

### 4.6. Nhóm Admin Controller (`AdminUserControllerTest` - 10 Test Cases)
Thư mục: `backend/src/test/java/com/example/ecommerce/controller/AdminUserControllerTest.java`

61. `TC-ADM-01`: Admin truy cập GET /api/v1/admin/users thành công trả 200 OK.
62. `TC-ADM-02`: Customer truy cập GET /api/v1/admin/users bị từ chối 403 FORBIDDEN (RBAC).
63. `TC-ADM-03`: Người dùng chưa đăng nhập truy cập GET /api/v1/admin/users bị từ chối 401 UNAUTHORIZED.
64. `TC-ADM-04`: Admin khóa tài khoản thành công PATCH /api/v1/admin/users/{id}/lock trả 200 OK.
65. `TC-ADM-05`: Admin khóa tài khoản với lý do trống bị Bean Validation trả 400 BAD REQUEST.
66. `TC-ADM-06`: Customer gọi PATCH /api/v1/admin/users/{id}/lock bị chặn 403 FORBIDDEN.
67. `TC-ADM-07`: Admin mở khóa tài khoản thành công PATCH /api/v1/admin/users/{id}/unlock trả 200 OK.
68. `TC-ADM-08`: Customer gọi PATCH /api/v1/admin/users/{id}/unlock bị chặn 403 FORBIDDEN.
69. `TC-ADM-09`: Admin gọi getUsers với page âm và size <= 0 -> chuẩn hóa về page=0 và size=10.
70. `TC-ADM-10`: Admin gọi getUsers với size vượt quá 100 -> giới hạn về MAX_PAGE_SIZE(100).

### 4.7. Nhóm Auth Controller (`AuthControllerTest` - 6 Test Cases)
Thư mục: `backend/src/test/java/com/example/ecommerce/controller/AuthControllerTest.java`

71. `TC-ATC-01`: Đăng ký tài khoản với dữ liệu hợp lệ trả 201 CREATED kèm Token.
72. `TC-ATC-02`: Đăng ký với email sai định dạng bị Bean Validation trả 400 BAD REQUEST.
73. `TC-ATC-03`: Đăng ký với mật khẩu ngắn hơn 6 ký tự bị Bean Validation trả 400 BAD REQUEST.
74. `TC-ATC-04`: Đăng nhập thành công trả 200 OK kèm Bearer JWT.
75. `TC-ATC-05`: Đăng nhập với tài khoản bị khóa trả 403 FORBIDDEN kèm lý do khóa.
76. `TC-ATC-06`: Đăng nhập sai email hoặc mật khẩu trả 401 UNAUTHORIZED.

### 4.8. Nhóm User Controller (`UserControllerTest` - 6 Test Cases)
Thư mục: `backend/src/test/java/com/example/ecommerce/controller/UserControllerTest.java`

77. `TC-USR-01`: Lấy thông tin cá nhân hiện tại GET /api/v1/users/me thành công trả 200 OK.
78. `TC-USR-02`: Chưa đăng nhập truy cập GET /api/v1/users/me trả 401 UNAUTHORIZED.
79. `TC-USR-03`: Cập nhật thông tin cá nhân PUT /api/v1/users/me thành công trả 200 OK.
80. `TC-USR-04`: Cập nhật thông tin với số điện thoại sai định dạng trả 400 BAD REQUEST.
81. `TC-USR-05`: Đổi mật khẩu PUT /api/v1/users/me/password thành công trả 200 OK.
82. `TC-USR-06`: Đổi mật khẩu với mật khẩu mới rỗng trả 400 BAD REQUEST.

### 4.9. Nhóm Health Controller (`HealthControllerTest` - 2 Test Cases)
Thư mục: `backend/src/test/java/com/example/ecommerce/controller/HealthControllerTest.java`

83. `TC-HLT-01`: Health Check GET /api/v1/health trả 200 OK với status UP.
84. `TC-HLT-02`: Health Check GET /api/health trả 200 OK với status UP.

### 4.10. Nhóm Xử lý Lỗi Toàn cục (`GlobalExceptionHandlerTest` - 12 Test Cases)
Thư mục: `backend/src/test/java/com/example/ecommerce/exception/GlobalExceptionHandlerTest.java`

85. `TC-ERR-01`: NoResourceFoundException trả về 404 NOT_FOUND.
86. `TC-ERR-02`: ResourceNotFoundException trả về 404 NOT_FOUND.
87. `TC-ERR-03`: BusinessException trả về 400 BAD_REQUEST.
88. `TC-ERR-04`: BadCredentialsException trả về 401 UNAUTHORIZED.
89. `TC-ERR-05`: LockedAccountException trả về 403 FORBIDDEN kèm lý do khóa.
90. `TC-ERR-06`: DisabledException trả về 403 FORBIDDEN.
91. `TC-ERR-07`: AccessDeniedException trả về 403 FORBIDDEN.
92. `TC-ERR-08`: General Exception trả về 500 INTERNAL_SERVER_ERROR.
93. `TC-ERR-09`: LockedAccountException không có lý do -> data trả về null.
94. `TC-ERR-10`: DisabledException với lý do tùy biến -> data trả về reason và lockReason.
95. `TC-ERR-11`: DisabledException với message rỗng -> data trả về null.
96. `TC-ERR-12`: ResourceNotFoundException với resource name và ID.

### 4.11. Nhóm Entity & Application Lifecycle (`UserTest` & `EcommerceApplicationTests` - 3 Test Cases)
97. `TC-ENT-01`: User Entity onCreate thiết lập createdAt và updatedAt.
98. `TC-ENT-02`: User Entity onUpdate cập nhật updatedAt.
99. `TC-APP-01`: Spring Boot Application Context khởi động trọn vẹn với Security, H2, JWT.
100. `TC-APP-02`: Role enum mapping hoạt động chính xác.

---

## 5. BÁO CÁO ĐỘ BAO PHỦ CHI TIẾT (JACOCO REPORT)

| Gói mã nguồn (Package) | Số dòng mã nguồn (Lines) | Số phương thức (Methods) | Instruction Coverage | Branch Coverage | Đánh giá |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **`com.example.ecommerce.service`** | 115 / 115 (100%) | 16 / 16 (100%) | **100%** (531 / 531) | **100%** (46 / 46) | **Tuyệt đối** |
| **`com.example.ecommerce.exception`** | 65 / 65 (100%) | 16 / 16 (100%) | **100%** (221 / 221) | **100%** (10 / 10) | **Tuyệt đối** |
| **`com.example.ecommerce.controller`** | 27 / 27 (100%) | 10 / 10 (100%) | **100%** (140 / 140) | **100%** (2 / 2) | **Tuyệt đối** |
| **`com.example.ecommerce.config`** | 52 / 52 (100%) | 12 / 12 (100%) | **100%** (242 / 242) | n/a | **Tuyệt đối** |
| **`com.example.ecommerce.dto.response`**| 28 / 28 (100%) | 4 / 4 (100%) | **100%** (62 / 62) | n/a | **Tuyệt đối** |
| **`com.example.ecommerce.entity`** | 9 / 9 (100%) | 3 / 3 (100%) | **100%** (28 / 28) | n/a | **Tuyệt đối** |
| **`com.example.ecommerce.security`** | 75 / 77 (97%) | 15 / 15 (100%) | **97%** (301 / 308) | **92%** (26 / 28) | **Vượt trội** |
| **`com.example.ecommerce` (Main)** | 1 / 3 (33%) | 1 / 2 (50%) | **37%** (3 / 8) | n/a | Khởi động Main |
| **TỔNG TOÀN BỘ HỆ THỐNG** | **372 / 376** | **77 / 78** | **99%** (1.528 / 1.540) | **97%** (84 / 86) | **ĐẠT XUẤT SẮC** |

> [!TIP]
> **Nhánh chưa được bao phủ và lý do kỹ thuật (2/86 branches còn lại):**
> - **1 nhánh trong `JwtUtils.validateToken` (Dòng 89):** Biểu thức `claims.getSubject().equals(userDetails.getUsername()) && notExpired;`. Nhánh `subject equals true` nhưng `notExpired == false` do thư viện JJWT tự động ném `ExpiredJwtException` ngay trong `parseClaims` trước khi kiểm tra điều kiện `notExpired`. Đây là cơ chế tự bảo vệ mặc định của JJWT.
> - **1 nhánh trong `JwtAuthenticationFilter.extractBearerToken` (Dòng 98):** Đoạn kiểm tra `StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")`. Nhánh đầu tiên trả false khi header null/rỗng đã được kiểm thử, nhánh thứ hai trả false khi header có text nhưng không bắt đầu bằng Bearer đã được kiểm thử; trường hợp biên phụ thuộc vào cơ chế đo bytecode phân nhánh của compiler.

---

## 6. HƯỚNG DẪN THỰC THI VÀ XEM BÁO CÁO

### 6.1. Lệnh kiểm thử tự động
```powershell
cd backend
.\gradlew clean test jacocoTestReport
```

### 6.2. Đường dẫn báo cáo trực quan
- **Báo cáo kết quả kiểm thử JUnit:**  
  `backend/build/reports/tests/test/index.html` (100 tests, 0 failures, 100% success rate)
- **Báo cáo độ bao phủ JaCoCo:**  
  `backend/build/reports/jacoco/test/html/index.html` (99% instruction coverage, 97% branch coverage)
