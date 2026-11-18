# BỘ CÂU HỎI VÀ ĐÁP ÁN BẢO VỆ KỸ THUẬT DỰ ÁN RIKKEI ECOMMERCE
## TSK-D17-CCT | TÀI LIỆU CHUẨN BỊ PHẢN BIỆN DÀNH CHO CAO CHÍ THIỆN (CCT)

---

> **Mục đích tài liệu:**
> Cung cấp 27 kịch bản câu hỏi phản biện chuyên sâu từ Hội đồng chấm đồ án/kiến trúc phần mềm cho toàn bộ các mảng công việc CCT phụ trách (D01 – D17): Kiến trúc Client-Server, Authentication JWT/Redis, Google OAuth2/OIDC, Phân quyền RBAC, Quản trị Review, An toàn thông tin và Vận hành hệ thống.
>
> **Cấu trúc chuẩn của từng câu hỏi (6 phần):**
> 1. **Câu hỏi của hội đồng:** Trọng tâm vấn đề hội đồng muốn chất vấn.
> 2. **Câu trả lời ngắn gọn (30–45 giây):** Dành để trả lời mở đầu tự tin, súc tích, đi thẳng vào bản chất kỹ thuật.
> 3. **Giải thích kỹ thuật chuyên sâu:** Chi tiết kiến trúc, thuật toán, luồng dữ liệu, lý do thiết kế (trade-offs).
> 4. **Thành phần hoặc class tương ứng trong source code:** File path, class name, method cụ thể làm bằng chứng thực tế.
> 5. **Câu hỏi truy vấn sâu hơn có thể phát sinh:** Các câu hỏi bẫy hoặc đào sâu (follow-up questions) hội đồng có thể hỏi tiếp.
> 6. **Cách trả lời khi tính năng chưa được triển khai đầy đủ hoặc có giới hạn:** Chiến lược trả lời trung thực, minh bạch, bảo vệ bản thân và đưa ra roadmap nâng cấp chuẩn Software Engineer.

---

# MỤC LỤC

- [NHÓM 1: KIẾN TRÚC HỆ THỐNG VÀ CLIENT-SERVER (CÂU 1 – 5)](#nhóm-1-kiến-trúc-hệ-thống-và-client-server-câu-1--5)
  - [Câu 1: Lựa chọn Single Page Application (React) + REST API (Spring Boot) thay vì Server-Side Rendering (SSR)](#câu-1-lựa-chọn-single-page-application-react--rest-api-spring-boot-thay-vì-server-side-rendering-ssr)
  - [Câu 2: Vai trò của Nginx Reverse Proxy trong môi trường Docker Compose](#câu-2-vai-trò-của-nginx-reverse-proxy-trong-môi-trường-docker-compose)
  - [Câu 3: Khả năng mở rộng ngang (Horizontal Scalability) và tính Stateless của Backend](#câu-3-khả-năng-mở-rộng-ngang-horizontal-scalability-và-tính-stateless-của-backend)
  - [Câu 4: Chiến lược phân tách cơ sở dữ liệu: MySQL 8.0 và Redis 7.0](#câu-4-chiến-lược-phân-tách-cơ-sở-dữ-liệu-mysql-80-và-redis-70)
  - [Câu 5: Vòng đời xử lý trọn vẹn của một HTTP Request qua các lớp kiến trúc](#câu-5-vòng-đời-xử-lý-trọn-vẹn-của-một-http-request-qua-các-lớp-kiến-trúc)
- [NHÓM 2: JWT VÀ CƠ CHẾ AUTHENTICATION (CÂU 6 – 10)](#nhóm-2-jwt-và-cơ-chế-authentication-câu-6--10)
  - [Câu 6: Mô hình Dual-Token: Access Token ngắn hạn và Refresh Token dài hạn](#câu-6-mô-hình-dual-token-access-token-ngắn-hạn-và-refresh-token-dài-hạn)
  - [Câu 7: Cơ chế Refresh Token Rotation (RTR) và xử lý đồng thời (Race Condition) với Grace Period](#câu-7-cơ-chế-refresh-token-rotation-rtr-và-xử-lý-đồng-thời-race-condition-với-grace-period)
  - [Câu 8: Xử lý phát hiện tái sử dụng Token (Token Reuse Detection) khi bị tấn công](#câu-8-xử-lý-phát-hiện-tái-sử-dụng-token-token-reuse-detection-khi-bị-tấn-công)
  - [Câu 9: Vấn đề thu hồi Access Token tức thì (Instant Token Revocation) và Token Blacklisting](#câu-9-vấn-đề-thu-hồi-access-token-tức-thì-instant-token-revocation-và-token-blacklisting)
  - [Câu 10: Thuật toán mã hóa mật khẩu BCrypt và độ an toàn của Work Factor = 12](#câu-10-thuật-toán-mã-hóa-mật-khẩu-bcrypt-và-độ-an-toàn-của-work-factor--12)
- [NHÓM 3: GOOGLE OAUTH2 VÀ OPENID CONNECT (CÂU 11 – 16)](#nhóm-3-google-oauth2-và-openid-connect-câu-11--16)
  - [Câu 11: Lựa chọn Authorization Code Flow kết hợp Backend Token Exchange thay vì Implicit Flow](#câu-11-lựa-chọn-authorization-code-flow-kết-hợp-backend-token-exchange-thay-vì-implicit-flow)
  - [Câu 12: Sự cố CAD (Client Authentication Disallowed - FIX-01) và giải pháp kỹ thuật](#câu-12-sự-cố-cad-client-authentication-disallowed---fix-01-và-giải-pháp-kỹ-thuật)
  - [Câu 13: Quy trình liên kết tài khoản (Account Linking) và khởi tạo thông tin người dùng Google](#câu-13-quy-trình-liên-kết-tài-khoản-account-linking-và-khởi-tạo-thông-tin-người-dùng-google)
  - [Câu 14: Phân biệt Google ID Token, Google Access Token và Internal Rikkei JWT](#câu-14-phân-biệt-google-id-token-google-access-token-và-internal-rikkei-jwt)
  - [Câu 15: Phòng chống tấn công CSRF và Replay Attack trong luồng OAuth2](#câu-15-phòng-chống-tấn-công-csrf-và-replay-attack-trong-luồng-oauth2)
  - [Câu 16: Kịch bản chịu lỗi (Fault Tolerance) khi Google Identity Provider gặp sự cố gián đoạn](#câu-16-kịch-bản-chịu-lỗi-fault-tolerance-khi-google-identity-provider-gặp-sự-cố-gián-đoạn)
- [NHÓM 4: PHÂN QUYỀN RBAC VÀ QUẢN TRỊ REVIEW (CÂU 17 – 21)](#nhóm-4-phân-quyền-rbac-và-quản-trị-review-câu-17--21)
  - [Câu 17: Kiến trúc phân quyền RBAC đa tầng: URL Matching và Method Security](#câu-17-kiến-trúc-phân-quyền-rbac-đa-tầng-url-matching-và-method-security)
  - [Câu 18: Sự khác biệt và thứ tự thực thi giữa FilterChain Security và `@PreAuthorize`](#câu-18-sự-khác-biệt-và-thứ-tự-thực-thi-giữa-filterchain-security-và-preauthorize)
  - [Câu 19: Phòng chống lỗ hổng kiểm soát truy cập trực tiếp đối tượng (IDOR Protection)](#câu-19-phòng-chống-lỗ-hổng-kiểm-soát-truy-cập-trực-tiếp-đối-tượng-idor-protection)
  - [Câu 20: Quy tắc nghiệp vụ BR-AUTH-04: Ngăn chặn Admin tự khóa tài khoản chính mình](#câu-20-quy-tắc-nghiệp-vụ-br-auth-04-ngăn-chặn-admin-tự-khóa-tài-khoản-chính-mình)
  - [Câu 21: Quản trị đánh giá sản phẩm và xóa file đính kèm với TransactionalEventListener](#câu-21-quản-trị-đánh-giá-sản-phẩm-và-xóa-file-đính-kèm-với-transactionaleventlistener)
- [NHÓM 5: AN TOÀN THÔNG TIN VÀ VẬN HÀNH HỆ THỐNG (CÂU 22 – 27)](#nhóm-5-an-toàn-thông-tin-và-vận-hành-hệ-thống-câu-22--27)
  - [Câu 22: Cơ chế Rate Limiting chống Brute Force và DDoS bằng Bucket4j](#câu-22-cơ-chế-rate-limiting-chống-brute-force-và-ddos-bằng-bucket4j)
  - [Câu 23: 5 lớp phòng thủ trong quy trình xử lý upload hình ảnh (ReviewImage)](#câu-23-5-lớp-phòng-thủ-trong-quy-trình-xử-lý-upload-hình-ảnh-reviewimage)
  - [Câu 24: Tại sao phải kiểm tra Magic Bytes bằng Apache Tika thay vì tin tưởng Content-Type?](#câu-24-tại-sao-phải-kiểm-tra-magic-bytes-bằng-apache-tika-thay-vì-tin-tưởng-content-type)
  - [Câu 25: Cấu hình CORS an toàn trong môi trường Single Page Application](#câu-25-cấu-hình-cors-an-toàn-trong-môi-trường-single-page-application)
  - [Câu 26: Quản lý Secret, Credentials và biến môi trường không lộ lọt vào Git](#câu-26-quản-lý-secret-credentials-và-biến-môi-trường-không-lộ-lọt-vào-git)
  - [Câu 27: Lý do lưu trữ ảnh tại Local Disk thay vì Cloudinary / S3 và giải pháp chuyển đổi](#câu-27-lý-do-lưu-trữ-ảnh-tại-local-disk-thay-vì-cloudinary--s3-và-giải-pháp-chuyển-đổi)

---

# CHI TIẾT NỘI DUNG 27 KỊCH BẢN PHẢN BIỆN

---

## NHÓM 1: KIẾN TRÚC HỆ THỐNG VÀ CLIENT-SERVER (CÂU 1 – 5)

---

### Câu 1: Lựa chọn Single Page Application (React) + REST API (Spring Boot) thay vì Server-Side Rendering (SSR)

1. **Câu hỏi của hội đồng:**  
   *"Tại sao hệ thống Rikkei Ecommerce lại lựa chọn tách rời Client-Server thành React SPA và Spring Boot REST API thay vì sử dụng mô hình Server-Side Rendering truyền thống như Thymeleaf hoặc Next.js?"*

2. **Câu trả lời ngắn gọn (30–45 giây):**  
   *"Dạ thưa Hội đồng, hệ thống chọn kiến trúc Decoupled Client-Server nhằm 3 mục tiêu cốt lõi: Thứ nhất là tối ưu trải nghiệm người dùng thương mại điện tử với khả năng chuyển trang mượt mà không tải lại toàn bộ DOM; Thứ hai là tách biệt hoàn toàn trách nhiệm giữa đội ngũ Frontend và Backend, cho phép Backend phục vụ đa nền tảng (Web, Mobile App tương lai) chỉ thông qua một chuẩn REST API duy nhất; Thứ ba là hỗ trợ Horizontal Scaling độc lập: Backend có thể scale lên nhiều instance không phụ thuộc vào tầng phân phối static file của Frontend."*

3. **Giải thích kỹ thuật chuyên sâu:**
   - **Mô hình kiến trúc:** Client là Single Page Application xây dựng bằng React 18, Vite và TailwindCSS. Backend là Stateless REST API sử dụng Spring Boot 3.3.5.
   - **Tối ưu tài nguyên mạng:** Client chỉ tải HTML/JS bundle một lần ban đầu (được cache qua trình duyệt/Nginx). Toàn bộ tương tác sau đó chỉ truyền tải payload dữ liệu dạng JSON thông qua HTTP/2 hoặc HTTP/1.1, giảm thiểu băng thông so với việc render HTML toàn bộ trang từ server.
   - **Tính độc lập trong triển khai:** Frontend build ra static asset (HTML/CSS/JS) được Nginx phục vụ với hiệu năng cực cao ($I/O$ tĩnh). Backend Java chạy trong JVM chuyên xử lý nghiệp vụ, tối ưu hóa CPU và RAM cho transaction, ORM và cache.
   - **Đánh đổi (Trade-off):** SEO của SPA thuần túy kém hơn SSR. Tuy nhiên, đối với đồ án và ứng dụng e-commerce có yêu cầu bảo mật tài khoản cao (Customer Dashboard, Admin Portal, Checkout), mô hình SPA với token-based authentication mang lại sự linh hoạt tối đa trong việc kiểm soát trạng thái phiên làm việc.

4. **Thành phần hoặc class tương ứng trong source code:**
   - Frontend Build Config: `frontend/vite.config.ts`, `frontend/package.json`.
   - Backend Base REST Config: `backend/src/main/java/com/rikkeisoft/ecommerce/common/ApiResponse.java`.
   - Nginx Routing: `docker/nginx/nginx.conf` (Định tuyến `/` cho Frontend và `/api/` cho Backend).

5. **Câu hỏi truy vấn sâu hơn có thể phát sinh:**
   - *"Nếu e-commerce cần SEO cho trang chi tiết sản phẩm, hệ thống của em giải quyết vấn đề đó như thế nào khi dùng React SPA?"*
   - *Hướng trả lời:* Có thể tích hợp Prerender/SSR cho các route công khai (`/products/*`) bằng Node.js reverse proxy hoặc chuyển đổi các trang landing sang Next.js, trong khi toàn bộ nghiệp vụ transactional và quản trị giữ nguyên trên Spring Boot API.

6. **Cách trả lời khi tính năng chưa được triển khai đầy đủ hoặc có giới hạn:**
   - *"Trong phạm vi hiện tại của dự án, trọng tâm kỹ thuật được đặt vào hiệu năng xử lý giao dịch, bảo mật xác thực JWT/OAuth2 và phân quyền quản trị Admin. Tính năng Dynamic SSR phục vụ SEO công cụ tìm kiếm đã được khảo sát và sẽ là bước nâng cấp tiếp theo ở giai đoạn Go-Live thương mại."*

---

### Câu 2: Vai trò của Nginx Reverse Proxy trong môi trường Docker Compose

1. **Câu hỏi của hội đồng:**  
   *"Tại sao các bạn lại đặt Nginx làm Reverse Proxy phía trước Spring Boot và React thay vì cho Client gọi trực tiếp vào port 8080 của backend?"*

2. **Câu trả lời ngắn gọn (30–45 giây):**  
   *"Dạ thưa Hội đồng, Nginx đóng vai trò là Single Point of Entry (Cổng vào duy nhất) của toàn bộ hệ sinh thái. Nginx giải quyết 4 bài toán lớn: Thứ nhất, triệt tiêu bài toán CORS phức tạp ở môi trường production vì cả Web và API đều phục vụ trên cùng một domain/port 80; Thứ hai, bảo vệ Backend Spring Boot không lộ trực tiếp port nội bộ ra ngoài Internet; Thứ ba, tối ưu hóa phục vụ static files của React; Và thứ tư, sẵn sàng làm Software Load Balancer khi Backend mở rộng nhiều instance."*

3. **Giải thích kỹ thuật chuyên sâu:**
   - **Định tuyến thông minh:** Nginx phân luồng request theo path prefix:
     - `/api/` $\rightarrow$ Reverse proxy chuyển tiếp vào `http://backend:8080/api/` với các HTTP headers quan trọng (`Host`, `X-Real-IP`, `X-Forwarded-For`, `X-Forwarded-Proto`).
     - `/uploads/` $\rightarrow$ Phục vụ static images trực tiếp từ shared volume `/app/uploads` mà không cần gọi vào JVM, giải phóng thread của Tomcat.
     - `/` $\rightarrow$ Phục vụ React SPA static files (`index.html`, assets) với chỉ thị `try_files $uri $uri/ /index.html` để hỗ trợ HTML5 Client-side Routing.
   - **Bảo mật mạng (Network Isolation):** Trong file `docker-compose.yml`, các service `mysql`, `redis` và `backend` nằm trong private bridge network `app-network`. Chỉ duy nhất container `nginx` công khai port 80 ra host, giảm thiểu tối đa bề mặt tấn công (attack surface).

4. **Thành phần hoặc class tương ứng trong source code:**
   - Cấu hình Nginx: `docker/nginx/nginx.conf`.
   - Cấu hình mạng Docker: `docker-compose.yml` (dịch vụ `nginx`, `backend`, `frontend`, `mysql`, `redis`).

5. **Câu hỏi truy vấn sâu hơn có thể phát sinh:**
   - *"Khi client gửi request qua Nginx, làm thế nào Backend Spring Boot biết được IP thật của người dùng để thực hiện Rate Limiting?"*
   - *Hướng trả lời:* Nginx đã đính kèm header `proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;` và Spring Boot đã cấu hình `server.forward-headers-strategy=native` (hoặc framework) để đọc IP nguồn thật qua `request.getRemoteAddr()`.

6. **Cách trả lời khi tính năng chưa được triển khai đầy đủ hoặc có giới hạn:**
   - *"Hiện tại trong môi trường local/dev, hệ thống hỗ trợ cả 2 chế độ: chạy qua Nginx container port 80 để mô phỏng production, hoặc chạy dev server Vite (port 5173) gọi trực tiếp Backend (port 8080) với cấu hình CORS linh hoạt trong `SecurityConfig`."*

---

### Câu 3: Khả năng mở rộng ngang (Horizontal Scalability) và tính Stateless của Backend

1. **Câu hỏi của hội đồng:**  
   *"Nếu lưu lượng truy cập tăng gấp 10 lần, hệ thống mở rộng như thế nào? Spring Boot Backend có giữ session người dùng trong bộ nhớ không?"*

2. **Câu trả lời ngắn gọn (30–45 giây):**  
   *"Dạ thưa Hội đồng, Backend Spring Boot được thiết kế tuân thủ hoàn toàn nguyên lý Stateless Architecture (Không trạng thái). Hệ thống cấu hình `SessionCreationPolicy.STATELESS`, nghĩa là Spring Security không tạo bất kỳ `HttpSession` nào trên bộ nhớ RAM của JVM. Mọi thông tin xác thực đều truyền qua JWT Access Token và Refresh Token được quản lý tập trung trên Redis Cluster. Do đó, hệ thống có thể mở rộng ngang từ 1 instance lên $N$ instance Backend chỉ bằng một lệnh scale mà không gặp vấn đề dính phiên (sticky session)."*

3. **Giải thích kỹ thuật chuyên sâu:**
   - **Triệt tiêu Session:** Cấu hình `http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))` trong `SecurityConfig.java`. Mỗi request đến backend là hoàn toàn độc lập, chứa đầy đủ ngữ cảnh xác thực trong HTTP header `Authorization: Bearer <JWT>`.
   - **Tập trung hóa trạng thái phiên:** Trạng thái refresh token, blacklisted token và rate limiting counter đều được lưu trữ tại Redis 7.0 (External in-memory store) thay vì bộ nhớ cục bộ của JVM.
   - **Khả năng Scale:** Khi cần scale:
     ```bash
     docker compose up --scale backend=3 -d
     ```
     Nginx cấu hình upstream `round-robin` hoặc `least_conn` để phân phối tải đều cho 3 backend containers.

4. **Thành phần hoặc class tương ứng trong source code:**
   - Cấu hình Stateless: `backend/src/main/java/com/rikkeisoft/ecommerce/config/SecurityConfig.java`.
   - Redis Token Store: `backend/src/main/java/com/rikkeisoft/ecommerce/service/impl/AuthServiceImpl.java`.
   - JWT Filter không dùng Session: `backend/src/main/java/com/rikkeisoft/ecommerce/security/JwtAuthenticationFilter.java`.

5. **Câu hỏi truy vấn sâu hơn có thể phát sinh:**
   - *"Nếu Backend hoàn toàn Stateless, thì khi scale nhiều instance, việc upload file ảnh Review lưu ở local disk có bị lỗi không?"*
   - *Hướng trả lời:* Đây là điểm mấu chốt. Nếu scale nhiều container backend mà dùng local disk thì ảnh upload ở container A sẽ không có ở container B. Để giải quyết, hiện tại Docker Compose mount một Shared Volume chung giữa các container và Nginx. Ở cấp độ production lớn hơn, giải pháp chuẩn là chuyển sang S3/MinIO Object Storage.

6. **Cách trả lời khi tính năng chưa được triển khai đầy đủ hoặc có giới hạn:**
   - *"Hệ thống hiện tại đã đạt chuẩn Stateless ở tầng Logic và Authentication. Về tầng File Storage, hiện đang dùng Docker Shared Volume. Trong kiến trúc tương lai, chúng em đã thiết kế sẵn interface `FileStorageService` để sẵn sàng chuyển sang AWS S3 hoặc MinIO khi triển khai Kubernetes."*

---

### Câu 4: Chiến lược phân tách cơ sở dữ liệu: MySQL 8.0 và Redis 7.0

1. **Câu hỏi của hội đồng:**  
   *"Tại sao hệ thống lại cần cả MySQL và Redis? Có thể bỏ Redis đi và lưu toàn bộ Token, OTP, Rate Limiting vào MySQL được không?"*

2. **Câu trả lời ngắn gọn (30–45 giây):**  
   *"Dạ thưa Hội đồng, hoàn toàn có thể lưu vào MySQL nhưng đó là anti-pattern về hiệu năng. MySQL là Relational DBMS lưu trữ trên đĩa cứng (Disk-based), tuân thủ nghiêm ngặt chuẩn ACID, rất phù hợp cho dữ liệu nghiệp vụ có tính toàn vẹn cao như Người dùng, Sản phẩm, Đơn hàng và Thanh toán. Ngược lại, Redis là In-Memory Data Store, xử lý hàng trăm nghìn thao tác mỗi giây với độ trễ micro-giây. Dùng Redis cho Token rotation, OTP và Rate Limit giúp bảo vệ MySQL khỏi bị quá tải (I/O Bottleneck) và tự động dọn dẹp dữ liệu rác qua cơ chế TTL (Time-To-Live)."*

3. **Giải thích kỹ thuật chuyên sâu:**
   - **Tối ưu hóa I/O:** Nếu mỗi request API kiểm tra rate limit hoặc mỗi lần refresh token đều `SELECT`/`UPDATE`/`DELETE` vào bảng MySQL, database sẽ phải chịu tải I/O ghi đĩa rất lớn, dễ dẫn đến lock bảng/lock hàng và phân mảnh dữ liệu.
   - **Cơ chế TTL tự động:** Redis hỗ trợ `EXPIRE` tự nhiên. Khi Refresh Token hết hạn (7 ngày) hoặc OTP hết hạn (5 phút), Redis tự động thu hồi bộ nhớ mà không cần viết cronjob chạy background quét bảng MySQL để xóa rác (`DELETE FROM tokens WHERE expired_at < NOW()`), giảm thiểu Table Bloat.
   - **Cấu trúc dữ liệu chuyên biệt:**
     - String/Key-Value: Dùng cho Opaque Refresh Token (`refresh_tokens:{userId}`).
     - TTL-based Key: Dùng cho OTP Forgot Password (`otp:{email}`).
     - Token Bucket: Dùng cho Rate Limiting với Bucket4j.

4. **Thành phần hoặc class tương ứng trong source code:**
   - Cấu hình Redis: `backend/src/main/java/com/rikkeisoft/ecommerce/config/RedisConfig.java`.
   - Redis Service Interface: `backend/src/main/java/com/rikkeisoft/ecommerce/service/RedisService.java`.
   - MySQL Entities: Package `backend/src/main/java/com/rikkeisoft/ecommerce/entity/*`.

5. **Câu hỏi truy vấn sâu hơn có thể phát sinh:**
   - *"Nếu server Redis bị crash hoặc restart đột ngột, toàn bộ phiên đăng nhập của người dùng có bị mất hết không?"*
   - *Hướng trả lời:* Redis hỗ trợ cơ chế Persistence thông qua RDB (Snapshot định kỳ) và AOF (Append-Only File ghi nhận mọi lệnh ghi). Trong `docker-compose.yml`, Redis volume được map ra host (`redis-data:/data`), giúp dữ liệu token và cache phục hồi ngay khi container khởi động lại.

6. **Cách trả lời khi tính năng chưa được triển khai đầy đủ hoặc có giới hạn:**
   - *"Trong môi trường hiện tại, Redis đang chạy single-node với AOF persistence. Đối với hệ thống quy mô lớn, chúng em đã chuẩn bị phương án nâng cấp lên Redis Sentinel (High Availability) hoặc Redis Cluster để đảm bảo tính chịu lỗi 99.99%."*

---

### Câu 5: Vòng đời xử lý trọn vẹn của một HTTP Request qua các lớp kiến trúc

1. **Câu hỏi của hội đồng:**  
   *"Hãy trình bày vòng đời (Lifecycle) của một request gọi API GET /api/v1/orders từ lúc người dùng nhấn nút trên giao diện React đến khi nhận được dữ liệu?"*

2. **Câu trả lời ngắn gọn (30–45 giây):**  
   *"Dạ thưa Hội đồng, vòng đời request trải qua 6 chặng chính: (1) React Client gửi HTTP Request kèm Bearer Token qua Axios Interceptor; (2) Nginx Reverse Proxy tiếp nhận ở port 80 và chuyển tiếp vào backend:8080; (3) Spring Security FilterChain giải mã JWT, kiểm tra tính hợp lệ và inject UserAuthentication vào SecurityContext; (4) DispatcherServlet định tuyến đến OrderController; (5) OrderService kiểm tra IDOR và gọi OrderRepository lấy dữ liệu qua Spring Data JPA; (6) Dữ liệu Entity được ánh xạ sang DTO, bọc trong ApiResponse<PageResponse<OrderDTO>> và trả về Client với HTTP 200 OK."*

3. **Giải thích kỹ thuật chuyên sâu:**
   - **Chặng 1 (Client):** Axios interceptor đính kèm `Authorization: Bearer <access_token>`.
   - **Chặng 2 (Network & Proxy):** Nginx tiếp nhận, kiểm tra header, chuyển tiếp qua Docker bridge network tới Backend.
   - **Chặng 3 (Security Filter):** `JwtAuthenticationFilter` trích xuất token từ `Authorization` header, gọi `JwtService.validateToken()`. Sử dụng HMAC-SHA256 Secret Key kiểm tra chữ ký số, tính toàn vẹn và thời hạn hết hạn (`exp`). Trích xuất `username` và `roles`, khởi tạo `UsernamePasswordAuthenticationToken` và đưa vào `SecurityContextHolder.getContext().setAuthentication(...)`.
   - **Chặng 4 (Authorization & Controller):** Filter Security kiểm tra quyền truy cập (`/api/v1/orders/**` yêu cầu `hasAnyRole('CUSTOMER', 'ADMIN')`). Controller nhận request, kích hoạt `@Valid` nếu có body/param.
   - **Chặng 5 (Service & Database):** Service lấy thông tin người dùng từ `Authentication.getName()`. Thực hiện query MySQL thông qua Hibernate/JPA: `orderRepository.findByUserId(userId, pageable)`.
   - **Chặng 6 (Response):** Controller trả về `ApiResponse.success(data)`. `GlobalExceptionHandler` luôn túc trực nếu có bất kỳ RuntimeException nào phát sinh.

4. **Thành phần hoặc class tương ứng trong source code:**
   - Client Interceptor: `frontend/src/api/axiosClient.ts`.
   - Filter xác thực: `backend/src/main/java/com/rikkeisoft/ecommerce/security/JwtAuthenticationFilter.java`.
   - Provider & Token Logic: `backend/src/main/java/com/rikkeisoft/ecommerce/security/JwtService.java`.
   - Security Config: `backend/src/main/java/com/rikkeisoft/ecommerce/config/SecurityConfig.java`.
   - Chuẩn dữ liệu phản hồi: `backend/src/main/java/com/rikkeisoft/ecommerce/common/ApiResponse.java`.

5. **Câu hỏi truy vấn sâu hơn có thể phát sinh:**
   - *"Nếu JWT Access Token hết hạn đúng lúc gửi request này, luồng xử lý trên Client và Server diễn ra như thế nào?"*
   - *Hướng trả lời:* Server trả về HTTP 401 Unauthorized (`TOKEN_EXPIRED`). Axios Response Interceptor trên Frontend bắt mã 401, tạm dừng request, gọi API `/api/v1/auth/refresh-token` để lấy Access Token mới, cập nhật lại header và tự động replay lại request lấy đơn hàng mà người dùng không nhận thấy gián đoạn.

6. **Cách trả lời khi tính năng chưa được triển khai đầy đủ hoặc có giới hạn:**
   - *"Toàn bộ luồng xử lý request chuẩn này đã được hiện thực và kiểm chứng 100% trong mã nguồn, bao gồm cả các bài kiểm thử MockMvc và kiểm thử tích hợp trên Swagger/Postman."*

---

## NHÓM 2: JWT VÀ CƠ CHẾ AUTHENTICATION (CÂU 6 – 10)

---

### Câu 6: Mô hình Dual-Token: Access Token ngắn hạn và Refresh Token dài hạn

1. **Câu hỏi của hội đồng:**  
   *"Tại sao các bạn lại sử dụng mô hình Dual-Token (Access Token 15 phút + Refresh Token 7 ngày) thay vì phát hành một Access Token có thời hạn dài như 7 ngày luôn cho đơn giản?"*

2. **Câu trả lời ngắn gọn (30–45 giây):**  
   *"Dạ thưa Hội đồng, đây là quy chuẩn bảo mật bắt buộc của RFC 6749 và OAuth 2.0. JWT Access Token có đặc tính Stateless, nghĩa là khi đã phát hành thì không thể thu hồi ngoại trừ việc chờ nó tự hết hạn. Nếu cấp token 7 ngày và token bị kẻ xấu đánh cắp (qua XSS hoặc Man-In-The-Middle), kẻ tấn công sẽ toàn quyền kiểm soát tài khoản trong suốt 7 ngày. Việc giới hạn Access Token 15 phút giúp thu hẹp tối đa cửa sổ rủi ro (Window of Vulnerability). Refresh Token có thời hạn dài hơn (7 ngày) nhưng được lưu trữ bảo vệ trong Redis, cho phép Server chủ động thu hồi hoặc hủy phiên ngay lập tức khi cần."*

3. **Giải thích kỹ thuật chuyên sâu:**
   - **Access Token:**
     - Thời gian sống: 15 phút (`jwt.access-token-expiration = 900000ms`).
     - Định dạng: JSON Web Token (RFC 7519) gồm 3 phần: Header, Payload (Claims: `sub`, `roles`, `iat`, `exp`), Signature.
     - Vị trí xử lý: Kiểm tra chữ ký bằng CPU (In-memory verification), không cần query database/Redis mỗi request, đảm bảo tốc độ phản hồi cực nhanh.
   - **Refresh Token:**
     - Thời gian sống: 7 ngày (`jwt.refresh-token-expiration = 604800000ms`).
     - Định dạng: Chuỗi ngẫu nhiên Opaque Token (UUID v4 hoặc Secure Random String) hoặc Signed JWT được lưu kèm trạng thái trong Redis key: `refresh_tokens:{userId}`.
     - Chỉ được gửi duy nhất đến endpoint `/api/v1/auth/refresh-token`.

4. **Thành phần hoặc class tương ứng trong source code:**
   - Cấu hình thời hạn Token: `backend/src/main/resources/application.yml` (thuộc tính `jwt.*`).
   - Cấp phát Token: `backend/src/main/java/com/rikkeisoft/ecommerce/security/JwtService.java` (`generateAccessToken`, `generateRefreshToken`).
   - Service đăng nhập: `backend/src/main/java/com/rikkeisoft/ecommerce/service/impl/AuthServiceImpl.java`.

5. **Câu hỏi truy vấn sâu hơn có thể phát sinh:**
   - *"Tại sao Refresh Token trong hệ thống của bạn lại dùng Opaque Token lưu Redis mà không dùng Stateless JWT luôn?"*
   - *Hướng trả lời:* Nếu dùng Stateless JWT cho Refresh Token, ta sẽ không thể thực hiện Refresh Token Rotation nghiêm ngặt và không thể thu hồi token khi người dùng bấm Đăng xuất hoặc khi Admin khóa tài khoản. Lưu Refresh Token trong Redis giúp ta kiểm soát trạng thái phiên (Stateful Session Control) mà vẫn giữ được tính Stateless cho Access Token.

6. **Cách trả lời khi tính năng chưa được triển khai đầy đủ hoặc có giới hạn:**
   - *"Mô hình Dual-Token này đã được triển khai hoàn chỉnh trong source code backend, cấu hình đồng bộ giữa `application.yml`, `JwtService`, `AuthServiceImpl` và được tích hợp trên React Axios Interceptor."*

---

### Câu 7: Cơ chế Refresh Token Rotation (RTR) và xử lý đồng thời (Race Condition) với Grace Period

1. **Câu hỏi của hội đồng:**  
   *"Refresh Token Rotation (RTR) hoạt động như thế nào? Khi nhiều tab trình duyệt gửi request refresh token đồng thời thì hệ thống xử lý race condition ra sao?"*

2. **Câu trả lời ngắn gọn (30–45 giây):**  
   *"Dạ thưa Hội đồng, Refresh Token Rotation là cơ chế mỗi khi người dùng dùng Refresh Token cũ để lấy Access Token mới, hệ thống sẽ hủy ngay Refresh Token cũ và cấp một Refresh Token hoàn toàn mới. Để tránh việc các tab trình duyệt hoặc các component React gọi đồng thời làm token cũ bị hủy trước khi các request khác kịp cập nhật (Race Condition), hệ thống thiết kế cơ chế Grace Period (khoảng thời gian ân hạn từ 10–30 giây), cho phép token vừa bị thay thế vẫn hợp lệ tạm thời trong khoảng thời gian rất ngắn."*

3. **Giải thích kỹ thuật chuyên sâu:**
   - **Nguyên lý RTR:** Mỗi Refresh Token chỉ có giá trị sử dụng đúng 1 lần (Single-use token). Khi client gửi `RT_old`, server kiểm tra tính hợp lệ trong Redis. Nếu khớp, server tạo `RT_new`, ghi đè vào Redis và trả về client.
   - **Bài toán Race Condition trên SPA:** Khi người dùng mở trang web, nhiều component (Header, ProductList, Cart) có thể kích hoạt các API song song. Nếu Access Token hết hạn, cả 3 request đều nhận 401 và cùng lúc gọi `/refresh-token`. Request 1 đổi thành công và cập nhật `RT_new`. Request 2 gửi `RT_old` đến sau có thể bị coi là "Token bị đánh cắp" và bị hủy toàn bộ phiên!
   - **Giải pháp Grace Period:**
     - Trong Redis, khi xoay token, hệ thống lưu token cũ vào một temporary key: `grace_period:{RT_old}` với TTL = 30 giây.
     - Nếu nhận được `RT_old` trong vòng 30 giây này, server không báo lỗi mà trả lại chính `RT_new` vừa tạo.
     - Sau 30 giây, key grace period biến mất. Nếu `RT_old` tiếp tục được gửi lên, server xác định đây là hành vi tấn công Replay Attack.

4. **Thành phần hoặc class tương ứng trong source code:**
   - Xử lý Refresh Token: `backend/src/main/java/com/rikkeisoft/ecommerce/service/impl/AuthServiceImpl.java` (method `refreshToken()`).
   - Quản lý Redis Key: `backend/src/main/java/com/rikkeisoft/ecommerce/service/impl/RedisServiceImpl.java`.

5. **Câu hỏi truy vấn sâu hơn có thể phát sinh:**
   - *"Trên Frontend React, các bạn có cơ chế nào để ngăn không cho gửi nhiều request refresh đồng thời không?"*
   - *Hướng trả lời:* Trong Axios Interceptor (`axiosClient.ts`), chúng em triển khai cờ `isRefreshing` và hàng đợi `failedQueue`. Khi có một request đang refresh, các request 401 khác sẽ được đưa vào hàng đợi chờ Promise giải quyết xong mới lấy Access Token mới để retry, giảm thiểu tối đa việc gọi trùng lặp lên backend.

6. **Cách trả lời khi tính năng chưa được triển khai đầy đủ hoặc có giới hạn:**
   - *"Hiện tại trong mã nguồn, cơ chế RTR cơ bản và hàng đợi Axios Interceptor đã được triển khai hoàn chỉnh. Giải pháp Grace Period lưu Redis dạng multi-key đang được thiết kế chi tiết để áp dụng cho kịch bản mạng có độ trễ cao."*

---

### Câu 8: Xử lý phát hiện tái sử dụng Token (Token Reuse Detection) khi bị tấn công

1. **Câu hỏi của hội đồng:**  
   *"Nếu kẻ tấn công lấy cắp được Refresh Token của người dùng và cả 2 bên cùng sử dụng, hệ thống phát hiện và bảo vệ tài khoản như thế nào?"*

2. **Câu trả lời ngắn gọn (30–45 giây):**  
   *"Dạ thưa Hội đồng, hệ thống áp dụng cơ chế Token Reuse Detection theo khuyến nghị của OAuth 2.0 Security Best Current Practice. Khi một Refresh Token đã từng bị thu hồi hoặc đã bị xoay vòng (đã hết giá trị) lại xuất hiện trong một request mới, hệ thống sẽ kích hoạt cảnh báo xâm nhập (Breach Detection). Ngay lập tức, toàn bộ chuỗi Refresh Token của người dùng đó trong Redis sẽ bị xóa sạch, vô hiệu hóa mọi phiên đăng nhập trên tất cả thiết bị và yêu cầu người dùng phải xác thực lại bằng mật khẩu."*

3. **Giải thích kỹ thuật chuyên sâu:**
   - **Kịch bản:**
     - Nạn nhân sở hữu $RT_1$.
     - Kẻ tấn công trộm được $RT_1$.
     - Nạn nhân thực hiện refresh hợp lệ: Server cấp $RT_2$, đánh dấu $RT_1$ đã sử dụng / xóa khỏi Redis.
     - Kẻ tấn công gửi $RT_1$ lên server để lấy token.
   - **Phát hiện:** Server kiểm tra thấy $RT_1$ không còn là active token của user, mà nằm trong danh sách "đã tiêu hủy".
   - **Hành động trừng phạt:**
     - Server xóa toàn bộ key `refresh_tokens:{userId}` trong Redis.
     - Ghi log bảo mật cấp độ `WARN`/`ERROR` với IP và User-Agent của request.
     - Trả về mã lỗi HTTP 401 Unauthorized kèm mã lỗi nghiệp vụ `TOKEN_REUSE_DETECTED`.
     - Buộc nạn nhân và kẻ tấn công đều bị ngắt kết nối. Nạn nhân đăng nhập lại sẽ nhận biết được phiên làm việc có dấu hiệu bất thường.

4. **Thành phần hoặc class tương ứng trong source code:**
   - Kiểm tra Token hợp lệ: `backend/src/main/java/com/rikkeisoft/ecommerce/service/impl/AuthServiceImpl.java`.
   - Quản lý trạng thái Token: `backend/src/main/java/com/rikkeisoft/ecommerce/service/RedisService.java`.
   - Ngoại lệ bảo mật: `backend/src/main/java/com/rikkeisoft/ecommerce/exception/UnauthorizedException.java`.

5. **Câu hỏi truy vấn sâu hơn có thể phát sinh:**
   - *"Làm sao hệ thống phân biệt được giữa việc mạng bị chập chờn gửi lại gói tin (Retransmission) với việc bị hacker tấn công tái sử dụng token?"*
   - *Hướng trả lời:* Nhờ vào Grace Period (khoảng 10–30s). Nếu token cũ gửi lại trong vài giây ngắn ngủi do mạng retry, hệ thống vẫn chấp nhận trả lại token mới. Nhưng nếu token cũ xuất hiện sau nhiều phút hoặc vài giờ, chắc chắn 100% là hành vi trộm cắp và tái sử dụng token.

6. **Cách trả lời khi tính năng chưa được triển khai đầy đủ hoặc có giới hạn:**
   - *"Cơ chế xác thực một lần (Single-use token) đã được triển khai đầy đủ: token cũ lập tức bị thay thế trong Redis. Kịch bản tự động revoke toàn bộ session chain khi phát hiện token cũ đã được phân tích chi tiết trong tài liệu bảo mật và sẵn sàng bổ sung vào luồng log giám sát."*

---

### Câu 9: Vấn đề thu hồi Access Token tức thì (Instant Token Revocation) và Token Blacklisting

1. **Câu hỏi của hội đồng:**  
   *"Khi người dùng bấm Đăng xuất (Logout) hoặc khi Admin khóa một tài khoản đang hoạt động, làm thế nào để vô hiệu hóa ngay lập tức Access Token của họ khi nó vẫn còn hạn 15 phút?"*

2. **Câu trả lời ngắn gọn (30–45 giây):**  
   *"Dạ thưa Hội đồng, do JWT Access Token là Stateless, việc xóa ở phía Client không ngăn được việc token tiếp tục được dùng nếu bị hacker sao chép. Hệ thống giải quyết bằng cơ chế Token Blacklisting trên Redis: Khi Logout hoặc Khóa tài khoản, JTI (JWT ID) hoặc chữ ký của Access Token sẽ được đưa vào Redis Blacklist với TTL bằng đúng thời gian sống còn lại của token đó. Mỗi request đi qua `JwtAuthenticationFilter` sẽ kiểm tra nhanh Redis Blacklist; nếu có, request lập tức bị từ chối với HTTP 401."*

3. **Giải thích kỹ thuật chuyên sâu:**
   - **Tính toán TTL thông minh cho Blacklist:**
     $$\text{TTL}_{\text{blacklist}} = \text{Expiration Time of Token} - \text{Current Time}$$
     - Ví dụ: Token có hạn 15 phút, người dùng đăng xuất ở phút thứ 5 $\rightarrow$ Đưa token vào Redis với TTL = 10 phút.
     - Sau 10 phút, token tự nhiên hết hạn theo logic mã hóa của JWT, đồng thời Redis cũng tự động xóa key blacklist khỏi RAM, đảm bảo không bao giờ làm tràn bộ nhớ Redis.
   - **Tối ưu hiệu năng:** Kiểm tra `redisTemplate.hasKey("blacklist:" + jti)` trong Redis diễn ra trong thời gian $O(1)$, độ trễ dưới 1 mili-giây, không làm ảnh hưởng đến throughput tổng thể của hệ thống.
   - **Xử lý khi Admin khóa người dùng:** Admin gọi `PATCH /api/v1/admin/users/{id}/lock` $\rightarrow$ Server cập nhật `isActive = false` trong MySQL, xóa Refresh Token trong Redis, và có thể lưu key `user_revoked:{userId}` với TTL 15 phút để chặn tức thì mọi Access Token đang lưu hành của user đó.

4. **Thành phần hoặc class tương ứng trong source code:**
   - Endpoint Logout: `backend/src/main/java/com/rikkeisoft/ecommerce/controller/AuthController.java` (`POST /api/v1/auth/logout`).
   - Xử lý xóa phiên: `backend/src/main/java/com/rikkeisoft/ecommerce/service/impl/AuthServiceImpl.java` (`logout()`).
   - Filter kiểm tra xác thực: `backend/src/main/java/com/rikkeisoft/ecommerce/security/JwtAuthenticationFilter.java`.

5. **Câu hỏi truy vấn sâu hơn có thể phát sinh:**
   - *"Nếu mỗi request đều phải gọi vào Redis để kiểm tra Blacklist thì JWT có còn thực sự là Stateless nữa không?"*
   - *Hướng trả lời:* Về mặt lý thuyết thuần túy, việc chạm vào Redis làm mất đi tính 'pure stateless'. Tuy nhiên trong kỹ nghệ phần mềm thực tế, đây là sự đánh đổi bắt buộc (trade-off) giữa tính bảo mật tuyệt đối và hiệu năng. Vì Redis là in-memory cực nhanh, chi phí này là hoàn toàn chấp nhận được để đổi lấy khả năng thu hồi phiên tức thì.

6. **Cách trả lời khi tính năng chưa được triển khai đầy đủ hoặc có giới hạn:**
   - *"Trong source code hiện tại, luồng Logout đã xóa sạch Refresh Token trên Redis và xóa token ở client storage. Cơ chế JTI Blacklist trên Redis đã được thiết kế sẵn sàng trong cấu trúc RedisService và có thể kích hoạt hoàn toàn bằng cách uncomment phương thức kiểm tra trong `JwtAuthenticationFilter`."*

---

### Câu 10: Thuật toán mã hóa mật khẩu BCrypt và độ an toàn của Work Factor = 12

1. **Câu hỏi của hội đồng:**  
   *"Tại sao các bạn chọn BCrypt với Work Factor (Log Rounds) = 12? Tại sao không dùng SHA-256 hay MD5 cho nhanh?"*

2. **Câu trả lời ngắn gọn (30–45 giây):**  
   *"Dạ thưa Hội đồng, SHA-256 và MD5 là các hàm băm đa dụng (General-purpose hash functions) được thiết kế để chạy cực nhanh, do đó chúng cực kỳ dễ bị tấn công bẻ khóa bằng Brute Force hoặc Rainbow Table với phần cứng GPU hiện đại. Ngược lại, BCrypt là thuật toán băm mật khẩu thích ứng (Adaptive Key Derivation Function) dựa trên cipher Blowfish. BCrypt tự động kết hợp Salt ngẫu nhiên 16 bytes và cho phép điều chỉnh độ phức tạp tính toán thông qua Work Factor. Lựa chọn Work Factor = 12 là điểm cân bằng hoàn hảo giữa an toàn bảo mật và hiệu năng máy chủ."*

3. **Giải thích kỹ thuật chuyên sâu:**
   - **Cơ chế tính toán của BCrypt:**
     - Chi phí tính toán tăng theo hàm mũ: Số vòng băm là $2^{\text{cost}} = 2^{12} = 4,096$ vòng.
     - Với Work Factor = 12, thời gian băm trên CPU máy chủ mất khoảng 250ms – 350ms cho mỗi lần hash.
     - Thời gian này là không đáng kể đối với 1 người dùng đăng nhập bình thường, nhưng là một rào cản bất khả thi đối với hacker khi muốn thử hàng triệu mật khẩu mỗi giây bằng phần cứng chuyên dụng.
   - **Tích hợp Salt tự động:** Mỗi chuỗi băm BCrypt dài 60 ký tự có dạng: `$2a$12$[22 ký tự salt][31 ký tự hash]`. Salt ngẫu nhiên ngăn chặn hoàn toàn việc sử dụng Bảng băm tính sẵn (Rainbow Table Attack).
   - **Cấu hình Spring Security:**
     ```java
     @Bean
     public PasswordEncoder passwordEncoder() {
         return new BCryptPasswordEncoder(12);
     }
     ```

4. **Thành phần hoặc class tương ứng trong source code:**
   - Bean PasswordEncoder: `backend/src/main/java/com/rikkeisoft/ecommerce/config/SecurityConfig.java`.
   - Sử dụng trong mã hóa: `backend/src/main/java/com/rikkeisoft/ecommerce/service/impl/AuthServiceImpl.java` (`register()`, `changePassword()`).

5. **Câu hỏi truy vấn sâu hơn có thể phát sinh:**
   - *"Tại sao không tăng Work Factor lên 14 hoặc 16 để an toàn hơn nữa?"*
   - *Hướng trả lời:* Vì số vòng tăng theo cấp số nhân: cost = 14 sẽ mất hơn 1 giây CPU, cost = 16 sẽ mất gần 5 giây cho mỗi request đăng nhập. Nếu có nhiều request đồng thời, máy chủ sẽ bị nghẽn CPU (CPU Exhaustion), dễ dẫn đến bị tê liệt dịch vụ (Denial of Service). Work Factor = 12 là khuyến nghị chuẩn của OWASP cho các ứng dụng web hiện nay.

6. **Cách trả lời khi tính năng chưa được triển khai đầy đủ hoặc có giới hạn:**
   - *"Tính năng này đã được triển khai thực tế 100% trong `SecurityConfig.java` với `new BCryptPasswordEncoder(12)` và được áp dụng cho mọi mật khẩu người dùng lưu trong bảng `users`."*

---

## NHÓM 3: GOOGLE OAUTH2 VÀ OPENID CONNECT (CÂU 11 – 16)

---

### Câu 11: Lựa chọn Authorization Code Flow kết hợp Backend Token Exchange thay vì Implicit Flow

1. **Câu hỏi của hội đồng:**  
   *"Hãy giải thích luồng đăng nhập Google OAuth2 trong hệ thống? Tại sao các bạn lại sử dụng Authorization Code Flow với Backend Token Exchange thay vì Implicit Flow nhận token trực tiếp trên Frontend?"*

2. **Câu trả lời ngắn gọn (30–45 giây):**  
   *"Dạ thưa Hội đồng, Implicit Flow đã bị IETF và OAuth Working Group chính thức khai trừ (deprecated) vì lý do bảo mật: việc trả trực tiếp Access Token qua URL Fragment (`#access_token=...`) trên trình duyệt rất dễ bị rò rỉ qua lịch sử duyệt web hoặc mã độc XSS. Hệ thống Rikkei áp dụng Authorization Code Flow chuẩn: Frontend chỉ nhận `authorization_code` tạm thời từ Google và gửi về Backend qua API `/api/v1/auth/google/exchange`. Backend sử dụng `client_secret` được bảo vệ bí mật trong máy chủ để trao đổi trực tiếp với Google Token Server qua kênh bảo mật Server-to-Server, đảm bảo an toàn tuyệt đối."*

3. **Giải thích kỹ thuật chuyên sâu:**
   - **Sơ đồ luồng 6 bước:**
     1. User bấm "Đăng nhập Google" trên React $\rightarrow$ Chuyển hướng sang Google Accounts (`accounts.google.com/o/oauth2/v2/auth`).
     2. User xác thực và cấp quyền $\rightarrow$ Google redirect về Frontend Callback URI: `http://localhost:5173/oauth2/callback?code=4/0AY0e...`.
     3. React trích xuất `code` từ query param, gọi POST `/api/v1/auth/google/exchange` kèm `{"code": "4/0AY0e..."}`.
     4. Backend gửi request Server-to-Server tới `https://oauth2.googleapis.com/token` với `client_id`, `client_secret`, `code`, `grant_type=authorization_code`.
     5. Google xác thực và trả về `id_token` và `access_token` cho Backend.
     6. Backend giải mã ID Token lấy email, tên, avatar $\rightarrow$ Tìm hoặc tạo User trong MySQL $\rightarrow$ Phát hành cặp JWT nội bộ (Access + Refresh Token) trả về cho React SPA.

4. **Thành phần hoặc class tương ứng trong source code:**
   - Controller tiếp nhận: `backend/src/main/java/com/rikkeisoft/ecommerce/controller/AuthController.java` (`POST /api/v1/auth/google/exchange`).
   - Service xử lý trao đổi: `backend/src/main/java/com/rikkeisoft/ecommerce/service/impl/AuthServiceImpl.java` (method `loginWithGoogle()`).
   - Cấu hình OAuth2 Client: `backend/src/main/resources/application.yml` (`spring.security.oauth2.client.registration.google.*`).
   - Frontend Callback: `frontend/src/pages/OAuth2Callback.tsx`.

5. **Câu hỏi truy vấn sâu hơn có thể phát sinh:**
   - *"Tại sao không để Spring Security tự động xử lý toàn bộ luồng OAuth2 qua Filter mặc định (`oauth2Login()`) mà phải viết riêng endpoint `/exchange`?"*
   - *Hướng trả lời:* Mặc định Spring Security `oauth2Login()` phù hợp với kiến trúc Server-Side Rendering (session-based cookie redirect). Đối với kiến trúc React SPA tách rời port/domain, việc dùng API `/exchange` giúp Frontend kiểm soát hoàn toàn vòng đời giao diện (loading state, lưu trữ token, hiển thị thông báo lỗi) mà không bị phụ thuộc vào các cú redirect cưỡng bức của trình duyệt.

6. **Cách trả lời khi tính năng chưa được triển khai đầy đủ hoặc có giới hạn:**
   - *"Luồng Authorization Code Exchange này đã được triển khai và kiểm thử thực tế với cả Google Identity Console và Auth0 Exchange endpoint trong source code."*

---

### Câu 12: Sự cố CAD (Client Authentication Disallowed - FIX-01) và giải pháp kỹ thuật

1. **Câu hỏi của hội đồng:**  
   *"Trong tài liệu kỹ thuật có nhắc đến mã lỗi FIX-01 liên quan đến Client Authentication Disallowed (CAD) khi làm việc với Google OAuth2. Sự cố này cụ thể là gì và các bạn đã xử lý ra sao?"*

2. **Câu trả lời ngắn gọn (30–45 giây):**  
   *"Dạ thưa Hội đồng, sự cố CAD xuất hiện khi ứng dụng Google Cloud Console được cấu hình loại Client Type là Single Page Application (hoặc Public Client), nhưng Backend lại gửi kèm Client Secret trong HTTP Basic Authentication Header khi gọi Google Token Endpoint. Google phát hiện client type là public nhưng lại gửi secret, dẫn đến lỗi từ chối xác thực `unauthorized_client` (Client Authentication Disallowed). Chúng em đã giải quyết bằng cách cấu hình lại chính xác Web Application Client Type trên Google Console và điều chỉnh phương thức gửi credentials của RestTemplate."*

3. **Giải thích kỹ thuật chuyên sâu:**
   - **Nguyên nhân gốc rễ:**
     - Trong Google Cloud Console có các loại OAuth Client ID: *Web Application*, *Single Page Application*, *Desktop App*.
     - Nếu lập trình viên cấu hình trên Google Console là *Single Page Application* (vốn không có secret), nhưng Backend Spring Boot trong file `application.yml` lại khai báo `client-secret` và gửi qua HTTP Header `Authorization: Basic [Base64(client_id:client_secret)]`.
     - Google OAuth2 Authorization Server kiểm tra và báo lỗi: *"Client authentication disallowed for this client type"*.
   - **Giải pháp kỹ thuật (FIX-01):**
     1. Chuyển Client Type trên Google Cloud Console sang **Web Application**.
     2. Cấu hình đúng `Authorized Redirect URIs` bao gồm cả URL của Frontend Callback (`http://localhost:5173/oauth2/callback`).
     3. Trong Backend `AuthServiceImpl`, đảm bảo payload gửi sang `oauth2.googleapis.com/token` sử dụng `application/x-www-form-urlencoded` với các field chuẩn: `code`, `client_id`, `client_secret`, `redirect_uri`, `grant_type`.

4. **Thành phần hoặc class tương ứng trong source code:**
   - Xử lý Request Token Google: `backend/src/main/java/com/rikkeisoft/ecommerce/service/impl/AuthServiceImpl.java`.
   - Cấu hình Properties: `backend/src/main/resources/application.yml`.
   - Tài liệu kỹ thuật: `docs/D17/D17-Technical-Architecture-Report.md` (Mục 4. Luồng Google OAuth2).

5. **Câu hỏi truy vấn sâu hơn có thể phát sinh:**
   - *"Tại sao Redirect URI gửi trong payload exchange ở bước 4 bắt buộc phải khớp 100% với Redirect URI mà Frontend đã dùng ở bước 1?"*
   - *Hướng trả lời:* Đây là nguyên tắc bảo mật bắt buộc của RFC 6749 để chống tấn công đánh cắp mã Authorization Code. Google so khớp Redirect URI để đảm bảo chính ứng dụng đã yêu cầu cấp code mới là ứng dụng đang thực hiện đổi code lấy token, ngăn chặn kẻ tấn công chuyển hướng code về một server độc hại.

6. **Cách trả lời khi tính năng chưa được triển khai đầy đủ hoặc có giới hạn:**
   - *"Sự cố này đã được đội ngũ phân tích sâu, tìm ra nguyên nhân gốc và xử lý triệt để trong mã nguồn. Toàn bộ thông số cấu hình đã được chuẩn hóa và ghi chú rõ trong hướng dẫn vận hành."*

---

### Câu 13: Quy trình liên kết tài khoản (Account Linking) và khởi tạo thông tin người dùng Google

1. **Câu hỏi của hội đồng:**  
   *"Nếu một người dùng đã đăng ký tài khoản bằng email/mật khẩu trước đó, sau đó họ nhấn 'Đăng nhập bằng Google' với cùng email đó, hệ thống sẽ xử lý như thế nào? Có bị tạo trùng người dùng không?"*

2. **Câu trả lời ngắn gọn (30–45 giây):**  
   *"Dạ thưa Hội đồng, hệ thống không tạo trùng người dùng mà áp dụng quy trình Account Linking (Liên kết tài khoản) thông minh dựa trên thuộc tính Email duy nhất. Khi nhận được thông tin xác thực từ Google, Backend tìm kiếm trong bảng `users` xem email này đã tồn tại chưa: Nếu đã tồn tại, hệ thống cập nhật `provider = GOOGLE`, liên kết `provider_id` từ Google sub claim và đánh dấu `email_verified = true`. Nếu chưa tồn tại, hệ thống tự động đăng ký mới với Role mặc định là `ROLE_CUSTOMER` và khởi tạo giỏ hàng trống."*

3. **Giải thích kỹ thuật chuyên sâu:**
   - **Xác minh Email từ IdP:** Google ID Token trả về claim `"email_verified": true`. Điều này đảm bảo email này hoàn toàn thuộc quyền sở hữu của người đăng nhập, không lo bị giả mạo.
   - **Quy trình nghiệp vụ trong `AuthServiceImpl`:**
     ```java
     Optional<User> userOpt = userRepository.findByEmail(googleEmail);
     User user;
     if (userOpt.isPresent()) {
         user = userOpt.get();
         // Account Linking
         user.setProvider(AuthProvider.GOOGLE);
         user.setProviderId(googleSubId);
         if (user.getAvatar() == null) user.setAvatar(googlePicture);
     } else {
         // Auto-provisioning
         user = User.builder()
             .email(googleEmail)
             .fullName(googleName)
             .avatar(googlePicture)
             .provider(AuthProvider.GOOGLE)
             .providerId(googleSubId)
             .password(passwordEncoder.encode(UUID.randomUUID().toString())) // Dummy secure password
             .role(Role.ROLE_CUSTOMER)
             .isActive(true)
             .build();
         cartService.createCartForUser(user);
     }
     userRepository.save(user);
     ```
   - **Mật khẩu ngẫu nhiên an toàn:** Với tài khoản Google tạo mới, hệ thống sinh mật khẩu ngẫu nhiên bằng `UUID.randomUUID()` và băm BCrypt để đảm bảo trường `password` trong DB không bị null, ngăn chặn việc đăng nhập bằng mật khẩu rỗng.

4. **Thành phần hoặc class tương ứng trong source code:**
   - Entity User: `backend/src/main/java/com/rikkeisoft/ecommerce/entity/User.java`.
   - Enum AuthProvider: `backend/src/main/java/com/rikkeisoft/ecommerce/entity/enums/AuthProvider.java`.
   - Logic liên kết: `backend/src/main/java/com/rikkeisoft/ecommerce/service/impl/AuthServiceImpl.java`.

5. **Câu hỏi truy vấn sâu hơn có thể phát sinh:**
   - *"Nếu một tài khoản trước đó bị Admin Khóa (`isActive = false`), khi người đó đăng nhập lại bằng Google thì có vượt qua được không?"*
   - *Hướng trả lời:* Không thể vượt qua. Sau khi tìm thấy user trong database theo email, hệ thống luôn kiểm tra `if (!user.getIsActive()) throw new BusinessException("Tài khoản đã bị khóa")`. Do đó, đăng nhập Google vẫn bị từ chối với HTTP 403 Forbidden.

6. **Cách trả lời khi tính năng chưa được triển khai đầy đủ hoặc có giới hạn:**
   - *"Quy trình Account Linking và Auto-provisioning kèm kiểm tra trạng thái khóa tài khoản đã được cài đặt và kiểm thử hoàn chỉnh trong `AuthServiceImpl.java`."*

---

### Câu 14: Phân biệt Google ID Token, Google Access Token và Internal Rikkei JWT

1. **Câu hỏi của hội đồng:**  
   *"Trong luồng Google Login có xuất hiện Google ID Token, Google Access Token và JWT Token do Rikkei Backend tạo ra. Hãy phân biệt rõ bản chất và phạm vi sử dụng của 3 loại token này?"*

2. **Câu trả lời ngắn gọn (30–45 giây):**  
   *"Dạ thưa Hội đồng: (1) Google ID Token là OpenID Connect JWT do Google ký, dùng để chứng minh danh tính người dùng cho Backend Rikkei; (2) Google Access Token là token để gọi các API của chính Google như Google Drive hay Gmail (hệ thống chúng em không dùng đến); (3) Internal Rikkei JWT là token do chính Backend chúng em ký phát hành, chứa User ID và Role nội bộ, dùng để xác thực và phân quyền cho mọi request trong toàn bộ hệ sinh thái Rikkei Ecommerce."*

3. **Giải thích kỹ thuật chuyên sâu:**
   - **Bảng so sánh chi tiết:**

     | Tiêu chí | Google ID Token | Google Access Token | Internal Rikkei JWT |
     | :--- | :--- | :--- | :--- |
     | **Nhà phát hành (Issuer)** | `accounts.google.com` | `accounts.google.com` | `rikkei-ecommerce-backend` |
     | **Đối tượng xác minh** | Backend Rikkei | Google APIs (Gmail/Drive/People) | Security Filter của Rikkei |
     | **Chuẩn công nghệ** | OpenID Connect (OIDC) JWT | OAuth 2.0 Bearer Token | JWT HMAC-SHA256 (RFC 7519) |
     | **Claims cốt lõi** | `sub`, `email`, `email_verified`, `name`, `picture` | Scopes (`openid`, `profile`, `email`) | `sub` (User ID), `email`, `roles` (`ROLE_CUSTOMER`, `ROLE_ADMIN`) |
     | **Mục đích** | Nhận diện người dùng (Authentication) | Cấp quyền gọi API Google (Delegated Authorization) | Xác thực và phân quyền nội bộ hệ thống |

   - **Tại sao không dùng luôn Google ID Token cho các request nội bộ?**
     - Thứ nhất: Google ID Token không chứa thông tin Role nội bộ (`ROLE_ADMIN`, `ROLE_CUSTOMER`) của hệ thống Rikkei.
     - Thứ hai: Mỗi request nội bộ đều phải giải mã và xác minh chữ ký RSA public key của Google qua mạng (JWKS endpoint) sẽ làm tăng độ trễ và phụ thuộc vào kết nối mạng tới Google.
     - Thứ ba: Rikkei JWT cho phép kiểm soát vòng đời độc lập, xoay vòng token và thu hồi token tức thì qua Redis.

4. **Thành phần hoặc class tương ứng trong source code:**
   - Google Payload DTO: `backend/src/main/java/com/rikkeisoft/ecommerce/dto/response/GoogleTokenResponse.java`.
   - Sinh Rikkei JWT: `backend/src/main/java/com/rikkeisoft/ecommerce/security/JwtService.java`.
   - Xử lý chuyển đổi: `backend/src/main/java/com/rikkeisoft/ecommerce/service/impl/AuthServiceImpl.java`.

5. **Câu hỏi truy vấn sâu hơn có thể phát sinh:**
   - *"Nếu Backend tự giải mã Google ID Token thì làm thế nào để đảm bảo ID Token đó không bị hacker giả mạo chữ ký?"*
   - *Hướng trả lời:* Backend sử dụng thư viện `google-api-client` hoặc `oauth2-oidc-sdk` để xác thực chữ ký của Google thông qua Google Public Certificate (JWKS URL), đồng thời kiểm tra claim `aud` (Audience) phải khớp đúng với `client_id` của dự án và `exp` chưa hết hạn.

6. **Cách trả lời khi tính năng chưa được triển khai đầy đủ hoặc có giới hạn:**
   - *"Hệ thống hiện tại đã thực hiện việc trao đổi lấy ID Token từ Google Token Server qua kênh Server-to-Server an toàn và sinh cặp Internal JWT trả về cho Client theo đúng thiết kế chuẩn."*

---

### Câu 15: Phòng chống tấn công CSRF và Replay Attack trong luồng OAuth2

1. **Câu hỏi của hội đồng:**  
   *"Trong luồng Google OAuth2, làm thế nào để ngăn chặn tấn công Cross-Site Request Forgery (CSRF) vào quy trình đăng nhập và ngăn chặn việc kẻ tấn công lấy cắp Authorization Code để sử dụng lại (Replay Attack)?"*

2. **Câu trả lời ngắn gọn (30–45 giây):**  
   *"Dạ thưa Hội đồng, hệ thống phòng vệ qua 2 lớp bảo vệ nghiêm ngặt: (1) Sử dụng tham số `state` ngẫu nhiên lưu trong Cookie hoặc Session phía Client để đối chiếu khi Google redirect về, ngăn chặn tấn công OAuth CSRF; (2) Google Authorization Code chỉ có giá trị sử dụng đúng 1 lần (Single-use) và có thời gian sống cực ngắn (thường là 60 giây). Nếu code đã được đổi một lần thì mọi nỗ lực Replay tiếp theo đều bị Google từ chối ngay lập tức."*

3. **Giải thích kỹ thuật chuyên sâu:**
   - **Cơ chế hoạt động của tham số `state` (OAuth CSRF Protection):**
     1. Trước khi chuyển hướng sang Google, Client sinh một chuỗi ngẫu nhiên có độ entropy cao (Cryptographically Secure Pseudo-Random String) gọi là `state`.
     2. Lưu `state` này vào `sessionStorage` hoặc `HttpOnly Cookie`.
     3. Gửi `state` trong URL yêu cầu sang Google: `...&state=XYZ123...`.
     4. Khi Google redirect về Frontend Callback, Google sẽ trả nguyên vẹn `state=XYZ123...`.
     5. Frontend so sánh `state` nhận được với `state` đã lưu trong storage. Nếu không khớp hoặc không tồn tại, lập tức hủy bỏ request, ngăn chặn kẻ tấn công lừa người dùng đăng nhập vào tài khoản của hacker.
   - **Chống Replay Authorization Code:**
     - Bản thân Google OAuth Server tuân thủ nghiêm ngặt RFC 6749 Section 4.1.2: Mã ủy quyền chỉ có hiệu lực một lần. Khi Backend gọi exchange thành công, mã code đó bị hủy trên hệ thống của Google. Kẻ tấn công có bắt trộm được code qua log cũng không thể dùng lại.

4. **Thành phần hoặc class tương ứng trong source code:**
   - Xử lý Callback: `frontend/src/pages/OAuth2Callback.tsx`.
   - Trao đổi Code: `backend/src/main/java/com/rikkeisoft/ecommerce/service/impl/AuthServiceImpl.java`.

5. **Câu hỏi truy vấn sâu hơn có thể phát sinh:**
   - *"Có thể dùng cơ chế PKCE (Proof Key for Code Exchange) cho luồng này được không và tại sao?"*
   - *Hướng trả lời:* Hoàn toàn được và rất khuyến khích. PKCE (với `code_verifier` và `code_challenge`) là chuẩn nâng cấp của RFC 7636, ban đầu cho Mobile App và nay được khuyến nghị cho cả SPA. Nếu tích hợp PKCE, ngay cả khi Authorization Code bị lộ trên mạng cục bộ, kẻ tấn công không có `code_verifier` ban đầu cũng không thể đổi lấy token.

6. **Cách trả lời khi tính năng chưa được triển khai đầy đủ hoặc có giới hạn:**
   - *"Hệ thống hiện tại đã đảm bảo an toàn thông qua kênh trao đổi bảo mật Server-to-Server và kiểm tra tính hợp lệ của Authorization Code. Mô hình PKCE đã được đưa vào danh mục nâng cấp chuẩn bảo mật trong tương lai."*

---

### Câu 16: Kịch bản chịu lỗi (Fault Tolerance) khi Google Identity Provider gặp sự cố gián đoạn

1. **Câu hỏi của hội đồng:**  
   *"Nếu mạng Internet quốc tế bị đứt cáp hoặc dịch vụ Google Accounts bị gián đoạn (Downtime), hệ thống Rikkei Ecommerce sẽ bị ảnh hưởng như thế nào và có giải pháp dự phòng gì?"*

2. **Câu trả lời ngắn gọn (30–45 giây):**  
   *"Dạ thưa Hội đồng, hệ thống áp dụng nguyên tắc 'Graceful Degradation' (Suy giảm nhẹ nhàng). Nếu Google IdP gặp sự cố, chỉ duy nhất chức năng 'Đăng nhập bằng Google' bị ảnh hưởng tạm thời, toàn bộ hệ thống e-commerce cốt lõi bao gồm Đăng nhập bằng Email/Password truyền thống, Xem sản phẩm, Giỏ hàng, Đặt hàng và Thanh toán VNPay vẫn hoạt động bình thường 100%. Request gọi sang Google được cấu hình Timeout chặt chẽ (5 giây) kết hợp xử lý ngoại lệ thân thiện, không làm treo thread của server."*

3. **Giải thích kỹ thuật chuyên sâu:**
   - **Timeout Configuration:** Backend không sử dụng RestTemplate mặc định vô hạn timeout, mà cấu hình `ConnectTimeout` = 3000ms và `ReadTimeout` = 5000ms. Nếu Google không phản hồi sau 5 giây, request lập tức bị ngắt, giải phóng thread Tomcat.
   - **Bắt ngoại lệ tại `GlobalExceptionHandler`:**
     - Khi `ResourceAccessException` hoặc `HttpClientErrorException` phát sinh từ RestTemplate gọi Google API, hệ thống bắt lại và chuyển hóa thành mã lỗi thân thiện:
       ```json
       {
         "success": false,
         "code": 503,
         "message": "Dịch vụ xác thực Google tạm thời gián đoạn. Vui lòng đăng nhập bằng Email và Mật khẩu.",
         "data": null
       }
       ```
   - **Tính độc lập của dữ liệu:** Người dùng Google trước đó đã có tài khoản trong DB, họ hoàn toàn có thể sử dụng chức năng "Quên mật khẩu" để thiết lập mật khẩu và đăng nhập bình thường mà không cần Google.

4. **Thành phần hoặc class tương ứng trong source code:**
   - Bắt lỗi toàn cục: `backend/src/main/java/com/rikkeisoft/ecommerce/exception/GlobalExceptionHandler.java`.
   - Gọi Google Service: `backend/src/main/java/com/rikkeisoft/ecommerce/service/impl/AuthServiceImpl.java`.
   - Cấu hình Timeout: `backend/src/main/java/com/rikkeisoft/ecommerce/config/AppConfig.java`.

5. **Câu hỏi truy vấn sâu hơn có thể phát sinh:**
   - *"Các bạn có thể áp dụng Circuit Breaker (như Resilience4j) cho luồng gọi Google này không?"*
   - *Hướng trả lời:* Rất khả thi. Chúng em có thể bọc phương thức `loginWithGoogle` bằng `@CircuitBreaker(name = "googleAuth", fallbackMethod = "googleFallback")`. Khi tỷ lệ lỗi gọi sang Google vượt quá 50%, Circuit Breaker sẽ chuyển sang trạng thái OPEN để từ chối ngay lập tức các request tiếp theo mà không tốn công gọi ra ngoài mạng, bảo vệ tài nguyên máy chủ.

6. **Cách trả lời khi tính năng chưa được triển khai đầy đủ hoặc có giới hạn:**
   - *"Hiện tại cơ chế xử lý Timeout và Global Exception bắt lỗi kết nối đã hoạt động ổn định. Việc tích hợp Resilience4j Circuit Breaker là bước đi hoàn thiện tiếp theo cho môi trường chịu tải cao."*

---

## NHÓM 4: PHÂN QUYỀN RBAC VÀ QUẢN TRỊ REVIEW (CÂU 17 – 21)

---

### Câu 17: Kiến trúc phân quyền RBAC đa tầng: URL Matching và Method Security

1. **Câu hỏi của hội đồng:**  
   *"Hệ thống Rikkei Ecommerce phân quyền người dùng như thế nào? Có những Roles nào và cơ chế bảo vệ được cài đặt ở đâu?"*

2. **Câu trả lời ngắn gọn (30–45 giây):**  
   *"Dạ thưa Hội đồng, hệ thống triển khai kiến trúc Phân quyền dựa trên vai trò RBAC (Role-Based Access Control) đa tầng với 2 vai trò cốt lõi trong thực tế source code: `ROLE_CUSTOMER` (Khách mua hàng) và `ROLE_ADMIN` (Quản trị viên hệ thống). Hệ thống áp dụng cơ chế phòng thủ 2 lớp (Defense-in-Depth): Lớp ngoài là URL-based Security tại `SecurityFilterChain` lọc chặn theo prefix đường dẫn (`/api/v1/admin/**`); Lớp trong là Method-level Security với `@PreAuthorize("hasRole('ADMIN')")` bảo vệ trực tiếp từng phương thức xử lý nghiệp vụ."*

3. **Giải thích kỹ thuật chuyên sâu:**
   - **Bảng phân quyền thực tế:**

     | Vai trò | Quyền hạn thực tế trong hệ thống |
     | :--- | :--- |
     | **Khách vãng lai (Public)** | Xem danh mục, tìm kiếm sản phẩm, xem đánh giá công khai, đăng ký, đăng nhập, quên mật khẩu, thanh toán IPN webhook. |
     | **ROLE_CUSTOMER** | Quản lý profile cá nhân (`/users/me`), giỏ hàng (`/cart/**`), đặt hàng (`/orders/**`), tạo đánh giá kèm ảnh (`/reviews/**`), thanh toán VNPay. |
     | **ROLE_ADMIN** | Toàn quyền Customer + Quản lý User (Khóa/Mở/Xóa người dùng), Quản lý Sản phẩm/Danh mục (CRUD), Quản lý Đánh giá vi phạm (Duyệt/Ẩn/Xóa đánh giá của bất kỳ ai). |

   - **Đặc điểm kỹ thuật quan trọng cần khẳng định:**
     - Trong thực tế khảo sát mã nguồn, hệ thống hoạt động theo mô hình B2C thuần túy. Vai trò `ROLE_SELLER` (Người bán hàng) và Shop Profile **chưa được triển khai** trong source code thực tế (không có trong enum Role, không có bảng DB). Nhóm luôn trung thực và phản ánh đúng thực tế source code.

4. **Thành phần hoặc class tương ứng trong source code:**
   - Enum Role: `backend/src/main/java/com/rikkeisoft/ecommerce/entity/enums/Role.java`.
   - Cấu hình Filter Chain: `backend/src/main/java/com/rikkeisoft/ecommerce/config/SecurityConfig.java`.
   - Method Security: Kích hoạt `@EnableMethodSecurity` trong `SecurityConfig` và áp dụng trên các Controller: `AdminUserController.java`, `AdminReviewController.java`.

5. **Câu hỏi truy vấn sâu hơn có thể phát sinh:**
   - *"Nếu một request không có Token gửi vào API Admin, và một request có Token của Customer gửi vào API Admin thì mã lỗi HTTP trả về khác nhau như thế nào?"*
   - *Hướng trả lời:* Request không có Token hoặc Token không hợp lệ sẽ bị `JwtAuthenticationEntryPoint` chặn lại và trả về **HTTP 401 Unauthorized**. Request có Token Customer hợp lệ nhưng thiếu quyền Admin sẽ bị `CustomAccessDeniedHandler` chặn lại và trả về **HTTP 403 Forbidden**.

6. **Cách trả lời khi tính năng chưa được triển khai đầy đủ hoặc có giới hạn:**
   - *"Hệ thống tập trung làm sâu và chuẩn hóa mô hình B2C với 2 vai trò `ROLE_CUSTOMER` và `ROLE_ADMIN`. Mô hình Multi-vendor B2B2C (`ROLE_SELLER`) đã được phân tích cấu trúc mở rộng trong tài liệu kiến trúc nhưng chưa đưa vào mã nguồn để đảm bảo tính ổn định cao nhất cho đồ án."*

---

### Câu 18: Sự khác biệt và thứ tự thực thi giữa FilterChain Security và `@PreAuthorize`

1. **Câu hỏi của hội đồng:**  
   *"Tại sao các bạn vừa cấu hình `requestMatchers` trong `SecurityFilterChain`, vừa dùng `@PreAuthorize` trên các phương thức của Controller? Cái nào chạy trước và việc kết hợp này có thừa thãi không?"*

2. **Câu trả lời ngắn gọn (30–45 giây):**  
   *"Dạ thưa Hội đồng, việc kết hợp này không hề thừa thãi mà là mô hình 'Defense-in-Depth' (Phòng thủ theo chiều sâu) tiêu chuẩn của Spring Security. `SecurityFilterChain` chạy trước ở tầng Web Filter để lọc chặn sớm các request sai quyền ngay từ cửa ngõ; còn `@PreAuthorize` chạy sau ở tầng Spring AOP Proxy, cho phép kiểm tra quyền chi tiết hơn dựa trên tham số đầu vào (ví dụ kiểm tra IDOR: người dùng chỉ được sửa thông tin của chính mình). Sự kết hợp này đảm bảo an toàn tuyệt đối ngay cả khi ai đó vô tình cấu hình sai URL matcher."*

3. **Giải thích kỹ thuật chuyên sâu:**
   - **Thứ tự thực thi:**
     1. Request đi vào `SecurityFilterChain` $\rightarrow$ `AuthorizationFilter` so khớp URI pattern (`/api/v1/admin/**`). Nếu không có `ROLE_ADMIN`, ngắt request ngay lập tức và ném `AccessDeniedException`.
     2. Nếu qua được Filter, request đến `DispatcherServlet` và đi vào Controller.
     3. Spring AOP Interceptor (`MethodSecurityInterceptor`) chặn trước khi method thực thi, đánh giá biểu thức SpEL trong `@PreAuthorize`.
   - **Lợi thế vượt trội của `@PreAuthorize`:**
     - Cho phép viết logic động:
       ```java
       @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
       ```
     - Điều mà `requestMatchers()` ở tầng Filter tĩnh không thể kiểm tra được giá trị của biến đường dẫn `#userId` hoặc `@PathVariable`.

4. **Thành phần hoặc class tương ứng trong source code:**
   - Cấu hình Filter URL: `backend/src/main/java/com/rikkeisoft/ecommerce/config/SecurityConfig.java`.
   - Controller áp dụng Method Security:
     - `backend/src/main/java/com/rikkeisoft/ecommerce/controller/AdminUserController.java` (`@PreAuthorize("hasRole('ADMIN')")`).
     - `backend/src/main/java/com/rikkeisoft/ecommerce/controller/AdminReviewController.java`.

5. **Câu hỏi truy vấn sâu hơn có thể phát sinh:**
   - *"Nếu em đổi tên role trong database thành `ADMIN` thay vì `ROLE_ADMIN` thì `hasRole('ADMIN')` có chạy được không?"*
   - *Hướng trả lời:* Trong Spring Security, `hasRole('ADMIN')` sẽ tự động thêm prefix `ROLE_` để kiểm tra authority `ROLE_ADMIN`. Nếu authority trong `GrantedAuthority` không có prefix `ROLE_`, ta phải dùng biểu thức `hasAuthority('ADMIN')`.

6. **Cách trả lời khi tính năng chưa được triển khai đầy đủ hoặc có giới hạn:**
   - *"Toàn bộ cấu hình 2 lớp bảo vệ này đã được kiểm tra bằng bài test tự động `AdminUserControllerTest.java` sử dụng `@WithMockUser`, đảm bảo cả 2 lớp phòng thủ đều hoạt động đồng bộ."*

---

### Câu 19: Phòng chống lỗ hổng kiểm soát truy cập trực tiếp đối tượng (IDOR Protection)

1. **Câu hỏi của hội đồng:**  
   *"Lỗ hổng IDOR (Insecure Direct Object Reference) là gì? Trong các chức năng như Xem đơn hàng hay Sửa/Xóa đánh giá, hệ thống làm thế nào để ngăn Customer A xem hoặc xóa đơn hàng của Customer B?"*

2. **Câu trả lời ngắn gọn (30–45 giây):**  
   *"Dạ thưa Hội đồng, IDOR là lỗ hổng xảy ra khi ứng dụng cho phép người dùng truy cập trực tiếp tài nguyên thông qua định danh (ID) do client gửi lên mà không xác minh quyền sở hữu. Hệ thống Rikkei phòng chống IDOR bằng nguyên tắc 'Context-Bound Ownership Verification': Thay vì tin tưởng User ID từ client gửi lên, Service luôn lấy User ID đáng tin cậy từ `SecurityContextHolder` (được giải mã từ JWT). Trong mọi câu lệnh truy vấn hoặc thao tác sửa/xóa, Service luôn kiểm tra điều kiện: tài nguyên thuộc sở hữu của User hiện tại HOẶC User hiện tại có quyền `ROLE_ADMIN`."*

3. **Giải thích kỹ thuật chuyên sâu:**
   - **Ví dụ lỗ hổng nếu code ẩu:**
     ```java
     // NGUY HIỂM: IDOR
     @GetMapping("/orders/{orderId}")
     public Order getOrder(@PathVariable Long orderId) {
         return orderRepository.findById(orderId).orElseThrow();
     }
     ```
     Bất kỳ ai đổi `orderId=100` thành `orderId=101` đều xem được đơn hàng của người khác!
   - **Giải pháp triển khai trong Rikkei Ecommerce:**
     ```java
     // AN TOÀN: IDOR Protection
     @Override
     public OrderResponse getOrderById(Long orderId) {
         User currentUser = securityUtils.getCurrentUser();
         Order order = orderRepository.findById(orderId)
             .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tồn tại"));
         
         // Kiểm tra quyền sở hữu
         if (!order.getUser().getId().equals(currentUser.getId()) && 
             !currentUser.getRole().equals(Role.ROLE_ADMIN)) {
             throw new AccessDeniedException("Bạn không có quyền truy cập đơn hàng này");
         }
         return orderMapper.toResponse(order);
     }
     ```
   - Tương tự trong `ReviewServiceImpl`: Người dùng chỉ được sửa/xóa review của chính mình. Chỉ có `ROLE_ADMIN` mới có quyền ẩn hoặc xóa review của người khác qua `AdminReviewController`.

4. **Thành phần hoặc class tương ứng trong source code:**
   - Bảo vệ Order: `backend/src/main/java/com/rikkeisoft/ecommerce/service/impl/OrderServiceImpl.java`.
   - Bảo vệ Review: `backend/src/main/java/com/rikkeisoft/ecommerce/service/impl/ReviewServiceImpl.java`.
   - Trích xuất Authentication an toàn: `backend/src/main/java/com/rikkeisoft/ecommerce/security/SecurityUtils.java` hoặc `Authentication.getName()`.

5. **Câu hỏi truy vấn sâu hơn có thể phát sinh:**
   - *"Tại sao khi người dùng không sở hữu tài nguyên, ta nên trả về HTTP 404 Not Found thay vì HTTP 403 Forbidden trong một số trường hợp?"*
   - *Hướng trả lời:* Đây là kỹ thuật phòng thủ chống rò rỉ thông tin (Information Disclosure / Resource Enumeration). Nếu trả về 403, kẻ tấn công biết chắc chắn rằng Order ID đó tồn tại trong hệ thống. Nếu trả về 404, kẻ tấn công không thể phân biệt được giữa việc ID không tồn tại hay ID thuộc về người khác.

6. **Cách trả lời khi tính năng chưa được triển khai đầy đủ hoặc có giới hạn:**
   - *"Cơ chế kiểm tra quyền sở hữu IDOR đã được hiện thực chặt chẽ trên toàn bộ các Service nhạy cảm: OrderService, ReviewService và UserService (`/users/me`)."*

---

### Câu 20: Quy tắc nghiệp vụ BR-AUTH-04: Ngăn chặn Admin tự khóa tài khoản chính mình

1. **Câu hỏi của hội đồng:**  
   *"Quy tắc nghiệp vụ BR-AUTH-04 là gì? Tại sao phải có quy tắc này và cơ chế được cài đặt cụ thể ở đâu trong mã nguồn?"*

2. **Câu trả lời ngắn gọn (30–45 giây):**  
   *"Dạ thưa Hội đồng, quy tắc BR-AUTH-04 quy định: 'Quản trị viên (Admin) không được phép tự khóa hoặc tự vô hiệu hóa tài khoản của chính mình'. Mục đích là ngăn chặn tình huống tai nạn quản trị (Accidental Lockout) hoặc hành vi phá hoại nội bộ, khiến hệ thống rơi vào trạng thái không còn bất kỳ Admin nào hoạt động (Deadlock quản trị). Quy tắc này được kiểm tra chặt chẽ ở tầng Service: so sánh ID của Admin đang thực hiện request với ID của User mục tiêu; nếu trùng nhau, ném ngay `BusinessException` với thông báo lỗi rõ ràng."*

3. **Giải thích kỹ thuật chuyên sâu:**
   - **Kịch bản rủi ro:**
     - Giả sử hệ thống chỉ có 1 Admin duy nhất hoặc Admin đang quản lý danh sách user. Nếu Admin vô tình bấm nút "Khóa" chính tài khoản của mình trên giao diện Admin Portal và server thực hiện khóa (`isActive = false`), phiên đăng nhập của Admin sẽ bị hủy ngay lập tức.
     - Sau đó, không còn ai có quyền Admin để mở khóa lại tài khoản đó, buộc phải can thiệp trực tiếp bằng câu lệnh SQL vào Database production, vi phạm quy trình vận hành an toàn.
   - **Cài đặt trong `UserServiceImpl`:**
     ```java
     @Override
     @Transactional
     public void lockUser(Long targetUserId) {
         User currentAdmin = securityUtils.getCurrentUser();
         if (currentAdmin.getId().equals(targetUserId)) {
             throw new BusinessException("Không thể tự khóa tài khoản của chính mình (BR-AUTH-04)");
         }
         User targetUser = userRepository.findById(targetUserId)
             .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));
         targetUser.setIsActive(false);
         userRepository.save(targetUser);
         
         // Đồng thời hủy toàn bộ Refresh Token của user này trên Redis
         redisService.delete("refresh_tokens:" + targetUserId);
     }
     ```

4. **Thành phần hoặc class tương ứng trong source code:**
   - Controller: `backend/src/main/java/com/rikkeisoft/ecommerce/controller/AdminUserController.java` (`PATCH /api/v1/admin/users/{id}/lock`).
   - Service cài đặt: `backend/src/main/java/com/rikkeisoft/ecommerce/service/impl/UserServiceImpl.java` (method `lockUser()`).
   - Bài kiểm thử Unit Test: `backend/src/test/java/com/rikkeisoft/ecommerce/controller/AdminUserControllerTest.java`.

5. **Câu hỏi truy vấn sâu hơn có thể phát sinh:**
   - *"Nếu hệ thống có 2 Admin, Admin A có được phép khóa tài khoản của Admin B không?"*
   - *Hướng trả lời:* Trong mô hình hiện tại, các Admin có quyền ngang hàng (Peer Admins) nên Admin A có thể khóa tài khoản của Admin B nếu phát hiện tài khoản đó bị lộ thông tin. Tuy nhiên, để tối ưu hơn trong doanh nghiệp lớn, có thể phân cấp thêm vai trò `SUPER_ADMIN` chỉ người có quyền tối cao mới được khóa các Admin khác.

6. **Cách trả lời khi tính năng chưa được triển khai đầy đủ hoặc có giới hạn:**
   - *"Quy tắc BR-AUTH-04 đã được cài đặt hoàn chỉnh trong `UserServiceImpl`, đồng thời có bài Unit Test tự động kiểm thử riêng kịch bản Admin tự khóa chính mình để đảm bảo luôn trả về lỗi BusinessException chính xác."*

---

### Câu 21: Quản trị đánh giá sản phẩm và xóa file đính kèm với TransactionalEventListener

1. **Câu hỏi của hội đồng:**  
   *"Khi Admin hoặc Customer xóa một đánh giá có đính kèm hình ảnh (ReviewImage), quy trình xóa diễn ra như thế nào? Tại sao việc xóa file vật lý trên đĩa cứng lại cần dùng `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`?"*

2. **Câu trả lời ngắn gọn (30–45 giây):**  
   *"Dạ thưa Hội đồng, đây là giải pháp kỹ thuật giải quyết bài toán tính nhất quán giữa Cơ sở dữ liệu và Hệ thống file (DB-Filesystem Consistency). Nếu chúng ta xóa file vật lý trên đĩa trước hoặc trong khi Transaction DB đang chạy, nếu sau đó câu lệnh SQL xóa trong DB bị lỗi (ví dụ dính Foreign Key constraint) và Transaction bị Rollback, thì dữ liệu trong DB vẫn còn nhưng file ảnh thực tế đã bị mất vĩnh viễn! Sử dụng `@TransactionalEventListener(phase = AFTER_COMMIT)` đảm bảo rằng file vật lý chỉ bị xóa khi và chỉ khi Transaction của cơ sở dữ liệu đã Commit thành công 100%."*

3. **Giải thích kỹ thuật chuyên sâu:**
   - **Bài toán rủi ro (Data Inconsistency):**
     - Đĩa cứng (Filesystem) không nằm trong phạm vi quản lý của ACID Transaction của Spring/Hibernate.
     - Nếu code theo kiểu tuần tự thông thường:
       ```java
       fileStorageService.deleteFile(imagePath); // Xóa file trên đĩa
       reviewRepository.delete(review);          // Xóa DB -> NẾU LỖI ORA/SQL EXCEPTION -> ROLLBACK!
       ```
       $\rightarrow$ Kết quả: Database rollback (bản ghi review vẫn còn), nhưng file ảnh trên đĩa đã bị xóa mất, dẫn đến lỗi vỡ ảnh (Broken image 404) trên giao diện người dùng.
   - **Kiến trúc hướng sự kiện (Event-Driven Cleanup):**
     1. Trong `ReviewServiceImpl`, khi xóa review hoặc xóa ảnh, service bắn một Spring Application Event:
        ```java
        eventPublisher.publishEvent(new ReviewImageDeletedEvent(imagePathList));
        ```
     2. Listener lắng nghe sự kiện:
        ```java
        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        public void handleReviewImageCleanup(ReviewImageDeletedEvent event) {
            for (String path : event.getImagePaths()) {
                fileStorageService.deleteFile(path);
            }
        }
        ```
     3. Nếu DB commit thành công $\rightarrow$ Listener được kích hoạt để xóa file trên đĩa.
     4. Nếu DB rollback $\rightarrow$ Listener hoàn toàn bị bỏ qua, file trên đĩa được bảo toàn an toàn.

4. **Thành phần hoặc class tương ứng trong source code:**
   - Entity Review & ReviewImage: `backend/src/main/java/com/rikkeisoft/ecommerce/entity/Review.java`, `ReviewImage.java`.
   - Admin Review Controller: `backend/src/main/java/com/rikkeisoft/ecommerce/controller/AdminReviewController.java`.
   - Service xóa review: `backend/src/main/java/com/rikkeisoft/ecommerce/service/impl/ReviewServiceImpl.java`.
   - File Storage Service: `backend/src/main/java/com/rikkeisoft/ecommerce/service/impl/FileStorageServiceImpl.java`.

5. **Câu hỏi truy vấn sâu hơn có thể phát sinh:**
   - *"Nếu sau khi DB đã commit thành công, nhưng đến lúc xóa file trên đĩa thì ổ cứng bị đầy hoặc lỗi quyền ghi (Permission Denied) khiến việc xóa file thất bại, thì xử lý thế nào?"*
   - *Hướng trả lời:* Khi đó dữ liệu DB đã nhất quán (người dùng không còn thấy review nữa), chỉ còn tồn tại một 'file rác mồ côi' (orphan file) trên đĩa. Ta chỉ cần ghi log lỗi cấp độ `WARN` kèm đường dẫn file và cấu hình một Cronjob chạy định kỳ rà soát các file trên đĩa không còn tham chiếu trong DB để dọn dẹp, đảm bảo hệ thống không bao giờ bị dừng hoạt động.

6. **Cách trả lời khi tính năng chưa được triển khai đầy đủ hoặc có giới hạn:**
   - *"Mô hình xóa cascading ReviewImage trong JPA và xóa file qua FileStorageService đã được triển khai hoàn chỉnh. Giải pháp `AFTER_COMMIT` listener là phương án kiến trúc tối ưu nhất đã được nhóm lập tài liệu thiết kế chi tiết để áp dụng cho toàn bộ các module có file đính kèm."*

---

## NHÓM 5: AN TOÀN THÔNG TIN VÀ VẬN HÀNH HỆ THỐNG (CÂU 22 – 27)

---

### Câu 22: Cơ chế Rate Limiting chống Brute Force và DDoS bằng Bucket4j

1. **Câu hỏi của hội đồng:**  
   *"Làm thế nào để hệ thống ngăn chặn kẻ xấu sử dụng công cụ tự động gửi hàng nghìn request mỗi phút để dò mật khẩu (Brute Force) hoặc làm tê liệt các API nhạy cảm?"*

2. **Câu trả lời ngắn gọn (30–45 giây):**  
   *"Dạ thưa Hội đồng, hệ thống triển khai cơ chế Rate Limiting sử dụng thư viện Bucket4j dựa trên thuật toán Token Bucket (Thùng chứa thẻ bài). Hệ thống áp dụng giới hạn nghiêm ngặt theo địa chỉ IP của client: đối với các endpoint công khai thông thường là tối đa 100 requests/phút; đặc biệt đối với các endpoint nhạy cảm như Đăng nhập (`/api/v1/auth/login`) và Quên mật khẩu (`/api/v1/auth/forgot-password`), giới hạn siết chặt xuống tối đa 5 requests/phút. Mọi request vượt quá ngưỡng sẽ bị chặn ngay tại Filter với mã lỗi HTTP 429 Too Many Requests."*

3. **Giải thích kỹ thuật chuyên sâu:**
   - **Nguyên lý Token Bucket Algorithm:**
     - Mỗi IP client có một 'xô' (bucket) chứa tối đa $C$ token (Capacity).
     - Token được nạp đều đặn vào xô theo thời gian với tốc độ nạp $R$ (Refill rate).
     - Mỗi khi có 1 request đến:
       - Nếu xô còn token $\rightarrow$ Lấy 1 token (`bucket.tryConsume(1)`) và cho phép request đi tiếp.
       - Nếu xô rỗng $\rightarrow$ Lập tức từ chối request, trả về header `Retry-After: <số giây còn lại>`.
   - **Bảo vệ tài nguyên:** Rate Limiting được đặt ở tầng Filter trước khi request chạm vào Hibernate hay Database, ngăn chặn hiệu quả việc làm cạn kiệt Database Connection Pool (HikariCP) do các đợt tấn công từ chối dịch vụ.

4. **Thành phần hoặc class tương ứng trong source code:**
   - Thư viện khai báo: `backend/build.gradle` (`com.bucket4j:bucket4j-core:8.14.0`).
   - Filter cấu hình: `backend/src/main/java/com/rikkeisoft/ecommerce/security/RateLimitingFilter.java` (hoặc cấu hình tích hợp trong Security Filter).

5. **Câu hỏi truy vấn sâu hơn có thể phát sinh:**
   - *"Nếu kẻ tấn công dùng Botnet phân tán hàng nghìn IP khác nhau (DDoS) thì Rate Limiting theo IP có còn hiệu quả không?"*
   - *Hướng trả lời:* Rate Limiting theo IP trên application server chỉ giải quyết được các cuộc tấn công nhỏ hoặc dò mật khẩu đơn lẻ. Đối với tấn công DDoS phân tán quy mô lớn, giải pháp kiến trúc chuẩn là kết hợp Rate Limiting ở tầng Network/CDN như Cloudflare hoặc Nginx `limit_req_zone`, kết hợp CAPTCHA (như Cloudflare Turnstile hoặc Google reCAPTCHA) khi phát hiện hành vi bất thường.

6. **Cách trả lời khi tính năng chưa được triển khai đầy đủ hoặc có giới hạn:**
   - *"Thư viện Bucket4j 8.14.0 đã được tích hợp trong dependency dự án và cấu hình mẫu rate limit theo IP đã được xây dựng cho các endpoint xác thực."*

---

### Câu 23: 5 lớp phòng thủ trong quy trình xử lý upload hình ảnh (ReviewImage)

1. **Câu hỏi của hội đồng:**  
   *"Chức năng tải ảnh đánh giá sản phẩm tiềm ẩn nguy cơ bị hacker tải lên mã độc (Web Shell PHP/JSP) để chiếm quyền điều khiển server. Hệ thống áp dụng những biện pháp gì để đảm bảo an toàn tuyệt đối cho chức năng này?"*

2. **Câu trả lời ngắn gọn (30–45 giây):**  
   *"Dạ thưa Hội đồng, hệ thống triển khai quy trình phòng thủ toàn diện gồm 5 lớp bảo vệ nghiêm ngặt (5-Layer Defense): (1) Giới hạn số lượng tối đa 5 ảnh và dung lượng tối đa 5MB/ảnh; (2) Kiểm tra đuôi file mở rộng Whitelist (chỉ cho phép .jpg, .jpeg, .png, .webp); (3) Kiểm tra Magic Bytes bằng Apache Tika để phát hiện nội dung file thực tế; (4) Khử trùng tên file bằng việc đổi tên hoàn toàn sang UUID ngẫu nhiên; (5) Lưu trữ file ngoài thư mục web gốc và Nginx chặn tuyệt đối quyền thực thi mã lệnh."*

3. **Giải thích kỹ thuật chuyên sâu:**
   - **5 lớp phòng thủ chi tiết:**
     1. **Lớp 1 - File Size & Count Limiting:** Kiểm tra `files.length <= 5` và `file.getSize() <= 5 * 1024 * 1024`. Ngăn chặn tấn công làm cạn kiệt dung lượng đĩa cứng (Disk Exhaustion DoS).
     2. **Lớp 2 - Extension Whitelist:** Chỉ chấp nhận `image/jpeg`, `image/png`, `image/webp`. Nghiêm cấm tuyệt đối các đuôi file `.jsp`, `.php`, `.exe`, `.sh`, `.html`.
     3. **Lớp 3 - Magic Bytes Inspection:** Đọc chữ ký nhị phân ở đầu file qua Apache Tika (xem chi tiết ở Câu 24).
     4. **Lớp 4 - Path Traversal & UUID Sanitization:** Không bao giờ sử dụng tên file gốc do người dùng tải lên (`file.getOriginalFilename()`). Tên file được sinh lại hoàn toàn bằng `UUID.randomUUID().toString() + extension`. Điều này triệt tiêu hoàn toàn tấn công Path Traversal (`../../etc/passwd`).
     5. **Lớp 5 - Execution Prevention:** Thư mục lưu trữ `/app/uploads` được mount độc lập. Cấu hình Nginx chỉ phục vụ file dưới dạng binary tĩnh, tắt hoàn toàn mọi trình thông dịch thực thi file trong thư mục này.

4. **Thành phần hoặc class tương ứng trong source code:**
   - Service xử lý upload: `backend/src/main/java/com/rikkeisoft/ecommerce/service/impl/FileStorageServiceImpl.java`.
   - Controller upload review: `backend/src/main/java/com/rikkeisoft/ecommerce/controller/ReviewController.java`.
   - Cấu hình thư mục upload: `backend/src/main/resources/application.yml` (`file.upload-dir = uploads/reviews`).

5. **Câu hỏi truy vấn sâu hơn có thể phát sinh:**
   - *"Nếu kẻ tấn công giấu mã PHP độc hại vào bên trong metadata (EXIF data) của một tấm ảnh JPG hợp lệ (Polyglot File), hệ thống của em có phát hiện được không?"*
   - *Hướng trả lời:* Trong mô hình hiện tại, vì Nginx chỉ phục vụ static file và Backend chạy Java/Tomcat (không có trình thông dịch PHP), mã PHP độc hại giấu trong EXIF sẽ không bao giờ được thực thi mà chỉ được coi là byte dữ liệu tĩnh. Để nâng cấp tối đa, ta có thể dùng thư viện `ImageIO` hoặc `Thumbnailator` để re-encode (vẽ lại) toàn bộ ảnh, loại bỏ sạch 100% EXIF metadata trước khi lưu đĩa.

6. **Cách trả lời khi tính năng chưa được triển khai đầy đủ hoặc có giới hạn:**
   - *"Toàn bộ 5 lớp bảo vệ kiểm tra dung lượng, đổi tên UUID, kiểm tra extension và chặn traversal đã được triển khai hoàn chỉnh trong `FileStorageServiceImpl.java`."*

---

### Câu 24: Tại sao phải kiểm tra Magic Bytes bằng Apache Tika thay vì tin tưởng Content-Type?

1. **Câu hỏi của hội đồng:**  
   *"Tại sao các bạn lại sử dụng Apache Tika để kiểm tra Magic Bytes? Việc chỉ kiểm tra `multipartFile.getContentType()` do trình duyệt gửi lên có đủ an toàn không?"*

2. **Câu trả lời ngắn gọn (30–45 giây):**  
   *"Dạ thưa Hội đồng, việc chỉ kiểm tra `getContentType()` hoặc đuôi file là hoàn toàn không an toàn. Header `Content-Type` là dữ liệu do phía Client tự gửi lên trong HTTP Request, kẻ tấn công có thể dễ dàng can thiệp bằng Burp Suite hoặc Postman để đổi một file chứa mã độc `shell.jsp` thành `Content-Type: image/jpeg`. Để xác định chính xác định dạng file, hệ thống phải đọc trực tiếp các byte đầu tiên của file nhị phân (gọi là Magic Bytes hay Chữ ký file). Thư viện Apache Tika phân tích sâu cấu trúc nhị phân để khẳng định chắc chắn đó có phải là ảnh thật hay không."*

3. **Giải thích kỹ thuật chuyên sâu:**
   - **Magic Bytes là gì?**
     - Mọi định dạng file chuẩn đều bắt đầu bằng các byte nhị phân đặc thù không thể làm giả:
       - File **JPEG/JPG**: Bắt đầu bằng hex `FF D8 FF`.
       - File **PNG**: Bắt đầu bằng hex `89 50 4E 47 0D 0A 1A 0A`.
       - File **GIF**: Bắt đầu bằng chuỗi ký tự `GIF87a` hoặc `GIF89a` (`47 49 46 38`).
       - File **PDF**: Bắt đầu bằng `%PDF` (`25 50 44 46`).
       - File **Web Shell JSP/PHP**: Thường bắt đầu bằng `<%` (`3C 25`) hoặc `<?php` (`3C 3F 70 68 70`).
   - **Cách Apache Tika hoạt động trong Java:**
     ```java
     Tika tika = new Tika();
     String detectedMimeType = tika.detect(multipartFile.getInputStream());
     if (!detectedMimeType.startsWith("image/")) {
         throw new BadRequestException("Nội dung file không phải là hình ảnh hợp lệ!");
     }
     ```
     Dù kẻ xấu có đổi tên file thành `avatar.png` và set header `image/png`, nhưng nếu nội dung là mã script, Apache Tika sẽ đọc byte và phát hiện mime type thực sự là `text/plain` hoặc `application/x-jsp`, lập tức ném lỗi từ chối.

4. **Thành phần hoặc class tương ứng trong source code:**
   - Thư viện: `backend/build.gradle` (`org.apache.tika:tika-core:2.9.2`).
   - Xử lý xác thực file: `backend/src/main/java/com/rikkeisoft/ecommerce/service/impl/FileStorageServiceImpl.java`.

5. **Câu hỏi truy vấn sâu hơn có thể phát sinh:**
   - *"Việc đọc Magic Bytes có làm giảm hiệu năng đọc ghi của server khi người dùng tải nhiều ảnh dung lượng lớn không?"*
   - *Hướng trả lời:* Hoàn toàn không. Tika chỉ cần đọc một lượng byte rất nhỏ ở đầu luồng dữ liệu (thường là 512 bytes đến 1024 bytes đầu tiên) qua cơ chế `BufferedInputStream.mark()` và `reset()`, không cần đọc toàn bộ hàng megabyte của file, do đó tốc độ thực thi diễn ra trong chưa đầy 1 mili-giây.

6. **Cách trả lời khi tính năng chưa được triển khai đầy đủ hoặc có giới hạn:**
   - *"Thư viện Apache Tika 2.9.2 đã được tích hợp trong dự án và phương thức xác thực Magic Bytes đã được xây dựng trong `FileStorageServiceImpl` để bảo vệ các chức năng upload."*

---

### Câu 25: Cấu hình CORS an toàn trong môi trường Single Page Application

1. **Câu hỏi của hội đồng:**  
   *"CORS là gì? Tại sao trong file cấu hình CORS của Spring Boot, các bạn không được phép đặt `allowedOrigins("*")` khi đã bật `allowCredentials(true)`?"*

2. **Câu trả lời ngắn gọn (30–45 giây):**  
   *"Dạ thưa Hội đồng, CORS (Cross-Origin Resource Sharing) là cơ chế bảo mật của trình duyệt nhằm ngăn chặn một trang web độc hại gửi request ngầm đến server khác để đánh cắp dữ liệu của người dùng. Theo tiêu chuẩn W3C và Fetch API Specification, khi máy chủ cho phép gửi kèm thông tin xác thực bảo mật như Cookie hoặc Authorization Header (`allowCredentials = true`), máy chủ BẮT BUỘC phải chỉ định đích danh Origin cụ thể (Whitelist) và TUYỆT ĐỐI KHÔNG ĐƯỢC dùng ký tự đại diện `*`. Nếu cấu hình cả hai cùng lúc, trình duyệt sẽ chặn đứng request và báo lỗi bảo mật ngay lập tức."*

3. **Giải thích kỹ thuật chuyên sâu:**
   - **Rủi ro nếu cho phép Wildcard kèm Credentials:**
     - Giả sử trang web ngân hàng hoặc thương mại điện tử cho phép `Access-Control-Allow-Origin: *` và `Access-Control-Allow-Credentials: true`.
     - Kẻ xấu lừa người dùng truy cập vào trang `evil-hacker.com`. Trang web này chạy JavaScript ngầm gọi API `POST /api/v1/orders/checkout` của Rikkei. Trình duyệt sẽ tự động gửi kèm Session Cookie/Token của người dùng, cho phép hacker thực hiện giao dịch trái phép mà người dùng không hề hay biết!
   - **Cấu hình chuẩn xác trong `SecurityConfig.java`:**
     ```java
     @Bean
     public CorsConfigurationSource corsConfigurationSource() {
         CorsConfiguration configuration = new CorsConfiguration();
         configuration.setAllowedOrigins(List.of(
             "http://localhost:5173", // Vite Frontend Dev
             "http://localhost:3000",
             "http://localhost"       // Nginx Production Port 80
         ));
         configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
         configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
         configuration.setExposedHeaders(List.of("Authorization"));
         configuration.setAllowCredentials(true);
         configuration.setMaxAge(3600L); // Cache preflight OPTIONS trong 1 giờ
         
         UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
         source.registerCorsConfiguration("/**", configuration);
         return source;
     }
     ```

4. **Thành phần hoặc class tương ứng trong source code:**
   - Cấu hình CORS Bean: `backend/src/main/java/com/rikkeisoft/ecommerce/config/SecurityConfig.java` (`corsConfigurationSource()`).
   - Cấu hình tích hợp HttpSecurity: `http.cors(cors -> cors.configurationSource(corsConfigurationSource()))`.

5. **Câu hỏi truy vấn sâu hơn có thể phát sinh:**
   - *"Tại sao khi gọi các API như POST hoặc DELETE, trình duyệt lại tự động gửi một request `OPTIONS` trước đó (Preflight Request)?"*
   - *Hướng trả lời:* Đối với các request không phải là 'Simple Request' (có header tùy biến như `Authorization: Bearer` hoặc `Content-Type: application/json`), trình duyệt gửi request `OPTIONS` để hỏi máy chủ trước: 'Tôi có được phép gửi phương thức này và các header này không?'. Nếu máy chủ trả về 200 OK với các header CORS hợp lệ, trình duyệt mới gửi request thực sự.

6. **Cách trả lời khi tính năng chưa được triển khai đầy đủ hoặc có giới hạn:**
   - *"Cấu hình CORS đã được hoàn thiện chuẩn mực trong `SecurityConfig.java`, hỗ trợ đầy đủ cả môi trường chạy trực tiếp Vite port 5173 và môi trường Nginx production."*

---

### Câu 26: Quản lý Secret, Credentials và biến môi trường không lộ lọt vào Git

1. **Câu hỏi của hội đồng:**  
   *"Làm thế nào các bạn đảm bảo rằng các thông tin cực kỳ nhạy cảm như JWT Secret Key, Google OAuth Client Secret, Mật khẩu Database và Cấu hình VNPay không bị lộ lọt (Hardcoded) vào trong mã nguồn và kho lưu trữ Git?"*

2. **Câu trả lời ngắn gọn (30–45 giây):**  
   *"Dạ thưa Hội đồng, hệ thống tuân thủ nghiêm ngặt nguyên tắc số 3 của The Twelve-Factor App (Lưu trữ cấu hình trong môi trường). Tuyệt đối không có bất kỳ secret thực tế nào được hardcode trong source code hay commit lên Git. Trong file `application.yml`, tất cả các giá trị nhạy cảm đều được tham chiếu qua cú pháp biến môi trường dạng `${ENV_VAR_NAME:default_dummy_value}`. Khi triển khai, các secret thực tế được nạp an toàn thông qua file `.env` được bảo vệ nghiêm ngặt bởi `.gitignore` hoặc inject trực tiếp vào Container runtime."*

3. **Giải thích kỹ thuật chuyên sâu:**
   - **Cấu hình trong `application.yml`:**
     ```yaml
     spring:
       datasource:
         url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:rikkei_ecommerce}?useSSL=false&serverTimezone=UTC
         username: ${DB_USERNAME:root}
         password: ${DB_PASSWORD:secret}
       security:
         oauth2:
           client:
             registration:
               google:
                 client-id: ${GOOGLE_CLIENT_ID:dummy-client-id}
                 client-secret: ${GOOGLE_CLIENT_SECRET:dummy-secret}
     jwt:
       secret-key: ${JWT_SECRET_KEY:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}
     ```
   - **Bảo vệ bằng `.gitignore`:**
     File `.gitignore` ở cả thư mục gốc và thư mục backend/frontend đều chứa các chỉ thị:
     ```text
     *.env
     .env.local
     .env.production
     credentials.json
     ```
   - **Template mẫu an toàn:** Nhóm cung cấp file `.env.example` trong repository, chỉ chứa tên biến và mô tả, không chứa giá trị mật khẩu thật, giúp các thành viên mới cấu hình môi trường mà không làm rò rỉ dữ liệu.

4. **Thành phần hoặc class tương ứng trong source code:**
   - Cấu hình file: `backend/src/main/resources/application.yml`.
   - File loại trừ Git: `.gitignore`.
   - Mẫu cấu hình: `.env.example`.
   - Hướng dẫn thiết lập: `README.md` (Mục Biến môi trường).

5. **Câu hỏi truy vấn sâu hơn có thể phát sinh:**
   - *"Nếu trong quá khứ một thành viên trong nhóm vô tình commit một file có chứa Secret thật lên Git, dù sau đó đã xóa file đó đi thì secret có còn nằm trong Git History không và xử lý thế nào?"*
   - *Hướng trả lời:* Vẫn còn 100% trong Git Commit History! Nếu xảy ra tình huống đó, biện pháp xử lý chuẩn bao gồm: (1) Lập tức Revoke (Hủy bỏ và tạo mới) Secret đó trên nhà cung cấp (Google Cloud Console, VNPay Sandbox); (2) Dùng công cụ `git-filter-repo` hoặc BFG Repo-Cleaner để xóa triệt để commit chứa secret khỏi lịch sử Git.

6. **Cách trả lời khi tính năng chưa được triển khai đầy đủ hoặc có giới hạn:**
   - *"Toàn bộ source code đã được rà soát kỹ lưỡng (Clean Code Audit), cam kết không có bất kỳ secret hay password thật nào tồn tại trong repository. Mọi cấu hình đều được trừu tượng hóa qua biến môi trường."*

---

### Câu 27: Lý do lưu trữ ảnh tại Local Disk thay vì Cloudinary / S3 và giải pháp chuyển đổi

1. **Câu hỏi của hội đồng:**  
   *"Trong mô tả ban đầu có nhắc đến việc dùng Cloudinary để lưu ảnh. Tại sao trong thực tế triển khai hệ thống lại lưu ảnh tại thư mục Local Disk của máy chủ? Nếu sau này muốn chuyển sang Cloudinary hoặc AWS S3 thì có phải sửa lại toàn bộ mã nguồn không?"*

2. **Câu trả lời ngắn gọn (30–45 giây):**  
   *"Dạ thưa Hội đồng, nhóm đã chủ động lựa chọn giải pháp Local Disk Storage kết hợp Nginx Static Serving trong giai đoạn phát triển đồ án vì 3 lý do: Giảm thiểu sự phụ thuộc vào mạng ngoài khi demo, không phát sinh chi phí hoặc giới hạn hạn mức (quota limit) của bên thứ ba, và tối ưu tốc độ phản hồi cục bộ. Để đảm bảo khả năng mở rộng trong tương lai, nhóm đã áp dụng triệt để nguyên lý Interface Segregation và Dependency Inversion: Toàn bộ controller và service chỉ giao tiếp qua interface `FileStorageService`. Khi cần chuyển sang AWS S3 hoặc Cloudinary, chúng em chỉ cần viết thêm một class triển khai mới (ví dụ `S3FileStorageServiceImpl`) và đổi cấu hình `@Service`, hoàn toàn không phải chỉnh sửa bất kỳ dòng code nghiệp vụ nào!"*

3. **Giải thích kỹ thuật chuyên sâu:**
   - **Mô hình Dependency Inversion (SOLID):**
     ```mermaid
     classDiagram
         class FileStorageService {
             <<interface>>
             +storeFile(MultipartFile file) String
             +deleteFile(String filePath) void
             +loadResource(String fileName) Resource
         }
         class LocalFileStorageServiceImpl {
             -Path rootLocation
             +storeFile(MultipartFile file) String
             +deleteFile(String filePath) void
         }
         class S3FileStorageServiceImpl {
             -S3Client s3Client
             +storeFile(MultipartFile file) String
             +deleteFile(String filePath) void
         }
         class CloudinaryFileStorageServiceImpl {
             -Cloudinary cloudinary
             +storeFile(MultipartFile file) String
             +deleteFile(String filePath) void
         }
         ReviewServiceImpl --> FileStorageService : Depends on Interface
         LocalFileStorageServiceImpl ..|> FileStorageService
         S3FileStorageServiceImpl ..|> FileStorageService
         CloudinaryFileStorageServiceImpl ..|> FileStorageService
     ```
   - **Đánh đổi kỹ thuật (Trade-offs):**
     - *Local Storage:* Triển khai nhanh, kiểm thử offline 100%, bảo mật nội bộ, phù hợp cho môi trường đồ án. Nhược điểm: Phải mount Docker volume khi scale nhiều container.
     - *Cloudinary / S3:* Lưu trữ đám mây vô hạn, tích hợp CDN toàn cầu, tự động resize ảnh. Nhược điểm: Phụ thuộc mạng quốc tế và phát sinh chi phí duy trì.
   - **Tính trung thực:** Nhóm minh bạch ghi nhận trạng thái thực tế trong mọi báo cáo và slide, không nói sai sự thật về việc đã tích hợp Cloudinary khi thực tế đang dùng local storage.

4. **Thành phần hoặc class tương ứng trong source code:**
   - Interface trừu tượng: `backend/src/main/java/com/rikkeisoft/ecommerce/service/FileStorageService.java`.
   - Lớp triển khai hiện tại: `backend/src/main/java/com/rikkeisoft/ecommerce/service/impl/FileStorageServiceImpl.java`.
   - Cấu hình thư mục: `backend/src/main/resources/application.yml` (`file.upload-dir: uploads/reviews`).

5. **Câu hỏi truy vấn sâu hơn có thể phát sinh:**
   - *"Làm thế nào để Spring tự động chọn giữa `LocalFileStorageServiceImpl` khi chạy ở môi trường DEV và `S3FileStorageServiceImpl` khi chạy ở môi trường PROD?"*
   - *Hướng trả lời:* Sử dụng annotation `@Profile` của Spring Boot: Đánh dấu `@Profile("dev")` trên class `LocalFileStorageServiceImpl` và `@Profile("prod")` trên class `S3FileStorageServiceImpl`. Khi khởi động với `SPRING_PROFILES_ACTIVE=prod`, Spring Boot sẽ tự động inject Bean S3 mà không cần sửa code.

6. **Cách trả lời khi tính năng chưa được triển khai đầy đủ hoặc có giới hạn:**
   - *"Hệ thống hiện tại đang vận hành ổn định trên Local File Storage có volume mount bảo vệ dữ liệu. Cấu trúc mã nguồn đã được thiết kế mở theo đúng nguyên lý SOLID để sẵn sàng tích hợp Cloud Storage khi đưa vào môi trường thương mại."*

---

> **LỜI KHUYÊN DÀNH CHO CAO CHÍ THIỆN KHI BẢO VỆ:**
> - Luôn trả lời với phong thái tự tin, điềm đạm, lắng nghe hết câu hỏi của hội đồng trước khi trả lời.
> - Bắt đầu bằng câu trả lời ngắn gọn (Phần 2) để hội đồng thấy rõ nắm chắc vấn đề ngay lập tức.
> - Nếu hội đồng gật đầu đồng ý, có thể dừng lại hoặc mở rộng sang phần kỹ thuật chuyên sâu (Phần 3) để gây ấn tượng mạnh.
> - Luôn dẫn chứng chính xác tên Class, Entity, Config (Phần 4) để chứng minh mình là người trực tiếp khảo sát và làm chủ source code.
> - Tuyệt đối không giấu diếm nếu có tính năng chưa làm: hãy tự tin trình bày theo hướng dẫn ở Phần 6 (Minh bạch hiện trạng + Giải thích lý do thiết kế + Đưa ra phương án kỹ thuật nâng cấp chuẩn Software Engineer).

