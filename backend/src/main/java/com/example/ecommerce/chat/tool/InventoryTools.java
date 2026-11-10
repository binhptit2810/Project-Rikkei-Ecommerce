package com.example.ecommerce.chat.tool;

import com.example.ecommerce.chat.tool.dto.StockCheckRequest;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Tool tra cứu số lượng tồn kho thực tế của sản phẩm.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Spring AI Tool Calling
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryTools {

    private final ProductRepository productRepository;

    /**
     * Tra cứu số lượng tồn kho thực tế của sản phẩm trong hệ thống.
     * Phân biệt rõ: không tồn tại, ngừng kinh doanh, hết hàng và còn hàng.
     */
    public String checkProductStock(StockCheckRequest request) {
        log.info("Tool checkProductStock called with request: {}", request);
        if (request == null || request.getProductId() == null) {
            return "Vui lòng cung cấp mã ID sản phẩm cần kiểm tra tồn kho.";
        }

        try {
            Optional<Product> productOpt = productRepository.findById(request.getProductId());
            if (productOpt.isEmpty()) {
                return "Sản phẩm mã #" + request.getProductId() + " không tồn tại trong hệ thống Rikkei Ecommerce.";
            }

            Product product = productOpt.get();
            if (Boolean.FALSE.equals(product.getActive())) {
                return String.format("Sản phẩm '%s' (mã #%d) hiện đang tạm ngừng kinh doanh.",
                        product.getName(), product.getId());
            }

            int stockQuantity = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
            if (stockQuantity <= 0) {
                return String.format("Sản phẩm '%s' (mã #%d) hiện đã TẠM HẾT HÀNG trong kho (số lượng tồn: 0).",
                        product.getName(), product.getId());
            }

            return String.format("Sản phẩm '%s' (mã #%d) HIỆN CÒN HÀNG: còn %d sản phẩm trong kho với mức giá niêm yết %,.0f đ.",
                    product.getName(), product.getId(), stockQuantity, product.getPrice());

        } catch (Exception e) {
            log.error("Error executing checkProductStock tool: {}", e.getMessage(), e);
            return "Lỗi khi kiểm tra tồn kho sản phẩm: " + e.getMessage();
        }
    }
}

