package com.example.ecommerce.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO định dạng JSON phẳng phản hồi cho Cổng thanh toán VNPay khi nhận IPN webhook.
 * Phụ trách: Cao Chí Thiện (TSK-D12-CCT - Phase 1)
 *
 * Chuẩn đặc tả VNPay 2.1.0:
 * RspCode:
 *   "00": Confirm Success (Giao dịch thành công hoặc đã ghi nhận kết quả thất bại thành công)
 *   "01": Order not Found (Không tìm thấy giao dịch với mã vnp_TxnRef)
 *   "02": Order already confirmed (Giao dịch đã được cập nhật trước đó - Idempotency)
 *   "04": Invalid Amount (Số tiền thanh toán không khớp)
 *   "97": Invalid Checksum (Chữ ký bảo mật không hợp lệ)
 *   "99": Unknown error (Lỗi hệ thống ngoại lệ)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VNPayIpnResponse {

    @JsonProperty("RspCode")
    private String rspCode;

    @JsonProperty("Message")
    private String message;

    public static VNPayIpnResponse success() {
        return new VNPayIpnResponse("00", "Confirm Success");
    }

    public static VNPayIpnResponse orderNotFound() {
        return new VNPayIpnResponse("01", "Order not Found");
    }

    public static VNPayIpnResponse alreadyConfirmed() {
        return new VNPayIpnResponse("02", "Order already confirmed");
    }

    public static VNPayIpnResponse invalidAmount() {
        return new VNPayIpnResponse("04", "Invalid Amount");
    }

    public static VNPayIpnResponse invalidChecksum() {
        return new VNPayIpnResponse("97", "Invalid Checksum");
    }

    public static VNPayIpnResponse unknownError() {
        return new VNPayIpnResponse("99", "Unknown error");
    }
}

