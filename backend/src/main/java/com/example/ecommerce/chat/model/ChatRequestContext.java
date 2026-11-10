package com.example.ecommerce.chat.model;

import com.example.ecommerce.dto.response.ProductResponse;
import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Ngữ cảnh quản lý ngân sách thời gian (Request Deadline), số vòng lặp Tool Calling
 * và thu thập danh sách sản phẩm có cấu trúc cho mỗi request Chatbot AI.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TASK-CHAT-PRODUCT-CARDS]
 */
@Getter
public class ChatRequestContext {

    private static final ThreadLocal<ChatRequestContext> CONTEXT_HOLDER = new ThreadLocal<>();
    private static final int MAX_REFERENCED_PRODUCTS = 10;

    private final long deadlineTimestamp;
    private final int maxTurns;
    private final AtomicInteger turnCount = new AtomicInteger(0);
    private final Map<Long, ProductResponse> referencedProductsMap = new LinkedHashMap<>();

    public ChatRequestContext(long deadlineTimestamp, int maxTurns) {
        this.deadlineTimestamp = deadlineTimestamp;
        this.maxTurns = maxTurns > 0 ? maxTurns : 3;
    }

    public static void init(long deadlineTimestamp, int maxTurns) {
        CONTEXT_HOLDER.set(new ChatRequestContext(deadlineTimestamp, maxTurns));
    }

    public static ChatRequestContext get() {
        return CONTEXT_HOLDER.get();
    }

    public static void clear() {
        CONTEXT_HOLDER.remove();
    }

    /**
     * Tính toán thời gian ngân sách còn lại tính bằng mili-giây.
     */
    public long getRemainingMs() {
        return deadlineTimestamp - System.currentTimeMillis();
    }

    /**
     * Tăng số thứ tự vòng gọi HTTP và trả về giá trị mới.
     */
    public int incrementAndGetTurn() {
        return turnCount.incrementAndGet();
    }

    /**
     * Thêm danh sách sản phẩm được truy xuất bởi ProductTools.
     * Khử trùng lặp theo ID và giới hạn tối đa MAX_REFERENCED_PRODUCTS.
     */
    public synchronized void addReferencedProducts(List<ProductResponse> products) {
        if (products == null || products.isEmpty()) {
            return;
        }
        for (ProductResponse product : products) {
            addReferencedProduct(product);
        }
    }

    /**
     * Thêm một sản phẩm đơn lẻ vào ngữ cảnh (dùng cho getProductDetail).
     */
    public synchronized void addReferencedProduct(ProductResponse product) {
        if (product == null || product.getId() == null) {
            return;
        }
        if (referencedProductsMap.size() < MAX_REFERENCED_PRODUCTS) {
            referencedProductsMap.putIfAbsent(product.getId(), product);
        }
    }

    /**
     * Lấy danh sách sản phẩm đã thu thập theo thứ tự xuất hiện ban đầu.
     */
    public synchronized List<ProductResponse> getReferencedProducts() {
        return new ArrayList<>(referencedProductsMap.values());
    }
}

