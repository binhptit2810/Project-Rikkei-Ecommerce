# BÁO CÁO NGHIỆM THU TỰ ĐỘNG - TASK [TSK-D14-DHA]

**Tên nhiệm vụ:** Thiết Kế UI Upload Ảnh Đánh Giá, Phản Hồi Admin & Trang Quản Lý Đánh Giá  
**Mã Task:** `[TSK-D14-DHA]`  
**Nhãn:** `PRIORITY: Must (lime)` | `MODULE: Frontend & Admin UI (blue)`  
**Người phụ trách:** Đỗ Hoàng Anh (MSV: B24DTCN300)  
**Nhánh làm việc:** `hoanganh/frontend-devops`  
**Nhánh gốc đồng bộ:** `develop`  
**Ngày thực hiện:** 12/11/2026  
**Môi trường:** Antigravity v2.31.0 (Model: Gemini 3.8 Flash)  

---

## I. TỔNG QUAN KẾT QUẢ THỰC HIỆN (EXECUTIVE SUMMARY)

Đã hoàn thành 100% tất cả các tiêu chí nghiệm thu (Definition of Done) và 5/5 hạng mục trong Checklist Trello mà không xảy ra bất kỳ lỗi runtime, lỗi console, lỗi cú pháp hay cảnh báo nào trong các file triển khai mới. Toàn bộ các quy tắc bảo vệ file môi trường (.env, docker-compose, application*.yml) và bảo toàn code của đồng đội trên nhánh develop đều được tuân thủ nghiêm ngặt.

### Bảng trạng thái Checklist Trello (5/5 hoàn thành xuất sắc):
| STT | Hạng mục Checklist Trello | Trạng thái | Ghi chú kỹ thuật |
|:---:|:---|:---:|:---|
| **[1]** | Thêm nút chọn ảnh và khung xem trước (preview thumbnail) trong form gửi review (tối đa 3 ảnh) | **HOÀN THÀNH (100%)** | Hỗ trợ JPG/PNG/WEBP/GIF, dung lượng <= 5MB/ảnh, tự động giới hạn tối đa 3 ảnh kèm thông báo thân thiện. Khung preview có nút xóa ảnh (X) và spinner loading chống double-submit. |
| **[2]** | Tích hợp lightbox phóng to khi click vào ảnh đánh giá của khách | **HOÀN THÀNH (100%)** | Modal Lightbox chuyên nghiệp, nền tối mờ `bg-black/90 backdrop-blur-md`, hỗ trợ Zoom In/Out/Reset (100%), Prev/Next điều hướng, bộ đếm ảnh X/Y, dải thumbnail chuyển ảnh, phím ESC và phím mũi tên bàn phím. |
| **[3]** | Thiết kế khung "Phản hồi từ Người bán" bo góc màu xám nhạt dưới review | **HOÀN THÀNH (100%)** | Bo góc mềm mại `rounded-2xl`, nền xám nhạt `bg-slate-50/90`, viền mảnh `border-slate-200/80` kèm vạch nhấn bên trái `border-l-4 border-indigo-600`, icon xác thực Store/ShieldCheck, thời gian phản hồi định dạng chi tiết. |
| **[4]** | Xây dựng component `AdminReviewsPage.jsx` cho Admin lọc đánh giá | **HOÀN THÀNH (100%)** | 4 thẻ KPI Summary Cards (Tổng số, Điểm trung bình, Chưa phản hồi, Đang hiển thị/ẩn); Bộ lọc đa chiều: Tìm kiếm từ khóa, Lọc số sao (1-5 sao), Lọc trạng thái hiển thị (Hiện/Ẩn), Lọc phản hồi (Đã/Chưa phản hồi); Nút Reset & Refresh. |
| **[5]** | Chức năng ẩn/hiện, xóa và trả lời (phản hồi) đánh giá dành cho Admin | **HOÀN THÀNH (100%)** | Nút chuyển đổi ẩn/hiện tức thì với loading indicator; Modal trả lời/sửa phản hồi với validate, đếm ký tự (1000 chars); Modal xác nhận xóa an toàn; Click thumbnail ảnh khách trong bảng cũng mở Lightbox sắc nét. |

---

## II. DANH SÁCH FILE ĐÃ TẠO MỚI VÀ CẬP NHẬT

### 1. File tạo mới:
1. `frontend/src/components/reviews/ReviewImageLightbox.jsx`:
   - Component Modal Lightbox xem ảnh lớn độ phân giải cao, hỗ trợ Zoom in/out, điều hướng Trước/Sau, dải thumbnail chuyển ảnh và phím tắt ESC/mũi tên.
2. `frontend/src/pages/admin/AdminReviewsPage.jsx`:
   - Trang quản trị đánh giá sản phẩm toàn diện cho Admin với KPI Cards, bộ lọc tìm kiếm, bảng quản lý, modal phản hồi, modal xác nhận xóa và kiểm duyệt ảnh.
3. `backend/src/main/java/com/example/ecommerce/controller/ReviewUploadController.java`:
   - Endpoint `POST /api/v1/reviews/upload` dành cho khách hàng đăng nhập tải ảnh đính kèm bài đánh giá (tối đa 3 ảnh).
4. `backend/src/test/java/com/example/ecommerce/controller/AdminReviewControllerTest.java`:
   - Bộ unit test toàn diện cho `AdminReviewController` (GET reviews, POST reply, PATCH visibility, DELETE review, phân quyền Admin vs Customer).
5. `docs/reports/TSK-D14-DHA-IMPLEMENTATION-REPORT.md`:
   - Tài liệu báo cáo nghiệm thu chi tiết này.

### 2. File cập nhật:
1. `frontend/src/components/products/ProductReviewsSection.jsx`:
   - Nâng cấp ReviewForm: nút chọn tối đa 3 ảnh, lưới preview thumbnail có nút xóa X, lightbox zoom khi click preview.
   - Thêm dải thumbnail ảnh của khách dưới mỗi review kèm overlay icon kính lúp mở Lightbox.
   - Thiết kế khung "Phản hồi từ Người bán" bo góc màu xám nhạt viền nhấn tím/xanh indigo dưới review.
   - Hỗ trợ thêm/xóa ảnh khi người dùng chỉnh sửa đánh giá (Inline Edit).
2. `frontend/src/services/reviewService.js`:
   - Bổ sung các phương thức: `uploadReviewImage`, `uploadReviewImages`, `getAllReviewsForAdmin`, `updateReplyReview`, `toggleReviewVisibility`, `deleteReviewByAdmin`.
3. `frontend/src/services/uploadService.js`:
   - Bổ sung phương thức `uploadReviewImage` và `uploadReviewImages` với fallback an toàn.
4. `frontend/src/App.jsx`:
   - Đăng ký tuyến đường `/admin/reviews` trỏ vào `AdminReviewsPage` trong cụm RoleRoute ADMIN.
5. `frontend/src/components/admin/AdminSidebar.jsx`:
   - Kích hoạt menu "Quản lý đánh giá" (`isImplemented: true`) dẫn tới `/admin/reviews`.
6. `backend/src/main/java/com/example/ecommerce/entity/Review.java`:
   - Bổ sung trường `images` (TEXT) và `isHidden` (Boolean, mặc định false).
7. `backend/src/main/java/com/example/ecommerce/dto/response/ReviewResponse.java`:
   - Bổ sung ánh xạ `images` (List<String>) và `isHidden` (Boolean).
8. `backend/src/main/java/com/example/ecommerce/dto/request/ReviewCreateRequest.java` & `ReviewRequest.java` & `ReviewUpdateRequest.java`:
   - Bổ sung trường `images` (List<String>).
9. `backend/src/main/java/com/example/ecommerce/repository/ReviewRepository.java`:
   - Thêm phương thức truy vấn `findAllForAdmin` lọc linh hoạt theo `productId`, `rating`, `isHidden` kết hợp phân trang `Pageable`.
10. `backend/src/main/java/com/example/ecommerce/service/ReviewService.java` & `ReviewServiceImpl.java`:
    - Triển khai `getAllReviewsForAdmin`, `deleteReviewByAdmin`, `toggleReviewVisibility` và lưu danh sách ảnh vào review.
11. `backend/src/main/java/com/example/ecommerce/controller/AdminReviewController.java`:
    - Bổ sung các endpoints `GET /api/v1/admin/reviews`, `PATCH /api/v1/admin/reviews/{id}/visibility`, `DELETE /api/v1/admin/reviews/{id}`.

---

## III. CHI TIẾT TRIỂN KHAI THEO TỪNG HẠNG MỤC CHECKLIST

### [1] Nút chọn tối đa 3 ảnh & Khung xem trước (Preview Thumbnail) trong ReviewForm
- Thiết kế nút chọn ảnh trực quan với icon máy ảnh `Camera` và `UploadCloud`, nhãn hiển thị số lượng hiện tại `(X/3 ảnh)`.
- Ràng buộc và validation ở Frontend:
  + Giới hạn tối đa 3 ảnh. Nếu người dùng chọn vượt quá 3 ảnh, hệ thống thông báo nhắc nhở lịch sự và chỉ giữ lại đủ số lượng 3 ảnh.
  + Kiểm tra định dạng tệp: Chỉ chấp nhận các định dạng ảnh hợp lệ `image/jpeg`, `image/png`, `image/webp`, `image/gif`.
  + Kiểm tra dung lượng tệp: Dung lượng tối đa 5MB/ảnh. Báo lỗi chi tiết tên file nếu quá giới hạn.
  + Khi đã đủ 3/3 ảnh: Nút chọn ảnh tự động chuyển sang trạng thái disabled với badge tích xanh "Đã đạt tối đa 3/3 ảnh".
- Khung xem trước (Preview Thumbnail):
  + Lưới thumbnail vuông vắn (`w-20 h-20` / `w-24 h-24`), bo góc `rounded-2xl`, hiệu ứng `object-cover` sắc nét.
  + Nút xóa ảnh góc trên bên phải (icon X nền đỏ tròn) cho phép người dùng loại bỏ nhanh ảnh đã chọn.
  + Hover vào thumbnail hiển thị overlay icon kính lúp `ZoomIn`, click vào sẽ mở Lightbox xem trước ảnh lớn.
  + Hiển thị trạng thái spinner loading trong quá trình upload ảnh và submit form, vô hiệu hóa nút submit để chống bấm trùng (double-submit).

### [2] Tích hợp Lightbox phóng to sắc nét khi click vào ảnh đánh giá của khách
- Dưới mỗi đánh giá của khách có đính kèm ảnh, các thumbnail được hiển thị gọn gàng, bo góc mềm mại, hover có hiệu ứng phóng nhẹ và icon kính lúp `ZoomIn`.
- Khi click vào bất kỳ ảnh nào, Modal Lightbox chuyên nghiệp xuất hiện:
  + Nền tối mờ sang trọng `fixed inset-0 z-50 bg-black/90 backdrop-blur-md`.
  + Ảnh hiển thị kích thước lớn, độ phân giải gốc sắc nét `max-h-[82vh] max-w-[90vw] object-contain`.
  + Hỗ trợ nút Phóng to (`ZoomIn`), Thu nhỏ (`ZoomOut`), Đặt lại kích thước gốc 100% (`RotateCcw`).
  + Khi đánh giá có từ 2-3 ảnh: Hiển thị nút mũi tên Trước (`ChevronLeft`) / Sau (`ChevronRight`), bộ đếm `Ảnh X / Y` và dải thumbnail nhỏ phía dưới để click chuyển ảnh tức thì.
  + Hỗ trợ đóng Lightbox: Bấm nút X, click ra vùng nền ngoài ảnh, hoặc bấm phím `Escape (ESC)`. Đồng thời hỗ trợ phím mũi tên `Trái / Phải` trên bàn phím để chuyển ảnh.
  + Hàm `resolveImageUrl` xử lý an toàn mọi dạng URL: relative server path (`/uploads/...`, `/api/v1/uploads/...`), absolute URL (`http/https`), `blob:` URL preview và `data:` URL.

### [3] Thiết kế khung "Phản hồi từ Người bán" bo góc màu xám nhạt
- Dưới mỗi đánh giá đã có phản hồi từ Admin/Cửa hàng:
  + Kiểu dáng: Bo góc mềm mại `rounded-2xl`, nền màu xám nhạt `bg-slate-50/90`, viền mảnh `border border-slate-200/80` kết hợp vạch nhấn bên trái `border-l-4 border-indigo-600`, padding `p-4`, cách tách biệt `mt-4`.
  + Tiêu đề khung: Nhãn nổi bật "Phản hồi từ Người bán" kết hợp icon cửa hàng `Store` và huy hiệu xác thực `ShieldCheck` màu xanh dương/tím indigo uy tín, kèm thời gian phản hồi định dạng chuẩn ngày giờ Việt Nam.
  + Nội dung phản hồi: Hiển thị văn bản sắc nét, dễ đọc, giữ đúng định dạng ngắt dòng `whitespace-pre-line leading-relaxed`.

### [4] & [5] Xây dựng trang Quản lý Đánh giá Admin (`AdminReviewsPage.jsx`)
- **KPI Summary Cards (4 thẻ đầu trang):**
  1. *Tổng số đánh giá*: Số lượng toàn hệ thống, icon `MessageSquare`.
  2. *Điểm trung bình*: Điểm rating trung bình to nổi bật kèm dải sao vàng, icon `Star`.
  3. *Chưa phản hồi*: Đánh dấu số lượng đánh giá cần Admin trả lời gấp, icon `MessageSquareReply`.
  4. *Trạng thái hiển thị*: Thống kê rõ số lượng đánh giá đang hiển thị và đang ẩn, icon `Eye`.
- **Bộ lọc thông minh (Filters & Search):**
  + Ô tìm kiếm từ khóa: Tìm kiếm tức thời theo tên sản phẩm, tên khách hàng, nội dung bình luận hoặc ID sản phẩm.
  + Dropdown lọc theo số sao: Tất cả sao, 5 sao, 4 sao, 3 sao, 2 sao, 1 sao.
  + Dropdown lọc theo trạng thái hiển thị: Tất cả, Đang hiển thị, Đang ẩn.
  + Dropdown lọc theo trạng thái phản hồi: Tất cả, Đã phản hồi, Chưa phản hồi.
  + Dải tag "Đang lọc" (Filter Pills) và nút "Đặt lại bộ lọc" (Reset).
- **Bảng dữ liệu quản trị (Table Management):**
  + Cột Sản phẩm: Ảnh thumbnail, tên sản phẩm có link mở xem chi tiết tại gian hàng, mã ID sản phẩm.
  + Cột Khách hàng: Avatar/chữ cái đầu, tên khách, ngày giờ gửi đánh giá.
  + Cột Đánh giá & Ảnh thực tế: Số sao vàng, nội dung nhận xét, danh sách thumbnail ảnh khách gửi. Admin click vào thumbnail sẽ mở ngay Lightbox để kiểm duyệt ảnh phóng to sắc nét.
  + Cột Phản hồi từ Admin: Nếu đã phản hồi thì hiển thị khung xám bo góc có nút "Sửa"; nếu chưa phản hồi thì có badge "Chưa phản hồi" màu hổ phách và nút "+ Trả lời".
  + Cột Trạng thái: Badge "Đang hiển thị" màu xanh lá hoặc "Đang ẩn" màu đỏ hồng.
  + Cột Thao tác:
    * Nút Ẩn/Hiện: Chuyển đổi trạng thái hiển thị tức thời, có spinner loading riêng biệt trên nút và thông báo toast thành công.
    * Nút Phản hồi: Mở Modal Trả lời Đánh giá, hiển thị lại trích dẫn bình luận và ảnh của khách, ô textarea nhập phản hồi có validate không để trống và bộ đếm 1000 ký tự.
    * Nút Xóa: Mở Modal xác nhận xóa an toàn, cảnh báo tính toán lại điểm rating trung bình.
- **Phân trang & Đồng bộ hệ thống:**
  + Tích hợp thanh phân trang (Pagination) mượt mà ở chân bảng.
  + Đăng ký route `/admin/reviews` trong `App.jsx` và kích hoạt mục menu "Quản lý đánh giá" trên `AdminSidebar.jsx`.

---

## IV. KẾT QUẢ KIỂM THỬ (QA/TEST VERIFICATION)

### 1. Frontend Build & Lint:
- **Lệnh thực thi:** `npm run build`
  + Kết quả: `✓ built in 7.86s` (Thành công 100%, 0 lỗi, 0 cảnh báo trong toàn bộ các file triển khai mới).
- **Lệnh thực thi:** `npm run lint`
  + Kết quả: Tất cả các file triển khai mới (`ProductReviewsSection.jsx`, `AdminReviewsPage.jsx`, `ReviewImageLightbox.jsx`, `reviewService.js`, `uploadService.js`, `App.jsx`, `AdminSidebar.jsx`) đều đạt **0 lỗi lint, 0 unused vars**.

### 2. Backend Test Suite:
- **Lệnh thực thi:** `.\gradlew.bat test --tests "*Review*"`
  + `AdminReviewControllerTest`: PASS 100% (5/5 tests: getAllReviews, authorization 403, replyReview, toggleReviewVisibility, deleteReview).
  + `ReviewControllerTest`: PASS 100%.
  + `ReviewServiceTest`: PASS 100%.
  + `ReviewUpdateAndReplyTest`: PASS 100%.
  + Kết quả: `BUILD SUCCESSFUL`.

### 3. Kiểm tra bảo vệ file cấu hình và môi trường:
- Không có bất kỳ thay đổi nào trong `.env`, `.env.example`, `frontend/.env*`, `docker-compose.yml`, hay `application*.yml`.
