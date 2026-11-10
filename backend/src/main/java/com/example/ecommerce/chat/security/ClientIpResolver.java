package com.example.ecommerce.chat.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Component xác định địa chỉ Client IP an toàn, chống giả mạo (IP Spoofing)
 * khi ứng dụng chạy phía sau Reverse Proxy (Nginx) hoặc trong môi trường Docker.
 *
 * Nguyên tắc bảo mật:
 * 1. Chỉ tin cậy các header chuyển tiếp (X-Real-IP, X-Forwarded-For) khi địa chỉ socket remote
 *    (request.getRemoteAddr()) thuộc danh sách Trusted Proxy cấu hình tường minh.
 * 2. Khi client kết nối trực tiếp không qua Trusted Proxy: BỎ QUA toàn bộ headers, chỉ dùng getRemoteAddr().
 * 3. Nginx cấu hình: `proxy_set_header X-Real-IP $remote_addr;` ghi đè cố định IP kết nối TCP,
 *    kẻ tấn công không thể giả mạo X-Real-IP khi đi qua Nginx.
 * 4. Không tin tưởng phần tử đầu tiên của X-Forwarded-For do client có thể tự inject.
 * 5. Sanitize và kiểm tra định dạng IPv4/IPv6 hợp lệ, loại bỏ ký tự lạ để chống injection vào Redis Key.
 */
@Component
@Slf4j
public class ClientIpResolver {

    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );

    private static final Pattern IPV6_PATTERN = Pattern.compile(
            "^[0-9a-fA-F:.]+$"
    );

    private final Set<String> trustedExactIps = new HashSet<>();
    private final List<String> trustedCidrs = new ArrayList<>();

    public ClientIpResolver(@Value("${app.security.trusted-proxies:127.0.0.1,::1,0:0:0:0:0:0:0:1}") String trustedProxiesConfig) {
        initTrustedProxies(trustedProxiesConfig);
    }

    private void initTrustedProxies(String config) {
        if (config == null || config.isBlank()) {
            trustedExactIps.add("127.0.0.1");
            trustedExactIps.add("::1");
            trustedExactIps.add("0:0:0:0:0:0:0:1");
            return;
        }

        String[] parts = config.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) continue;
            if (trimmed.contains("/")) {
                trustedCidrs.add(trimmed);
            } else {
                trustedExactIps.add(trimmed.toLowerCase());
            }
        }
    }

    /**
     * Trích xuất địa chỉ IP thực tế của client an toàn từ HTTP request.
     *
     * @param request HttpServletRequest
     * @return Chuỗi IP client đã được sanitize an toàn
     */
    public String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr == null || remoteAddr.isBlank()) {
            return "unknown";
        }
        remoteAddr = remoteAddr.trim();

        // 1. Kiểm tra socket IP có thuộc danh sách Trusted Proxy hay không
        if (!isTrustedProxy(remoteAddr)) {
            // Kết nối trực tiếp: hoàn toàn bỏ qua mọi header client gửi lên
            return sanitizeIp(remoteAddr);
        }

        // 2. Kết nối đến từ Trusted Proxy: Nginx ghi đè X-Real-IP bằng $remote_addr
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            String sanitizedRealIp = sanitizeIp(xRealIp.trim());
            if (isValidIp(sanitizedRealIp)) {
                return sanitizedRealIp;
            }
        }

        // 3. Fallback sang X-Forwarded-For nếu X-Real-IP không có (lấy IP cuối cùng trước proxy)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            String resolvedXff = extractRightmostClientIp(xForwardedFor);
            if (resolvedXff != null) {
                return resolvedXff;
            }
        }

        // 4. Nếu không có header nào hợp lệ từ proxy, dùng chính remoteAddr đã sanitize
        return sanitizeIp(remoteAddr);
    }

    /**
     * Kiểm tra một địa chỉ IP có thuộc Trusted Proxy cấu hình hay không.
     */
    public boolean isTrustedProxy(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        String cleanIp = ip.trim().toLowerCase();
        if (trustedExactIps.contains(cleanIp)) {
            return true;
        }

        for (String cidr : trustedCidrs) {
            if (matchesCidr(cleanIp, cidr)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesCidr(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/");
            if (parts.length != 2) return false;
            InetAddress target = InetAddress.getByName(ip);
            InetAddress network = InetAddress.getByName(parts[0]);
            if (!(target instanceof Inet4Address && network instanceof Inet4Address)) {
                return false;
            }
            int prefix = Integer.parseInt(parts[1]);
            if (prefix < 0 || prefix > 32) return false;
            int mask = (prefix == 0) ? 0 : (-1 << (32 - prefix));
            int targetInt = ByteBuffer.wrap(target.getAddress()).getInt();
            int networkInt = ByteBuffer.wrap(network.getAddress()).getInt();
            return (targetInt & mask) == (networkInt & mask);
        } catch (Exception e) {
            return false;
        }
    }

    private String extractRightmostClientIp(String xForwardedFor) {
        String[] hops = xForwardedFor.split(",");
        for (int i = hops.length - 1; i >= 0; i--) {
            String candidate = sanitizeIp(hops[i].trim());
            if (isValidIp(candidate) && !isTrustedProxy(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    public boolean isValidIp(String ip) {
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            return false;
        }
        if (IPV4_PATTERN.matcher(ip).matches()) {
            return true;
        }
        return IPV6_PATTERN.matcher(ip).matches() && ip.contains(":");
    }

    public String sanitizeIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "unknown";
        }
        String sanitized = ip.trim().toLowerCase();
        // Loại bỏ mọi ký tự nguy hiểm cho Redis key hoặc log
        sanitized = sanitized.replaceAll("[^0-9a-f:.]", "");
        return sanitized.isEmpty() ? "unknown" : sanitized;
    }
}

