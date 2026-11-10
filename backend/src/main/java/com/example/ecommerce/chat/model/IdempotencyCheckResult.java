package com.example.ecommerce.chat.model;

import com.example.ecommerce.dto.response.ChatResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kết quả kiểm tra Idempotency key trong Redis.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Idempotency Cache Pattern
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyCheckResult {

    public enum Status {
        NEW,
        IN_PROGRESS,
        COMPLETED,
        MISMATCH
    }

    private Status status;
    private ChatResponse cachedResponse;

    public static IdempotencyCheckResult newRequest() {
        return new IdempotencyCheckResult(Status.NEW, null);
    }

    public static IdempotencyCheckResult inProgress() {
        return new IdempotencyCheckResult(Status.IN_PROGRESS, null);
    }

    public static IdempotencyCheckResult completed(ChatResponse cached) {
        return new IdempotencyCheckResult(Status.COMPLETED, cached);
    }

    public static IdempotencyCheckResult mismatch() {
        return new IdempotencyCheckResult(Status.MISMATCH, null);
    }
}

