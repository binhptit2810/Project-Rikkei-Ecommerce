package com.example.ecommerce.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Tiện ích xử lý mã hóa, sắp xếp tham số alphabet và băm chữ ký bảo mật HMAC-SHA512 chuẩn VNPay.
 * Phụ trách: Đặng Thanh Bình (DTB)
 * Phối hợp: Cao Chí Thiện (CCT)
 */
@Slf4j
public final class VNPayUtil {

    private VNPayUtil() {
        // Utility class
    }

    /**
     * Băm chuỗi dữ liệu với khóa bí mật secretKey theo thuật toán HMAC-SHA512 (chuẩn RFC 2104).
     *
     * @param key  Khóa bí mật (hashSecret của VNPay)
     * @param data Chuỗi dữ liệu cần băm (hashData)
     * @return Chuỗi băm Hex dạng chữ thường (lowercase)
     */
    public static String hmacSHA512(final String key, final String data) {
        try {
            if (key == null || data == null) {
                throw new IllegalArgumentException("Khóa bí mật hoặc dữ liệu băm không được null");
            }
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);

            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);

            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            log.error("Lỗi khi băm HMAC-SHA512: {}", ex.getMessage());
            return "";
        }
    }

    /**
     * Sắp xếp các tham số alphabet và tạo chuỗi hashData phục vụ sinh chữ ký bảo mật.
     *
     * @param fields    Map chứa các tham số VNPay
     * @param secretKey Khóa bí mật hashSecret
     * @return Chữ ký bảo mật HMAC-SHA512
     */
    public static String hashAllFields(Map<String, String> fields, String secretKey) {
        List<String> fieldNames = fields.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().trim().isEmpty())
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());

        StringBuilder hashData = new StringBuilder();
        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            String fieldValue = fields.get(fieldName);

            try {
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));

                if (i < fieldNames.size() - 1) {
                    hashData.append('&');
                }
            } catch (Exception ex) {
                log.warn("Lỗi URL encode tham số {}: {}", fieldName, ex.getMessage());
            }
        }

        return hmacSHA512(secretKey, hashData.toString());
    }

    /**
     * Xây dựng URL thanh toán VNPay hoàn chỉnh bao gồm query parameters và chữ ký vnp_SecureHash.
     *
     * @param params    Các tham số thanh toán vnp_*
     * @param payUrl    Địa chỉ cổng thanh toán Sandbox (vpcpay.html)
     * @param secretKey Khóa bí mật hashSecret
     * @return URL thanh toán chuyển hướng tới VNPay
     */
    public static String buildPaymentUrl(Map<String, String> params, String payUrl, String secretKey) {
        List<String> fieldNames = params.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().trim().isEmpty())
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            String fieldValue = params.get(fieldName);

            try {
                String encodedKey = URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString());
                String encodedValue = URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString());

                // Build hash data
                hashData.append(fieldName).append('=').append(encodedValue);

                // Build query
                query.append(encodedKey).append('=').append(encodedValue);

                if (i < fieldNames.size() - 1) {
                    hashData.append('&');
                    query.append('&');
                }
            } catch (Exception ex) {
                log.warn("Lỗi encode khi tạo paymentUrl cho {}: {}", fieldName, ex.getMessage());
            }
        }

        String secureHash = hmacSHA512(secretKey, hashData.toString());
        query.append("&vnp_SecureHash=").append(secureHash);

        return payUrl + "?" + query.toString();
    }

    /**
     * Lấy địa chỉ IP chính xác của client gửi yêu cầu (hỗ trợ reverse proxy, Cloudflare, Nginx).
     *
     * @param request HttpServletRequest
     * @return Địa chỉ IP (chuẩn hóa IPv4 nếu là localhost)
     */
    public static String getIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }

        String[] headers = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "X-Real-IP"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For có thể chứa chuỗi IP phân tách bởi dấu phẩy, lấy IP đầu tiên
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : ip;
            }
        }

        String remoteAddr = request.getRemoteAddr();
        return "0:0:0:0:0:0:0:1".equals(remoteAddr) ? "127.0.0.1" : (remoteAddr != null ? remoteAddr : "127.0.0.1");
    }

    /**
     * Xác thực chữ ký dữ liệu trả về từ VNPay (Return URL hoặc IPN Callback).
     *
     * @param queryParams Toàn bộ tham số nhận được từ VNPay
     * @param secretKey   Khóa bí mật hashSecret
     * @return true nếu chữ ký hợp lệ và nguyên vẹn
     */
    public static boolean verifyCallback(Map<String, String> queryParams, String secretKey) {
        if (queryParams == null || queryParams.isEmpty() || secretKey == null) {
            return false;
        }

        String vnpSecureHash = queryParams.get("vnp_SecureHash");
        if (vnpSecureHash == null || vnpSecureHash.trim().isEmpty()) {
            return false;
        }

        // Sao chép map và loại bỏ vnp_SecureHash, vnp_SecureHashType trước khi băm kiểm tra
        Map<String, String> fieldsToHash = new HashMap<>(queryParams);
        fieldsToHash.remove("vnp_SecureHash");
        fieldsToHash.remove("vnp_SecureHashType");

        String calculatedHash = hashAllFields(fieldsToHash, secretKey);
        return calculatedHash.equalsIgnoreCase(vnpSecureHash);
    }
}

