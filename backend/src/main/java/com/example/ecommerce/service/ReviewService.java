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
 * Phụ trách: Đỗ Đức Mạnh (DDM), Đỗ Hoàng Anh (DHA), Cao Chí Thiện (CCT)
 * Task ID: [TSK-D11-DDM], [TSK-D14-DHA] & [TSK-D14-CCT]
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
     * Thống kê điểm đánh giá trung bình và số lượng đánh giá theo từng mức sao (1-5 sao) của các đánh giá hiển thị công khai.
     *
     * @param productId ID sản phẩm
     * @return ProductRatingSummaryResponse
     */
    ProductRatingSummaryResponse getProductRatingSummary(Long productId);

    /**
     * Khách hàng tạo đánh giá mới cho sản phẩm đã mua và hoàn thành giao dịch.
     *
     * @param productId ID sản phẩm
     * @param request   Dữ liệu đánh giá (rating 1-5, comment, tùy chọn images)
     * @return ReviewResponse
     */
    ReviewResponse createReview(Long productId, ReviewCreateRequest request);

    /**
     * Khách hàng chỉnh sửa đánh giá đã gửi (chính chủ người dùng).
     *
     * @param productId ID sản phẩm
     * @param reviewId  ID đánh giá
     * @param request   Dữ liệu chỉnh sửa (rating 1-5, comment, tùy chọn images)
     * @return ReviewResponse sau khi cập nhật
     */
    ReviewResponse updateReview(Long productId, Long reviewId, ReviewRequest request);

    /**
     * Xóa đánh giá sản phẩm (chính chủ hoặc Quản trị viên).
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
     * Lấy toàn bộ danh sách đánh giá của hệ thống dành cho Admin quản lý có phân trang và bộ lọc.
     *
     * @param pageable  Thông tin phân trang và sắp xếp
     * @param productId Lọc theo ID sản phẩm (tùy chọn)
     * @param rating    Lọc theo số sao (tùy chọn)
     * @param isHidden  Lọc theo trạng thái ẩn/hiện (tùy chọn)
     * @return PageResponse chứa danh sách ReviewResponse
     */
    PageResponse<ReviewResponse> getAllReviewsForAdmin(Pageable pageable, Long productId, Integer rating, Boolean isHidden);

    /**
     * Lấy danh sách tất cả đánh giá phục vụ Quản trị viên (Admin) kèm phân trang và lọc theo trạng thái hiển thị.
     * [TSK-D14-CCT]
     *
     * @param isVisible Lọc theo trạng thái hiển thị (null: tất cả, true: hiển thị, false: đã ẩn)
     * @param pageable  Thông tin phân trang và sắp xếp
     * @return PageResponse chứa danh sách ReviewResponse
     */
    PageResponse<ReviewResponse> getAllReviewsForAdmin(Boolean isVisible, Pageable pageable);

    /**
     * Quản trị viên (ADMIN) chuyển đổi trạng thái hiển thị công khai (ẩn/hiện) của đánh giá.
     * Tự động tính toán lại điểm rating và số lượng đánh giá công khai của sản phẩm.
     *
     * @param id ID đánh giá cần chuyển đổi trạng thái
     * @return ReviewResponse với trạng thái hiển thị mới
     */
    ReviewResponse toggleReviewVisibility(Long id);

    /**
     * Quản trị viên (ADMIN) xóa vĩnh viễn đánh giá vi phạm và dọn dẹp các tệp ảnh khỏi Local Storage.
     *
     * @param id ID đánh giá cần xóa
     */
    void deleteReviewByAdmin(Long id);
}
