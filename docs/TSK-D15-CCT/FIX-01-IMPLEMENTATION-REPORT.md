# BÁO CÁO HOÀN THÀNH TRIỂN KHAI [FIX-01]
## Khắc phục Lỗ hổng OAuth2 Ticket Invalidation / Denial of Service (Atomic Compare-And-Delete)

- **Dự án:** Rikkei Ecommerce  
- **Mã công việc:** `[FIX-01]`  
- **Vấn đề khắc phục:** Lỗ hổng bảo mật `[SEC-FIND-01]` – Tiêu thụ vé trước khi xác minh Browser Binding Cookie gây nguy cơ Từ chối dịch vụ (DoS)  
- **Người thực hiện:** Senior Java Backend Engineer & Application Security Engineer (Cao Chí Thiện)  
- **Thời điểm hoàn thành:** 13/11/2026  
- **Trạng thái:** **HOÀN THÀNH 100% (VERIFIED & TESTED)**  

---

### 1. Executive Summary

Bản vá `[FIX-01]` đã giải quyết triệt để lỗ hổng an ninh `[SEC-FIND-01]` trong cơ chế trao đổi One-Time Ticket của module Google OAuth2 (`POST /api/v1/auth/oauth2/exchange`).

Bằng việc chuyển đổi quy trình tiêu thụ vé từ cơ chế `GETDEL` (xóa trước kiểm tra sau) sang mô hình **Atomic Compare-And-Delete (CAD)** thực thi hoàn toàn bên trong Redis Lua Script:
1. **Nếu cookie binding không hợp lệ hoặc thiếu:** Backend trả về mã lỗi HTTP 401 Unauthorized, vé xác thực được **BẢO TOÀN NGUYÊN VẸN TRONG REDIS** (không bị xóa). Người dùng chính chủ từ trình duyệt thật vẫn có thể tiếp tục trao đổi vé thành công.
2. **Nếu cookie binding hợp lệ:** Vé được xóa khỏi Redis **nguyên tử** ngay trong cùng chu kỳ xử lý của Redis và trả dữ liệu ticket cho Java phát hành JWT Access Token và Refresh Token.
3. **Chống Replay Attack & Concurrency Race Condition:** Vé chỉ có thể tiêu thụ đúng một lần duy nhất. Khi có nhiều request đồng thời, chỉ duy nhất 1 request hợp lệ đầu tiên thành công; mọi request còn lại đều bị từ chối 401.

Toàn bộ **12 kịch bản an ninh** (`FIX01-T01` đến `FIX01-T12`) và **288 bài kiểm thử hồi quy** toàn hệ thống đều đạt `BUILD SUCCESSFUL` với **0 lỗi**.

---

### 2. Root Cause (Nguyên nhân gốc rễ)

Trong phiên bản ban đầu:
```java
// Logic cũ: Đọc và XÓA VÉ NGAY LẬP TỨC trên Redis
String getAndDelScript =
        "local val = redis.call('GET', KEYS[1])\n" +
        "if val then\n" +
        "    redis.call('DEL', KEYS[1])\n" +
        "end\n" +
        "return val";
```
Thao tác `DEL` diễn ra trước khi Java kiểm tra `ticketData.getBindingHash().equals(bindingHash)`.  
Nếu kẻ tấn công thu thập được chuỗi `ticket` từ URL (thông qua Referer header, web proxy log hoặc browser history) và gửi request với cookie giả mạo `oauth2_binding=ATTACKER`, script trên sẽ xóa sạch vé khỏi Redis. Dù kẻ tấn công bị từ chối (401), nhưng người dùng chính chủ khi thực hiện exchange ngay sau đó cũng bị từ chối (401) vì vé đã biến mất khỏi Redis -> **Tấn công Từ chối Dịch vụ (DoS) thành công**.

---

### 3. Files Modified (Các file đã chỉnh sửa)

1. **Backend Service:**  
   [`backend/src/main/java/com/example/ecommerce/service/AuthService.java`](file:///d:/Doanh%20nghiep/20-10-2026/Project-Rikkei-Ecommerce/backend/src/main/java/com/example/ecommerce/service/AuthService.java)
   - Tích hợp Lua Script Atomic Compare-And-Delete `COMPARE_AND_DELETE_TICKET_LUA_SCRIPT`.
   - Xử lý các mã trạng thái phản hồi từ Redis: `0` (Not Found / Expired), `1` (Success / Consumed), `2` (Binding Mismatch / Preserved).
   - Bổ sung lớp bảo vệ kép (Defense-in-Depth) chống Timing Attack.

2. **Backend Unit & Concurrency Test:**  
   [`backend/src/test/java/com/example/ecommerce/security/oauth2/OAuth2TicketExchangeTest.java`](file:///d:/Doanh%20nghiep/20-10-2026/Project-Rikkei-Ecommerce/backend/src/test/java/com/example/ecommerce/security/oauth2/OAuth2TicketExchangeTest.java)
   - Mở rộng toàn bộ 12 test cases kiểm thử chức năng, chống đánh cắp vé DoS, chống replay attack và concurrency testing với `ExecutorService` & `CountDownLatch`.

---

### 4. Technical Implementation (Chi tiết giải pháp kỹ thuật)

#### A. Redis Lua Script (Atomic Compare-And-Delete)
```lua
local ticketJson = redis.call('GET', KEYS[1])
if not ticketJson then
    return {0, ''}
end

local ok, ticket = pcall(cjson.decode, ticketJson)
if not ok or not ticket then
    return {0, ''}
end

local storedHash = ticket['bindingHash']
local expectedHash = ARGV[1]

-- So sánh bindingHash: Nếu không khớp, trả về status 2 và TUYỆT ĐỐI KHÔNG XÓA VÉ
if not storedHash or storedHash ~= expectedHash then
    return {2, ''}
end

-- Nếu khớp: Xóa vé nguyên tử và trả về status 1 cùng dữ liệu ticket JSON
redis.call('DEL', KEYS[1])
return {1, ticketJson}
```

#### B. Quy ước trạng thái (Status Contract)
- `0`: Ticket không tồn tại, đã hết hạn TTL hoặc dữ liệu JSON bị hỏng -> Ném `BadCredentialsException("Mã xác thực không hợp lệ hoặc đã hết hạn")`.
- `1`: Binding hợp lệ, ticket được xóa thành công -> Deserialize JSON và tiếp tục quy trình phát hành token.
- `2`: Binding không hợp lệ -> Ghi log cảnh báo an ninh, ném `BadCredentialsException("Mã xác thực không khớp với phiên trình duyệt khởi tạo")`, vé trong Redis giữ nguyên TTL ban đầu.

#### C. Xử lý trong `AuthService.java`
- Nếu `bindingSecret` bị rỗng hoặc null: Kiểm tra ngay ở đầu hàm và ném lỗi 401, không gọi Redis, không làm ảnh hưởng vé.
- Truyền `redisKey` (`KEYS[1]`) và `bindingHash` (`ARGV[1]`) vào `DefaultRedisScript<List>`.
- Giải mã kết quả đa phần tử `List<?>` an toàn kiểu `Number` và `String`.
- Giữ nguyên toàn bộ các bước kiểm tra tài khoản tiếp theo: `userRepository.findById`, `user.getActive()`, `user.getRole() == Role.CUSTOMER`, và xóa cookie `oauth2_binding` sau khi cấp token.

---

### 5. Lua Script Atomicity Explanation (Tính nguyên tử của Redis Lua Script)

Redis vận hành theo kiến trúc đơn luồng (Single-Threaded Event Loop) khi thực thi các lệnh thao tác dữ liệu. Khi một Lua script được thực thi thông qua lệnh `EVAL` / `EVALSHA`:
1. **Không có lệnh xen ngang (Isolation):** Không bất kỳ một client hay request nào khác có thể chen vào giữa các lệnh `redis.call('GET')`, kiểm tra `storedHash == expectedHash`, và `redis.call('DEL')`.
2. **Khắc phục triệt để Time-of-Check to Time-of-Use (TOCTOU):** Trong mô hình cũ hoặc mô hình đọc trong Java rồi xóa sau, tồn tại khoảng trễ mạng (network window) giữa lúc đọc và lúc xóa. Trong mô hình mới, bước kiểm tra binding và bước xóa xảy ra trong **cùng một microsecond trên bộ nhớ Redis**.
3. **An toàn JSON với `pcall`:** Sử dụng hàm bảo vệ `pcall(cjson.decode, ticketJson)` đảm bảo nếu dữ liệu trong Redis có bất thường, script vẫn bắt được lỗi và trả về an toàn thay vì làm sập runtime của Redis.

---

### 6. Security Improvements (Những cải tiến an ninh đạt được)

| Nguy cơ bảo mật | Trước bản vá FIX-01 | Sau bản vá FIX-01 |
| :--- | :--- | :--- |
| **DoS qua Ticket Invalidation** | Kẻ tấn công gửi cookie sai -> Vé bị xóa ngay -> Người dùng chính chủ bị lỗi 401. | Vé được giữ nguyên trong Redis (status 2); kẻ tấn công nhận 401; người dùng thật exchange thành công. |
| **Replay Attack** | Có chống replay nhưng kèm rủi ro DoS. | Chống replay an toàn: Lần 1 thành công xóa vé, lần 2 nhận status 0 (từ chối 401). |
| **Concurrent Race Conditions** | Hai request đồng thời có thể gây race condition kiểm tra state. | Redis Lua tuần tự hóa tuyệt đối: Chỉ 1 request đầu tiên nhận status 1, request sau nhận status 0. |
| **Missing Cookie Handling** | Nếu thiếu cookie, có thể phát sinh ngoại lệ không mong muốn. | Kiểm tra `!StringUtils.hasText(bindingSecret)` ngay tại Java, chặn 401 tức thì không cần gọi Redis. |

---

### 7. Test Cases and Actual Results (Kết quả kiểm thử chi tiết)

Đã chạy kiểm thử thực tế với lệnh:
```powershell
.\backend\gradlew.bat -p backend test --tests "com.example.ecommerce.security.oauth2.OAuth2TicketExchangeTest"
```

| Mã kiểm thử | Kịch bản kiểm thử | Kỳ vọng | Kết quả thực tế |
| :--- | :--- | :--- | :---: |
| **FIX01-T01** | Ticket hợp lệ + binding hợp lệ | Lua CAD trả status 1, exchange thành công, cấp Access & Refresh Token | **PASSED** |
| **FIX01-T02** | Ticket hợp lệ + binding sai | Lua CAD trả status 2, trả 401, vé không bị xóa khỏi Redis | **PASSED** |
| **FIX01-T03** | Ticket hợp lệ + thiếu cookie binding | Trả 401 ngay tại Java, không tương tác với Redis | **PASSED** |
| **FIX01-T04** | Ticket không tồn tại trong Redis | Lua CAD trả status 0, trả 401 Unauthorized | **PASSED** |
| **FIX01-T05** | Ticket hết hạn TTL | Lua CAD trả status 0, trả 401 Unauthorized | **PASSED** |
| **FIX01-T06** | Ticket đã được sử dụng trước đó | Lua CAD trả status 0, trả 401 Unauthorized | **PASSED** |
| **FIX01-T07** | Ticket thiếu trường `bindingHash` | Lua CAD trả status 2, trả 401, vé không bị tiêu thụ | **PASSED** |
| **FIX01-T08** | Binding đúng nhưng User bị khóa | Tiêu thụ vé nhưng ném `LockedAccountException`, không cấp token | **PASSED** |
| **FIX01-T09** | **Ticket Theft / DoS Prevention:** Attacker gửi binding sai -> bị 401, sau đó người dùng thật gửi binding đúng -> thành công | Attacker bị từ chối và vé không mất; người dùng thật đăng nhập thành công | **PASSED** |
| **FIX01-T10** | **Replay Attack:** Gọi exchange lần 1 thành công; gọi lại lần 2 bị từ chối | Lần 1 trả AuthResponse; lần 2 ném BadCredentialsException | **PASSED** |
| **FIX01-T11** | **Concurrent Exchange:** 2 thread đồng thời cùng gửi ticket và binding hợp lệ | Duy nhất 1 thread thành công, 1 thread nhận 401 (nguyên tử) | **PASSED** |
| **FIX01-T12** | **Concurrent Invalid & Valid:** Thread A (binding sai) và Thread B (binding đúng) chạy đồng thời | Thread A bị từ chối 401, Thread B đổi vé thành công không bị gián đoạn | **PASSED** |

---

### 8. Regression Test Results (Kết quả kiểm thử hồi quy toàn diện)

#### A. Trọn bộ package OAuth2
Lệnh: `.\backend\gradlew.bat -p backend test --tests "com.example.ecommerce.security.oauth2.*"`
- `CustomOidcUserServiceTest`: **6/6 PASSED**
- `OAuth2ControllerIntegrationTest`: **3/3 PASSED**
- `OAuth2TicketExchangeTest`: **12/12 PASSED**
- **Tổng cộng:** **21/21 tests PASSED (100%)**

#### B. Trọn bộ hồi quy Authentication & Security
Lệnh: `.\backend\gradlew.bat -p backend test --tests "*Auth*" --tests "*Security*" --tests "*Jwt*" --tests "*User*" --tests "*Token*"`
- **Tổng số tests thực thi:** **288 tests**
- **Thành công:** **288 tests (100%)**
- **Thất bại:** **0**
- **Bỏ qua (Ignored):** 3 (các mock bên thứ ba sandbox)
- **Thời gian thực thi:** 8.589s (`BUILD SUCCESSFUL`)

Khẳng định: Bản vá `[FIX-01]` không gây bất kỳ tác động tiêu cực nào tới luồng đăng nhập thông thường bằng email/mật khẩu, xác thực JWT hay cơ chế xoay vòng Refresh Token hiện có.

---

### 9. Remaining Risks (Các rủi ro tồn đọng ngoài phạm vi FIX-01)

Theo đúng nguyên tắc phân định phạm vi:
1. `[SEC-FIND-02]` (Database migration trên volume cũ): Cần chạy lệnh migration CLI khi deploy trên container MySQL cũ.
2. `[SEC-FIND-03]` (Circular Dependency & `@Lazy`): Cần bóc tách `PasswordEncoderConfig` theo kế hoạch `FIX-04` (P1).
3. `[SEC-FIND-05]` (Nginx `$host` làm mất port 3000): Cần cập nhật `nginx.conf` theo kế hoạch `FIX-03` (P1).
4. `Google Credentials`: Chờ quản trị viên cung cấp `GOOGLE_CLIENT_ID` và `GOOGLE_CLIENT_SECRET` để kiểm thử live end-to-end trên Google Cloud.

---

### 10. Acceptance Criteria Checklist (Tiêu chí nghiệm thu)

- [x] Lua Script xử lý GET, kiểm tra binding và DEL nguyên tử.
- [x] Binding sai không làm mất ticket trong Redis.
- [x] Binding thiếu không làm mất ticket trong Redis.
- [x] Binding hợp lệ tiêu thụ ticket thành công đúng 1 lần.
- [x] Ticket chỉ sử dụng được một lần duy nhất (chống replay attack).
- [x] Hai request exchange đồng thời không thể cùng thành công (chống race condition).
- [x] Request với binding sai không thể vô hiệu hóa request của người dùng chính chủ (chống DoS).
- [x] Các kiểm tra tài khoản (`active`, role `CUSTOMER`) và phân quyền hiện tại vẫn hoạt động chuẩn xác.
- [x] Không thay đổi API contract của Frontend (`POST /api/v1/auth/oauth2/exchange`).
- [x] Toàn bộ 21 OAuth2 regression tests và 288 auth tests đạt 100%.
- [x] Không phát sinh bất kỳ cảnh báo hoặc lỗi biên dịch (`BUILD SUCCESSFUL`).

---

### 11. Final Verdict

### **KẾT LUẬN: `PASS – REMEDIATION SUCCESSFUL`**

Bản vá `[FIX-01]` đã được kiểm chứng bằng thực nghiệm mã nguồn và bộ kiểm thử tự động đa luồng. Lỗ hổng `[SEC-FIND-01]` đã được đóng hoàn toàn. Hệ thống đã sẵn sàng cho các bước tiếp theo trong quy trình triển khai.

