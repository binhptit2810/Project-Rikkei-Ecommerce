package com.example.ecommerce.chat.tool;

import com.example.ecommerce.chat.tool.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Cấu hình đăng ký các công cụ (Tool / Function Calling) cho Spring AI.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Spring AI Tool Calling
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class ChatToolsConfig {

    private final ProductTools productTools;
    private final InventoryTools inventoryTools;
    private final OrderTools orderTools;

    @Bean
    public FunctionCallback searchProductsCallback() {
        return FunctionCallback.builder()
                .description("Tìm kiếm danh sách sản phẩm theo từ khóa (tên/mô tả), danh mục hoặc khoảng giá. Tối đa 5 kết quả đang bán.")
                .function("searchProducts", (ProductSearchRequest req) -> productTools.searchProducts(req))
                .inputType(ProductSearchRequest.class)
                .build();
    }

    @Bean
    public FunctionCallback getProductDetailCallback() {
        return FunctionCallback.builder()
                .description("Lấy thông tin chi tiết một sản phẩm theo ID (tên, giá, mô tả, danh mục, tồn kho).")
                .function("getProductDetail", (ProductDetailRequest req) -> productTools.getProductDetail(req))
                .inputType(ProductDetailRequest.class)
                .build();
    }

    @Bean
    public FunctionCallback checkProductStockCallback() {
        return FunctionCallback.builder()
                .description("Kiểm tra số lượng tồn kho thực tế của một sản phẩm cụ thể theo ID. Cho biết tình trạng còn hàng hay hết hàng.")
                .function("checkProductStock", (StockCheckRequest req) -> inventoryTools.checkProductStock(req))
                .inputType(StockCheckRequest.class)
                .build();
    }

    @Bean
    public FunctionCallback getMyOrdersCallback() {
        return FunctionCallback.builder()
                .description("Tra cứu danh sách các đơn hàng gần đây của khách hàng đang đăng nhập. Yêu cầu tài khoản đã đăng nhập.")
                .function("getMyOrders", (MyOrdersRequest req, ToolContext ctx) -> orderTools.getMyOrders(req, ctx))
                .inputType(MyOrdersRequest.class)
                .build();
    }

    @Bean
    public FunctionCallback getOrderDetailCallback() {
        return FunctionCallback.builder()
                .description("Tra cứu thông tin chi tiết một đơn hàng cụ thể theo mã đơn hàng hoặc ID. Yêu cầu tài khoản đã đăng nhập.")
                .function("getOrderDetail", (OrderDetailRequest req, ToolContext ctx) -> orderTools.getOrderDetail(req, ctx))
                .inputType(OrderDetailRequest.class)
                .build();
    }

    /**
     * Danh sách Tool cho phép khách vãng lai (Guest) sử dụng.
     * Tuyệt đối không bao gồm Order Tools để đảm bảo an toàn dữ liệu cá nhân.
     */
    public FunctionCallback[] getGuestTools() {
        return new FunctionCallback[]{
                searchProductsCallback(),
                getProductDetailCallback(),
                checkProductStockCallback()
        };
    }

    /**
     * Danh sách Tool đầy đủ cho người dùng đã đăng nhập (Authenticated User).
     */
    public FunctionCallback[] getAllTools() {
        return new FunctionCallback[]{
                searchProductsCallback(),
                getProductDetailCallback(),
                checkProductStockCallback(),
                getMyOrdersCallback(),
                getOrderDetailCallback()
        };
    }
}

