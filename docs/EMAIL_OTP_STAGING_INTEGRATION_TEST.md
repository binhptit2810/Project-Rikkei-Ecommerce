# BÁO CÁO NGHIỆM THU KIỂM THỬ TÍCH HỢP STAGING (STAGING INTEGRATION TEST REPORT)
## TÍNH NĂNG: XÁC THỰC EMAIL BẰNG MÃ OTP (EMAIL OTP AUTHENTICATION)
### DỰ ÁN: PROJECT-RIKKEI-ECOMMERCE

---

> **Thời điểm thực hiện kiểm thử:** Ngày 07 tháng 11 năm 2026  
> **Người thực hiện:** Cao Chí Thiện — Senior Backend & Application Security Engineer  
> **Mục tiêu:** Xác minh thực tế trên môi trường staging độc lập với MySQL 8 và Redis 7 thật, chứng minh tính toàn vẹn của Database Migration, tính nguyên tử của Redis Lua Script và độ ổn định dưới tải đồng thời (50 concurrent requests).  
> **Kết luận nghiệm thu:** **ĐỦ ĐIỀU KIỆN TRIỂN KHAI PRODUCTION**

---

## 1. THÔNG TIN MÔI TRƯỜNG KIỂM THỬ (TEST ENVIRONMENT)

Toàn bộ các bài kiểm tra được thực hiện trực tiếp trên dịch vụ hạ tầng thực tế đang chạy:

| Thành phần | Phiên bản / Công nghệ | Endpoint / Kết nối | Mô tả trạng thái |
| :--- | :--- | :--- | :--- |
| **MySQL Service** | MySQL Community Server `8.0.46` (Docker) | `127.0.0.1:3307` | Database độc lập: `staging_test_migration_db`, `staging_test_concurrent_db` |
| **Redis Service** | Redis Server `7.2.7-alpine` (Docker) | `127.0.0.1:6379` | Kiểm tra kết nối TCP raw + RESP: `+PONG`. Sử dụng cho Lua script testing |
| **Mock SMTP / Mail**| Spring Mockito `@MockBean` & Resend SMTP config | In-memory Mock | Giả lập gửi mail thành công và kịch bản lỗi mạng SMTP connection timeout |
| **Backend Runtime** | Java 21 LTS / Spring Boot `3.3.5` / Gradle `8.10.2` | Local JVM | JJWT 0.12.6, Lettuce Redis Client, HikariCP pool |
| **Frontend Runtime**| React 18 / TypeScript / Vite `5.4.21` | Node.js v20+ | Single Page Application, Axios interceptors, Zustand store |

---

## 2. KẾT QUẢ CHI TIẾT TỪNG HẠNG MỤC KIỂM THỬ

### HẠNG MỤC A: MYSQL 8 MIGRATION INTEGRATION TEST

File script kiểm thử: `database/migration_add_email_verification.sql`  
Client thực thi: MySQL CLI `8.0.46` kết nối trực tiếp cổng `3307`.

#### Kịch bản A1: Chạy Migration lần đầu (First Run)
- **Thiết lập:** Khởi tạo database `staging_test_migration_db` với bảng `users` ban đầu chưa có cột `email_verified`. Thêm 2 tài khoản legacy: ID 1 (`legacy_customer@test.com`, CUSTOMER) và ID 2 (`legacy_admin@test.com`, ADMIN).
- **Thực thi:** Chạy toàn bộ script `database/migration_add_email_verification.sql`.
- **Kết quả xác minh:**
  + Bảng `schema_migrations` được tạo tự động; marker `MIG_001_EMAIL_VERIFICATION` được khởi tạo và chốt `target_max_id = 2` trước khi thêm cột.
  + Cột `email_verified` (BOOLEAN NOT NULL DEFAULT FALSE) và `email_verified_at` (DATETIME NULL) được bổ sung thành công vào bảng `users`.
  + Cả hai tài khoản legacy (ID 1, ID 2) được backfill chính xác sang `email_verified = 1`, `email_verified_at` được gán mốc thời gian hiện tại.
  + Trạng thái marker trong `schema_migrations` chuyển thành `COMPLETED`.
- **Trạng thái:** **PASS**

#### Kịch bản A2: Chạy lại Migration lần thứ hai (Idempotency / Rerun Safety)
- **Thiết lập:** Sau lần chạy đầu, hệ thống tiếp nhận đăng ký mới của 2 tài khoản: ID 3 (`customer_pending@test.com`, `email_verified = 0`) và ID 4 (`customer_verified@test.com`, `email_verified = 1`).
- **Thực thi:** Chạy lại script migration lần 2 trên cùng database.
- **Kết quả xác minh:**
  + Script phát hiện marker `MIG_001_EMAIL_VERIFICATION` có trạng thái `COMPLETED`.
  + Script in log `Migration MIG_001_EMAIL_VERIFICATION already completed. Skipping.` và kết thúc ngay lập tức.
  + **Bảo vệ tài khoản mới:** Tài khoản ID 3 (`customer_pending`) vẫn giữ nguyên giá trị `email_verified = 0`. Tuyệt đối không bị ghi đè hoặc bypass xác thực.
  + Không phát sinh lỗi `Duplicate column name`.
- **Trạng thái:** **PASS**

#### Kịch bản A3: Khôi phục trạng thái Migration dở dang (Failure Recovery)
- **Thiết lập:** Mô phỏng tình huống migration bị đứt kết nối giữa chừng sau khi chốt snapshot: `schema_migrations` lưu `status = 'PENDING', target_max_id = 2`. Trong lúc gián đoạn, một tài khoản mới ID 5 (`customer_gap@test.com`) được ứng dụng tạo mới với `email_verified = 0`.
- **Thực thi:** Tiến hành kích hoạt lại migration để khôi phục.
- **Kết quả xác minh:**
  + Script phát hiện trạng thái `PENDING` và tái sử dụng đúng `target_max_id = 2` đã chốt từ trước.
  + Câu lệnh UPDATE có điều kiện `id <= 2` chỉ backfill các tài khoản ID 1 và ID 2.
  + Tài khoản ID 5 có `id = 5 > 2` được bảo vệ an toàn 100%, giữ nguyên trạng thái `email_verified = 0`.
  + Trạng thái chuyển thành công sang `COMPLETED`.
- **Trạng thái:** **PASS**

#### Kịch bản A4: Kiểm tra an toàn khi nhiều tiến trình chạy song song (Concurrency)
- **Thiết lập:** Tạo database `staging_test_concurrent_db`. Dùng PowerShell `Start-Job` khởi chạy đồng thời 2 tiến trình chạy cùng file migration script vào cùng database.
- **Kết quả xác minh:**
  + Tiến trình 1 chiếm Metadata Lock, hoàn thành thành công toàn bộ migration và cập nhật `COMPLETED`.
  + Tiến trình 2 xử lý an toàn theo cơ chế lock của MySQL InnoDB DDL và kiểm tra `schema_migrations`, không gây deadlock, crash server hoặc làm hỏng dữ liệu bảng `users`.
- **Trạng thái:** **PASS**

---

### HẠNG MỤC B: REDIS LUA ATOMIC LOCK INTEGRATION TEST

Kiểm thử trên Redis Server 7 thật tại `127.0.0.1:6379`.  
Class kiểm thử: `com.example.ecommerce.service.EmailOtpStagingIntegrationTest`.

| Mã Test | Tên Kịch Bản Kiểm Thử | Hành Động Thực Tế | Kết Quả Mong Đợi | Kết Quả Thực Tế | Trạng Thái |
| :--- | :--- | :--- | :--- | :--- | :---: |
| **LUA-01** | Release đúng chủ sở hữu (Owner Match) | Request A acquire lock sinh `lockValueA`. Gọi `releaseRateLimitLock(key, lockValueA)` | Lua script Compare-and-Delete trả về 1, key bị xóa khỏi Redis | Trả về `true` (1), key biến mất khỏi Redis | **PASS** |
| **LUA-02** | Release sai chủ sở hữu (Wrong Owner) | Request A có `lockValueA`. Request B gọi release với `lockValueB` | Lua script trả về 0, key không bị xóa và giữ nguyên `lockValueA` | Trả về `false` (0), key vẫn lưu `lockValueA` | **PASS** |
| **LUA-03** | Khóa đã hết hạn (Expired Lock) | Gọi release khi key không tồn tại trong Redis | Lua script trả về 0, không throw exception | Trả về `false` (0) | **PASS** |
| **LUA-04** | Chống xóa nhầm khóa của Request sau (Stale Request Protection) | Request A bị timeout (60s), Request B chiếm lock mới với `lockValueB`. Request A gửi release với `lockValueA` cũ | Lua script trả về 0, bảo vệ toàn vẹn `lockValueB` của Request B | Trả về `false` (0), key vẫn còn và giữ `lockValueB` | **PASS** |
| **LUA-05** | Chu trình Verify & Claim OTP trên Redis thật | Thực thi `VERIFY_AND_CLAIM_LUA_SCRIPT` với OTP đúng, OTP sai, double-claim và brute-force | Đúng: `SUCCESS:...` & set `CLAIMED`. Double: `ALREADY_USED`. Sai: `MISMATCH:n`. Quá 5 lần: `MAX_ATTEMPTS_EXCEEDED` & xóa key | Trả về chính xác 100% các mã phản hồi và xóa key khi brute-force | **PASS** |

---

### HẠNG MỤC C: CONCURRENT TEST (50 REQUESTS ĐỒNG THỜI)

Kiểm thử đa luồng bằng `ExecutorService` (50 threads) và điều phối kích hoạt đồng loạt bằng `CountDownLatch(1)`.

#### 1. Kiểm thử 50 concurrent requests cùng đăng ký 1 email (`register`)
- **Tình huống:** 50 threads đồng loạt gọi `authService.register()` với cùng email `staging_test_register_concurrent@example.com`.
- **Dữ liệu thực tế:**
  + Số lượng request thành công: **1** (Trả về `RegisterResponse`, `emailVerified = false`).
  + Số lượng request bị chặn: **49** (Bị chặn bởi nghiệp vụ `BusinessException: Email này đã được đăng ký nhưng chưa hoàn tất xác thực` hoặc ràng buộc duy nhất CSDL `DataIntegrityViolationException`).
  + Dữ liệu MySQL: Kiểm tra `userRepository.findAll()` -> Có **đúng 1 bản ghi** duy nhất với role `CUSTOMER`, `active = true`, `email_verified = false`. Không xảy ra duplicate user.
  + Dữ liệu Redis: Kiểm tra `auth:verify_otp:...` -> Có **đúng 1 mã OTP** hợp lệ được lưu trữ.
- **Trạng thái:** **PASS**

#### 2. Kiểm thử 50 concurrent requests cùng yêu cầu gửi lại OTP (`resendVerification`)
- **Tình huống:** Chuẩn bị tài khoản `staging_test_resend_concurrent@example.com` chưa xác thực. 50 threads đồng loạt gọi `authService.resendVerification()`.
- **Dữ liệu thực tế:**
  + Số lượng request thành công: **1** (Chiếm được khóa nguyên tử `setIfAbsent` SET NX EX 60s).
  + Số lượng request bị chặn rate limit: **49** (Nhận ngoại lệ `BusinessException: Vui lòng đợi 60 giây trước khi yêu cầu mã mới`).
  + Dữ liệu Mock SMTP: Phương thức `emailService.sendVerificationOtpEmail()` chỉ được gọi **đúng 1 lần duy nhất**. Chặn hoàn toàn nguy cơ spam mail và lãng phí hạn ngạch SMTP.
- **Trạng thái:** **PASS**

#### 3. Kiểm thử 50 concurrent requests đăng ký 50 email khác nhau
- **Tình huống:** 50 threads đồng loạt đăng ký 50 địa chỉ email độc lập từ `staging_test_user_0@...` đến `staging_test_user_49@...`.
- **Dữ liệu thực tế:**
  + Số lượng request thành công: **50 / 50 (100%)**.
  + Số lượng thất bại / nghẽn: **0**.
  + Các khóa rate limit và mã OTP của từng email hoạt động hoàn toàn độc lập, không gây nghẽn chéo hoặc chặn nhầm người dùng khác.
  + CSDL ghi nhận đủ 50 tài khoản người dùng mới.
- **Trạng thái:** **PASS**

#### 4. Thử nghiệm phục hồi khi gặp lỗi gửi mail (SMTP Failure Resilience under Load)
- **Tình huống:** Mô phỏng sự cố mạng dịch vụ email (SMTP timeout / connection refused).
- **Dữ liệu thực tế:**
  + Phương thức `register()` bắt ngoại lệ SMTP, xóa OTP rác và gọi `releaseRateLimitLock` qua Lua script Compare-and-Delete để giải phóng khóa 60s.
  + Ngay sau khi giải phóng, kiểm tra `redisTemplate.hasKey(verifyRateLimitKey)` -> Trả về `false`.
  + Người dùng lập tức gọi `resendVerification()` -> Thành công ngay lập tức mà **không phải chờ 60 giây**.
  + Cơ chế Compare-and-Delete đảm bảo việc giải phóng lock bị lỗi không bao giờ xóa nhầm lock hợp lệ của request tiếp theo.
- **Trạng thái:** **PASS**

---

## 3. LOG MINH CHỨNG THỰC TẾ (RÚT GỌN & CHE THÔNG TIN NHẠY CẢM)

```text
========================================================================================
[STAGING INTEGRATION SUITE] Executing on real MySQL 8 (port 3307) and Redis 7 (port 6379)
========================================================================================
> Task :compileJava UP-TO-DATE
> Task :classes UP-TO-DATE
> Task :compileTestJava UP-TO-DATE
> Task :testClasses UP-TO-DATE

Running com.example.ecommerce.service.EmailOtpStagingIntegrationTest:
  [PASS] testRedisLuaLock_RequestA_AcquiresAndReleasesSuccessfully
  [PASS] testRedisLuaLock_RequestB_WrongLockValueFails
  [PASS] testRedisLuaLock_ExpiredKeyReturnsFalse
  [PASS] testRedisLuaLock_StaleRequestCannotDeleteNewLock
  [PASS] testRedisLuaVerifyAndClaim_CompleteLifecycleOnRealRedis
  [PASS] testConcurrentRegister_SameEmail_50Requests (1 success, 49 rejected, 1 user in DB)
  [PASS] testConcurrentResendVerification_SameEmail_50Requests (1 success, 49 rate-limited, 1 SMTP call)
  [PASS] testConcurrentRegister_DistinctEmails_50Requests (50/50 success, independent rate limits)
  [PASS] testRegister_SmtpFailure_LockCleanlyReleasedViaLua (lock released immediately on error)

========================================================================================
[AUTOMATED TEST SUMMARY]
Total Tests: 443 | Failures: 0 | Ignored: 0 | Success Rate: 100% | Duration: 16.913s
========================================================================================

[FRONTEND BUILD VERIFICATION]
> vite build
✓ 2063 modules transformed.
dist/index.html                   1.24 kB │ gzip:  0.64 kB
dist/assets/index-Cw5foCYO.css   79.90 kB │ gzip: 12.92 kB
dist/assets/router-DIrMWSM9.js  164.46 kB │ gzip: 53.65 kB
dist/assets/index-6k445Upt.js   436.48 kB │ gzip: 86.37 kB
✓ built in 14.79s (Exit Code: 0)
```

---

## 4. ĐÁNH GIÁ RỦI RO & ĐIỀU KIỆN TRIỂN KHAI PRODUCTION

### Phân tích rủi ro còn lại

1. **Khóa bảng khi chạy DDL Migration trên CSDL quy mô cực lớn:**
   - Trong MySQL 8, thao tác `ALTER TABLE users ADD COLUMN ...` có hỗ trợ `ALGORITHM=INSTANT` đối với việc thêm cột ở cuối bảng. Thời gian thực thi là tức thì (miliseconds) và không khóa bảng đối với bảng dưới hàng triệu dòng.
   - *Khuyến nghị:* Chạy migration vào khung giờ bảo trì thấp điểm (maintenance window) để đảm bảo an toàn tuyệt đối.

2. **Cấu hình biến môi trường Production:**
   - Biến bí mật `APP_OTP_HMAC_SECRET`: Tuyệt đối không để giá trị mặc định của dev. Cần thiết lập chuỗi ngẫu nhiên CSPRNG tối thiểu 32 ký tự (256-bit) trên VPS/K8s/Docker Compose.
   - Biến cấu hình Resend API Key: Đảm bảo hạn ngạch gửi mail của tài khoản Resend đủ đáp ứng lưu lượng đăng ký thực tế.

### Checklist điều kiện trước khi Deploy Production

- [x] Script migration `database/migration_add_email_verification.sql` đã được xác minh tính idempotent và cơ chế snapshot an toàn trên MySQL 8.
- [x] Redis Lua Script Compare-and-Delete đã được kiểm thử trực tiếp trên Redis 7.
- [x] Backend test suite đạt 100% tỷ lệ vượt qua (443/443 tests).
- [x] Frontend Single Page Application build thành công không lỗi cú pháp hoặc đóng gói.
- [x] Kiểm tra bảo vệ chống User Enumeration và Brute-force 5 lần thử đã hoạt động chính xác.

---

## 5. KẾT LUẬN NGHIỆM THU

Tính năng **Email OTP Authentication** của dự án `Project-Rikkei-Ecommerce` đã hoàn thành kiểm thử tích hợp thực tế trên môi trường Staging với đầy đủ các tiêu chí an toàn cao nhất:

1. **Database Migration:** Không phụ thuộc tên database, bảo toàn tuyệt đối trạng thái của tài khoản cũ và tài khoản mới, an toàn khi chạy lại hoặc khôi phục từ trạng thái dở dang.
2. **Redis Concurrency:** Khóa rate limit và xác thực OTP được bảo vệ bằng Lua scripts nguyên tử, loại trừ hoàn toàn race condition, double-claim và xóa nhầm lock.
3. **Hiệu năng & Khả năng chịu tải:** Chặn đứng 100% nỗ lực spam đồng thời 50 requests, bảo vệ hệ thống CSDL và hạn ngạch SMTP.

> ### KẾT LUẬN CHÍNH THỨC: **ĐỦ ĐIỀU KIỆN TRIỂN KHAI PRODUCTION**

