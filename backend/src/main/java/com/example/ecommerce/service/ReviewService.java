package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.AdminReplyRequest;
import com.example.ecommerce.dto.request.ReviewCreateRequest;
import com.example.ecommerce.dto.request.ReviewRequest;
import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.dto.response.ProductRatingSummaryResponse;
import com.example.ecommerce.dto.response.ReviewImageResponse;
import com.example.ecommerce.dto.response.ReviewResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service xử lý nghiệp vụ Đánh giá & Nhận xét sản phẩm.
 * Phụ trách: Đỗ Đức Mạnh (DDM), Cao Chí Thiện (CCT)
 * Task ID: [TSK-D11-DDM], [TSK-D14-DDM] & [TSK-D14-CCT] Tích Hợp Upload Ảnh Đánh Giá (Local Storage) & API Quản Lý Đánh Giá
 */
public interface ReviewService {

    /**
     * Lấy danh sách đánh giá của sản phẩm có phân trang, mặc định sắp xếp mới nhất trước (chỉ trả về các đánh giá hiển thị công khai).
     *
     * @param productId ID sản phẩm
     * @param pageable  Thông tin phân trang và sắp xếp
     * @return PageResponse chứa danh sách ReviewResponse
     */
    PageResponse<ReviewResponse> getProductReviews(Long productId, Pageable pageable);

    /**
     * Thống kê điểm đánh giá trung bình và số lượng đánh giá theo từng mức sao (1-5 sao) của các đánh giá hiển thị.
     *
     * @param productId ID sản phẩm
     * @return ProductRatingSummaryResponse
     */
    ProductRatingSummaryResponse getProductRatingSummary(Long productId);

    /**
     * Tạo đánh giá mới cho sản phẩm.
     * Ràng buộc:
     * - Người dùng phải đăng nhập.
     * - Chỉ khách hàng đã mua sản phẩm và đơn hàng ở trạng thái COMPLETED mới được đánh giá.
     * - Mỗi người dùng chỉ được đánh giá 1 lần duy nhất trên 1 sản phẩm.
     * - Tự động tính toán lại ratingAverage và totalReviews cập nhật vào Product.
     *
     * @param productId ID sản phẩm
     * @param request   Dữ liệu đánh giá (rating, comment)
     * @return ReviewResponse
     */
    ReviewResponse createReview(Long productId, ReviewCreateRequest request);

    /**
     * Chỉnh sửa đánh giá sản phẩm (Chính chủ người dùng).
     *
     * @param productId ID sản phẩm
     * @param reviewId  ID đánh giá cần chỉnh sửa
     * @param request   Dữ liệu cập nhật (rating, comment)
     * @return ReviewResponse
     */
    ReviewResponse updateReview(Long productId, Long reviewId, ReviewRequest request);

    /**
     * Xóa đánh giá sản phẩm (Chính chủ hoặc Quản trị viên ADMIN).
     *
     * @param productId ID sản phẩm
     * @param reviewId  ID đánh giá cần xóa
     */
    void deleteReview(Long productId, Long reviewId);

    /**
     * Quản trị viên phản hồi trực tiếp đánh giá của khách hàng.
     *
     * @param reviewId ID đánh giá
     * @param request  Nội dung phản hồi từ Admin
     * @return ReviewResponse
     */
    ReviewResponse replyReview(Long reviewId, AdminReplyRequest request);

    /**
     * Tải lên hình ảnh đính kèm cho đánh giá sản phẩm (Dành riêng cho khách hàng sở hữu đánh giá).
     * [TSK-D14-CCT]
     *
     * @param reviewId ID đánh giá cần tải ảnh
     * @param files    Danh sách tệp tin ảnh tải lên (JPG, PNG, < 5MB/ảnh, tổng không quá 3 ảnh)
     * @return Danh sách thông tin các ảnh vừa tải lên thành công
     */
    List<ReviewImageResponse> uploadReviewImages(Long reviewId, List<MultipartFile> files);

    /**
     * Quản trị viên (ADMIN) chuyển đổi trạng thái hiển thị công khai (ẩn/hiện) của đánh giá.
     * Tự động tính toán lại điểm rating và số lượng đánh giá công khai của sản phẩm.
     * [TSK-D14-CCT]
     *
     * @param id ID đánh giá cần chuyển đổi trạng thái
     * @return ReviewResponse với trạng thái hiển thị mới
     */
    ReviewResponse toggleReviewVisibility(Long id);

    /**
     * Quản trị viên (ADMIN) xóa vĩnh viễn đánh giá vi phạm và dọn dẹp các tệp ảnh khỏi Local Storage.
     * [TSK-D14-CCT]
     *
     * @param id ID đánh giá cần xóa
     */
    void deleteReviewByAdmin(Long id);
}
