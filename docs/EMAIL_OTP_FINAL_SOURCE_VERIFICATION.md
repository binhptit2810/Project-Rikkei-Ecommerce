# BÁO CÁO ĐỐI CHIẾU NGUỒN VÀ ĐÁNH GIÁ NGUYÊN TRẠNG (FINAL SOURCE VERIFICATION)
## TÍNH NĂNG: XÁC THỰC EMAIL BẰNG MÃ OTP (EMAIL OTP)
### DỰ ÁN: PROJECT-RIKKEI-ECOMMERCE

---

> **Thời điểm thẩm định:** 07/11/2026  
> **Phương pháp kiểm tra:** Đọc và đối chiếu trực tiếp mã nguồn thực tế (Direct Source Code Inspection), không dựa vào giả định hoặc báo cáo trước đó.  
> **Nguyên tắc tuân thủ:** Không sửa code, không chạy Docker/VPS, không thay đổi Git, bảo mật tuyệt đối các giá trị secret.

---

## 1. KIỂM TRA TOÀN DIỆN DATABASE MIGRATION

File kiểm tra: `database/migration_add_email_verification.sql`

### 1.1. Thời điểm chốt `target_max_id` so với `ALTER TABLE`
- **Vị trí code:**
  - Dòng 36–45:
    ```sql
    SET @current_max_user_id = (SELECT IFNULL(MAX(id), 0) FROM users);
    SET @init_marker = (SELECT IF(
        @is_completed = 0 AND @has_pending_record = 0,
        "INSERT INTO schema_migrations (version, description, status, target_max_id, executed_at) VALUES ('20261107_email_otp', 'Add email verification columns and backfill legacy accounts', 'PENDING', @current_max_user_id, CURRENT_TIMESTAMP)",
        "SELECT 1"
    ));
    PREPARE stmtInitMarker FROM @init_marker;
    EXECUTE stmtInitMarker;
    DEALLOCATE PREPARE stmtInitMarker;
    ```
  - Dòng 53–74: Kiểm tra và thực thi `ALTER TABLE users ADD COLUMN email_verified ...`.
- **Kết quả:** **PASS**
- **Mức độ ảnh hưởng:** Tích cực (Bảo vệ dữ liệu).
- **Phân tích:** `target_max_id` được ghi nhận vào `schema_migrations` ở trạng thái `PENDING` **hoàn toàn trước khi** câu lệnh `ALTER TABLE` được thực thi. Bất kỳ tài khoản người dùng nào đăng ký trong hoặc sau khi bảng `users` được thêm cột mới sẽ có `id > target_max_id`, loại trừ triệt để race condition bị backfill nhầm.

---

### 1.2. Tên marker migration hiện tại
- **Vị trí code:**
  - Dòng 20: `SET @migration_version = '20261107_email_otp';`
  - Dòng 40: `VALUES ('20261107_email_otp', ...)`
  - Dòng 90: `WHERE version = '20261107_email_otp'`
- **Kết quả:** **PASS** (Trong SQL), nhưng **MISMATCH** so với văn bản báo cáo cũ.
- **Mức độ ảnh hưởng:** Trung bình (Tài liệu / Vận hành).
- **Phân tích:** Tên marker chính thức trong file SQL là `'20261107_email_otp'`. Một số báo cáo trước ghi nhận tên `MIG_001_EMAIL_VERIFICATION`. Khi truy vấn đối soát trong CSDL thực tế, DevOps phải tìm kiếm `version = '20261107_email_otp'`.
- **Đề xuất xử lý:** Chuẩn hóa thống nhất tên marker `20261107_email_otp` trong tài liệu vận hành và checklist triển khai.

---

### 1.3. Cơ chế xử lý marker COMPLETED và PENDING
- **Vị trí code:**
  - Dòng 21–24: `SET @is_completed = (SELECT COUNT(*) FROM schema_migrations WHERE version = @migration_version AND status = 'COMPLETED');`
  - Dòng 29–32: `SET @has_pending_record = (SELECT COUNT(*) FROM schema_migrations WHERE version = @migration_version AND status = 'PENDING');`
  - Dòng 48–51: Đọc lại `@frozen_max_id` từ `schema_migrations`.
- **Kết quả:** **PASS**
- **Mức độ ảnh hưởng:** Tích cực (Khả năng chịu lỗi và tự phục hồi).
- **Phân tích:**
  - Khi đã `COMPLETED`: `@is_completed = 1`, tất cả các dynamic SQL đều gán thành `"SELECT 1"`. Không thực hiện bất kỳ thay đổi nào.
  - Khi đang `PENDING` (chạy lại sau khi bị gián đoạn): Không ghi đè snapshot mới mà tái sử dụng chính xác `@frozen_max_id` đã lưu từ lần chạy trước, bảo đảm tài khoản tạo trong lúc gián đoạn không bị backfill sai.

---

### 1.4. Xử lý hai tiến trình chạy Migration đồng thời (Concurrency & Exit Code)
- **Vị trí code:**
  - Dòng 10–17: `CREATE TABLE IF NOT EXISTS schema_migrations (version VARCHAR(50) NOT NULL PRIMARY KEY, ...)`
  - Dòng 40: `INSERT INTO schema_migrations (version, description, status, target_max_id, executed_at) VALUES ('20261107_email_otp', ...)`
- **Kết quả:** **FAIL** (Về mặt Exit Code của tiến trình thứ hai).
- **Mức độ ảnh hưởng:** Trung bình (Operational alert).
- **Phân tích:**
  - Lệnh INSERT ở dòng 40 **không sử dụng `INSERT IGNORE`** và không có Distributed Lock / Advisory Lock (`GET_LOCK`).
  - Nếu hai tiến trình chạy file migration cùng một thời điểm từ đầu:
    + Tiến trình 1 chèn bản ghi thành công.
    + Tiến trình 2 kiểm tra đồng thời thấy chưa có bản ghi, sẽ cố chèn và gặp lỗi:
      `ERROR 1062 (23000): Duplicate entry '20261107_email_otp' for key 'schema_migrations.PRIMARY'`.
    + Tiến trình 2 sẽ **kết thúc với Exit Code khác 0 (thất bại)**.
  - Về mặt an toàn dữ liệu: Bảng `users` không bị hỏng nhờ cơ chế InnoDB Metadata Lock (MDL) và Primary Key của `schema_migrations`. Tuy nhiên, kết luận trong báo cáo cũ rằng *"Cả hai tiến trình đều hoàn thành êm thuận với code 0"* là không chính xác trên thực tế nếu chạy cùng thời điểm khởi đầu.
- **Đề xuất xử lý:**
  Sửa câu lệnh INSERT thành `INSERT IGNORE INTO schema_migrations ...` hoặc bổ sung MySQL Advisory Lock ở đầu script:
  ```sql
  SELECT GET_LOCK('migration_20261107_email_otp', 30);
  -- thực thi migration
  SELECT RELEASE_LOCK('migration_20261107_email_otp');
  ```

---

### 1.5. Đánh giá tính Idempotent của Migration
- **Kết quả:**
  - **Chạy tuần tự (Sequential):** **PASS (100% Idempotent)**. Chạy lại N lần đều an toàn, không lỗi, không ghi đè dữ liệu tài khoản mới.
  - **Chạy đồng thời (Concurrent):** **PARTIALLY PASS**. Dữ liệu được bảo toàn an toàn nhưng tiến trình phụ có thể bị exit code 1 do lỗi trùng Primary Key.

---

## 2. KIỂM TRA CƠ CHẾ REDIS RATE LIMIT VÀ ATOMIC LOCK

File kiểm tra: `backend/src/main/java/com/example/ecommerce/service/AuthService.java`

```
+----------------------------------------------------------------------------------------------------+
|                                    PHÂN ĐỊNH 4 CƠ CHẾ KIỂM SOÁT                                     |
+------------------------------------+---------------------------------------------------------------+
| Cơ chế kiểm soát                   | Trạng thái hiện tại trong mã nguồn                            |
+------------------------------------+---------------------------------------------------------------+
| 1. Email-based Rate Limit          | ĐÃ CÓ: auth:verify_ratelimit:<email> (60s cooldown)           |
| 2. IP-based Rate Limit             | CHƯA CÓ (NOT IMPLEMENTED): Backend không đọc IP client       |
| 3. OTP Attempt Limit (Brute-force) | ĐÃ CÓ: auth:verify_attempts:<email> (Tối đa 5 lần via Lua)   |
| 4. Concurrency Mutex / Lock        | ĐÃ CÓ: setIfAbsent (SET NX EX) + Lua Compare-and-Delete       |
+------------------------------------+---------------------------------------------------------------+
```

### 2.1. Resend Cooldown 60s: Email-based hay IP-based?
- **Vị trí code:** `AuthService.java`, Dòng 414–417:
  ```java
  String rateLimitKey = VERIFY_RATELIMIT_PREFIX + normalizedEmail;
  String lockValue = UUID.randomUUID().toString();
  Boolean acquired = redisTemplate.opsForValue().setIfAbsent(rateLimitKey, lockValue, otpRateLimitSeconds, TimeUnit.SECONDS);
  ```
- **Kết quả:** **PASS** (cho Email-based Rate Limit).
- **Mức độ ảnh hưởng:** Chặn spam theo từng tài khoản, nhưng không ngăn được một IP spam nhiều email khác nhau.
- **Phân tích:** Khóa rate limit hoàn toàn phụ thuộc vào chuỗi `normalizedEmail`. Hệ thống không lưu trữ hoặc kiểm tra địa chỉ IP trong phương thức này.

---

### 2.2. Cơ chế Rate Limit giữa Register và Resend
- **Vị trí code:** `AuthService.java`:
  - Dòng 260–266 (`register`):
    ```java
    String verifyRateLimitKey = VERIFY_RATELIMIT_PREFIX + normalizedEmail;
    String lockValue = UUID.randomUUID().toString();
    redisTemplate.opsForValue().set(verifyRateLimitKey, lockValue, otpRateLimitSeconds, TimeUnit.SECONDS);
    ```
  - Dòng 414–421 (`resendVerification`):
    ```java
    Boolean acquired = redisTemplate.opsForValue().setIfAbsent(rateLimitKey, lockValue, otpRateLimitSeconds, TimeUnit.SECONDS);
    if (Boolean.FALSE.equals(acquired)) {
        throw new BusinessException("Vui lòng đợi 60 giây trước khi yêu cầu mã mới");
    }
    ```
- **Kết quả:** **PASS** (Chia sẻ chung không gian khóa cooldown).
- **Mức độ ảnh hưởng:** Cao.
- **Phân tích:**
  - `register` và `resendVerification` **dùng chung tiền tố và tên key** (`auth:verify_ratelimit:<normalizedEmail>`).
  - Sau khi người dùng đăng ký thành công, `register` ghi đè thời hạn 60s vào key này. Nếu người dùng lập tức bấm "Gửi lại mã", `resendVerification` sẽ phát hiện key đang tồn tại và chặn lại ngay lập tức (bảo vệ giới hạn 60s).
  - Điểm cần lưu ý: `register()` **không kiểm tra rate limit trước khi tạo tài khoản**. Khả năng chống duplicate của `register()` dựa vào `findByEmail` và ràng buộc `UNIQUE KEY` trong CSDL.

---

### 2.3. Tạo và giải phóng Redis Lock (Compare-and-Delete)
- **Vị trí code:** `AuthService.java`:
  - Định nghĩa Lua script (Dòng 179–184):
    ```lua
    if redis.call('GET', KEYS[1]) == ARGV[1] then
        return redis.call('DEL', KEYS[1])
    else
        return 0
    end
    ```
  - Phương thức giải phóng (Dòng 193–208): `releaseRateLimitLock(String rateLimitKey, String lockValue)`.
  - Vị trí sử dụng:
    + Dòng 278: Trong `catch (Exception ex)` của `register()`.
    + Dòng 457: Trong `catch (Exception ex)` của `resendVerification()`.
- **Kết quả:** **PASS**
- **Mức độ ảnh hưởng:** Rất cao (Bảo vệ tính sẵn sàng và chống race condition).
- **Phân tích:** Khi gặp lỗi SMTP, lock chỉ bị xóa nếu giá trị hiện tại trong Redis khớp chính xác với `lockValue` (UUID) do request đó tạo ra. Nếu request bị stale và đã có request mới chiếm lock, lock mới sẽ không bị xóa nhầm.

---

### 2.4. Tính nhất quán của Compare-and-Delete trên toàn bộ AuthService
- **Vị trí code:** `AuthService.java`, Dòng 649–654 (`forgotPassword`):
  ```java
  String rateLimitKey = OTP_RATELIMIT_PREFIX + normalizedEmail;
  Boolean isRateLimited = redisTemplate.hasKey(rateLimitKey);
  if (Boolean.TRUE.equals(isRateLimited)) { ... }
  ```
- **Kết quả:** **FAIL** (Về mặt nhất quán kiến trúc).
- **Mức độ ảnh hưởng:** Thấp đối với Email OTP đăng ký, nhưng là nợ kỹ thuật (Technical Debt) đối với module Forgot Password cũ.
- **Phân tích:** Luồng Email OTP mới đã chuẩn hóa sang atomic `setIfAbsent` và Lua Compare-and-Delete. Tuy nhiên, luồng `forgotPassword` cũ vẫn đang dùng mô hình check-then-act (`hasKey()` rồi `set()`).
- **Đề xuất xử lý:** Đồng bộ hóa `forgotPassword()` sang mô hình `setIfAbsent` và `releaseRateLimitLock` tương tự `resendVerification`.

---

### 2.5. Giới hạn số lần nhập OTP sai (Brute-force Protection)
- **Vị trí code:** `AuthService.java`, Dòng 136–168 (`VERIFY_AND_CLAIM_LUA_SCRIPT`) & Dòng 346–353:
  ```lua
  local attempts = tonumber(redis.call('GET', KEYS[2]) or '0')
  if attempts >= tonumber(ARGV[2]) then
      redis.call('DEL', KEYS[1])
      return 'MAX_ATTEMPTS_EXCEEDED'
  end
  ```
- **Kết quả:** **PASS**
- **Mức độ ảnh hưởng:** Rất cao (Bảo mật OTP).
- **Phân tích:** Mỗi lần xác thực sai, Lua script tăng biến đếm `auth:verify_attempts:<email>` và gán TTL bằng TTL còn lại của OTP. Khi đạt ngưỡng 5 lần, key OTP bị xóa ngay lập tức trên Redis và người dùng bị hủy mã OTP.

---

### 2.6. Giới hạn request theo IP trong Backend
- **Vị trí code:** Toàn bộ thư mục `backend/src/main/java`.
- **Kết quả:** **NOT IMPLEMENTED**
- **Mức độ ảnh hưởng:** Cao.
- **Phân tích:** Không có bất kỳ Servlet Filter, HandlerInterceptor hay Bucket4j cấu hình nào trong Spring Boot thực hiện thu thập IP hoặc rate limit theo IP. Nếu kẻ tấn công sử dụng danh sách 10.000 email khác nhau từ một địa chỉ IP duy nhất, Backend sẽ tiếp nhận toàn bộ các request này mà không chặn IP.

---

## 3. ĐỐI CHIẾU BIẾN MÔI TRƯỜNG VÀ CẤU HÌNH SECRET

### 3.1. Đối chiếu tên biến thực tế
- **Trong file `backend/src/main/resources/application.yml` (Dòng 99–100):**
  ```yaml
  app:
    otp:
      hmac-secret: ${OTP_HMAC_SECRET:ecommerce-default-otp-hmac-secret-change-in-production-2024}
  ```
- **Trong file `backend/src/main/resources/application-docker.yml` (Dòng 29–30):**
  ```yaml
  app:
    otp:
      hmac-secret: ${OTP_HMAC_SECRET:}
  ```
- **Trong file `docker-compose.yml` (Dòng 92):**
  ```yaml
  backend:
    environment:
      OTP_HMAC_SECRET: ${OTP_HMAC_SECRET:-}
  ```
- **Trong file `AuthService.java` (Dòng 76–104):**
  ```java
  @Value("${app.otp.hmac-secret:ecommerce-default-otp-hmac-secret-change-in-production-2024}")
  private String otpHmacSecret;
  ...
  throw new IllegalStateException("CRITICAL SECURITY ERROR: Biến môi trường OTP_HMAC_SECRET bắt buộc phải được cấu hình và có độ dài tối thiểu 32 ký tự trên môi trường Production.");
  ```

### 3.2. So sánh `OTP_HMAC_SECRET` vs `APP_OTP_HMAC_SECRET`
- **Kết quả:** **DISCREPANCY DETECTED (PHÁT HIỆN LỆCH TÊN BIẾN)**
- **Mức độ ảnh hưởng:** **NGHIÊM TRỌNG (CRITICAL)** nếu triển khai sai biến trên VPS.
- **Phân tích:**
  - Tên biến thực tế mà toàn bộ mã nguồn và cấu hình Docker đang sử dụng là **`OTP_HMAC_SECRET`**.
  - Báo cáo kiểm thử Staging trước đó có một đoạn khuyến nghị dùng `APP_OTP_HMAC_SECRET`.
  - **Hậu quả nếu cấu hình theo báo cáo cũ:**
    Nếu DevOps thiết lập `APP_OTP_HMAC_SECRET=...` trong file `.env` trên VPS mà không khai báo `OTP_HMAC_SECRET`, thì `application-docker.yml` sẽ đọc `${OTP_HMAC_SECRET:}` ra giá trị rỗng `""`. Khi ứng dụng khởi động, phương thức `@PostConstruct validateOtpHmacSecret()` trong `AuthService` sẽ ném ngoại lệ:
    `CRITICAL SECURITY ERROR: Biến môi trường OTP_HMAC_SECRET bắt buộc phải được cấu hình...`
    -> **Ứng dụng Spring Boot Backend sẽ bị sập (CrashOnStartup) ngay lập tức!**
- **Đề xuất xử lý:**
  1. Chỉ sử dụng duy nhất tên biến: **`OTP_HMAC_SECRET`** trong toàn bộ tài liệu triển khai và file `.env`.
  2. Bổ sung fallback trong `application-docker.yml` để hỗ trợ cả hai biến nếu cần tương thích:
     `hmac-secret: ${OTP_HMAC_SECRET:${APP_OTP_HMAC_SECRET:}}`

---

## 4. KIỂM TRA NGINX VÀ NGUY CƠ NHẬN DIỆN CHUNG IP

File kiểm tra: `frontend/nginx.conf`

### 4.1. Khảo sát chỉ thị `limit_req_zone` và `limit_req`
- **Vị trí code:** Toàn bộ `frontend/nginx.conf` (52 dòng).
- **Kết quả:** **NOT IMPLEMENTED**
- **Mức độ ảnh hưởng:** Cao (Chưa có phòng thủ DoS/Brute-force ở tầng mạng).
- **Phân tích:** Nginx hiện tại chỉ đóng vai trò web server tĩnh (React) và reverse proxy thuần túy (`proxy_pass http://backend:8080;`). Hoàn toàn chưa cấu hình module `ngx_http_limit_req_module`.

---

### 4.2. Cách Nginx thu thập IP client
- **Vị trí code:** `frontend/nginx.conf`, Dòng 29–31:
  ```nginx
  proxy_set_header X-Real-IP $remote_addr;
  proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
  proxy_set_header X-Forwarded-Proto $scheme;
  ```
- **Phân tích:** Nginx lấy trực tiếp `$remote_addr` (IP thiết lập kết nối TCP tới Nginx) để gán vào `X-Real-IP`.

---

### 4.3. Nguy cơ tất cả người dùng bị gộp chung một IP (Denial of Service diện rộng)
- **Kết quả:** **CRITICAL RISK (RỦI RO CỰC KỲ CAO KHI BẬT RATE LIMIT THEO `$binary_remote_addr`)**
- **Cơ chế phát sinh lỗi:**
  1. Trong môi trường Production thực tế, hệ thống thường được đặt sau một Lớp Proxy Ngoài (Reverse Proxy trên VPS, AWS ALB, Cloudflare CDN hoặc Nginx Ingress).
  2. File `frontend/nginx.conf` hiện tại **hoàn toàn thiếu cấu hình `ngx_http_realip_module`**:
     - Chưa có: `set_real_ip_from <IP_PROXY_HOẶC_CLOUDFLARE>;`
     - Chưa có: `real_ip_header X-Forwarded-For;` (hoặc `CF-Connecting-IP;`)
     - Chưa có: `real_ip_recursive on;`
  3. Nếu ai đó cấu hình `limit_req_zone $binary_remote_addr zone=auth_limit:10m rate=5r/m;` vào `frontend/nginx.conf` mà không có các cấu hình `real_ip` trên:
     - Giá trị `$remote_addr` của **TẤT CẢ mọi người dùng truy cập từ Internet** sẽ bị gộp thành IP của Proxy/Cloudflare Edge.
     - Ngay khi 1 người dùng gửi request thứ 5, toàn bộ người dùng thật khác trên toàn thế giới sẽ bị Nginx trả về lỗi **HTTP 429 Too Many Requests**.
     - Hệ thống tự gây ra sự cố Từ chối Dịch vụ (Self-inflicted DoS).

---

### 4.4. Đề xuất cấu hình an toàn cho Nginx trước khi kích hoạt IP Rate Limit

Cần bổ sung vào `frontend/nginx.conf` khối nhận diện IP thực tế trước khi đặt zone:

```nginx
# 1. Cấu hình giải mã IP thật từ CDN / Reverse Proxy
set_real_ip_from 172.16.0.0/12;    # Dải IP Docker Network nội bộ
set_real_ip_from 10.0.0.0/8;        # Dải IP Private Network (VPS/LB)
# Nếu sử dụng Cloudflare, cần include danh sách IP Cloudflare:
# set_real_ip_from 103.21.244.0/22; ...
real_ip_header X-Forwarded-For;
real_ip_recursive on;

# 2. Định nghĩa Zone Rate Limit theo IP thực tế đã giải mã
limit_req_zone $binary_remote_addr zone=auth_ip_limit:10m rate=10r/m;

# 3. Áp dụng giới hạn riêng cho các API xác thực nhạy cảm
location ~ ^/api/v1/auth/(register|resend-verification|forgot-password) {
    limit_req zone=auth_ip_limit burst=5 nodelay;
    limit_req_status 429;

    proxy_pass $backend_upstream;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

---

## 5. TỔNG KẾT VÀ KẾT LUẬN TỪNG HẠNG MỤC

### 1. Email OTP Backend: **PASS**
- Xác thực HMAC-SHA256, chu trình sinh và hủy OTP an toàn.
- Kiểm soát brute-force 5 lần thử via Lua script hoạt động chính xác.
- Mã nguồn sạch, không hardcode secret, kiểm soát điều kiện biên chặt chẽ.

### 2. Database Migration: **PASS CÓ ĐIỀU KIỆN (CONDITIONAL PASS)**
- Chốt snapshot `target_max_id` trước khi ALTER TABLE: Rất an toàn và đúng đắn.
- Chạy tuần tự và khôi phục sự cố: 100% an toàn.
- *Điều kiện:* Không chạy song song nhiều tiến trình migration script cùng lúc từ đầu vì có thể gặp lỗi `ERROR 1062` do thiếu `INSERT IGNORE` trên `schema_migrations`.

### 3. Redis Atomic Lock: **PASS**
- Sử dụng đúng `setIfAbsent` (SET NX EX) và giải phóng khóa qua Lua Compare-and-Delete tại các catch block.
- Chống stale request xóa nhầm khóa của request mới.

### 4. Email-based Rate Limit: **PASS**
- Rate limit 60s theo từng email hoạt động chuẩn xác, bảo vệ cả luồng đăng ký và gửi lại mã xác thực.

### 5. IP-based Rate Limit tại Nginx: **NOT IMPLEMENTED / HIGH RISK NẾU CHƯA CẤU HÌNH REAL_IP**
- Hiện tại Nginx chưa có rate limit theo IP.
- Nghiêm cấm bật `limit_req_zone $binary_remote_addr` khi chưa cấu hình `ngx_http_realip_module` vì sẽ gây lỗi DoS chặn toàn bộ người dùng dùng chung IP của Proxy/Cloudflare.

### 6. Production Deployment Readiness: **READY WITH MANDATORY CONDITIONS (ĐỦ ĐIỀU KIỆN KÈM CÁC ĐIỀU KIỆN BẮT BUỘC)**

Hệ thống **ĐỦ ĐIỀU KIỆN TRIỂN KHAI PRODUCTION** khi và chỉ khi đội ngũ vận hành đáp ứng đúng 3 điều kiện sau:

1. **Khởi tạo biến môi trường:** Bắt buộc đặt tên biến là `OTP_HMAC_SECRET` (không dùng `APP_OTP_HMAC_SECRET`), độ dài secret ngẫu nhiên $\ge 32$ ký tự.
2. **Thực thi Migration:** Chạy script `database/migration_add_email_verification.sql` đơn tiến trình trong maintenance window; xác nhận marker `20261107_email_otp` đạt trạng thái `COMPLETED`.
3. **Cấu hình Nginx IP Rate Limit:** Khi triển khai IP Rate Limit tại Nginx, phải cấu hình đồng thời module `real_ip` tương thích với hạ tầng mạng VPS/Cloudflare trước khi kích hoạt `limit_req`.

