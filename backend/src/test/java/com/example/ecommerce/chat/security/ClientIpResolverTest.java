package com.example.ecommerce.chat.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit Test xác minh tính năng trích xuất Client IP an toàn và chống IP Spoofing qua Nginx.
 * Phụ trách: Cao Chí Thiện (TSK-D12-CCT)
 */
class ClientIpResolverTest {

    private final ClientIpResolver resolver = new ClientIpResolver("127.0.0.1,::1,172.16.0.0/12");

    @Test
    @DisplayName("IP-01: Kết nối trực tiếp (Direct Connection) không qua Trusted Proxy -> Chỉ dùng remoteAddr, bỏ qua mọi header")
    void directConnection_IgnoresSpoofedHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.50"); // Public client IP kết nối thẳng tới backend port 8080
        request.addHeader("X-Real-IP", "8.8.8.8"); // Attacker giả mạo header
        request.addHeader("X-Forwarded-For", "1.1.1.1, 8.8.8.8");

        String clientIp = resolver.resolveClientIp(request);

        // Bỏ qua toàn bộ header giả mạo, lấy chính xác IP socket kết nối
        assertThat(clientIp).isEqualTo("203.0.113.50");
    }

    @Test
    @DisplayName("IP-02: Kết nối chuyển tiếp từ Nginx Trusted Proxy (127.0.0.1) -> Lấy X-Real-IP đã được Nginx ghi đè an toàn")
    void forwardedFromTrustedProxy_UsesXRealIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1"); // Nginx trên localhost
        request.addHeader("X-Real-IP", "198.51.100.75"); // Nginx gán bằng $remote_addr
        request.addHeader("X-Forwarded-For", "1.1.1.1, 198.51.100.75");

        String clientIp = resolver.resolveClientIp(request);

        assertThat(clientIp).isEqualTo("198.51.100.75");
    }

    @Test
    @DisplayName("IP-03: Kết nối chuyển tiếp từ Nginx trong Docker network (172.20.0.5 thuộc 172.16.0.0/12) -> Nhận diện trusted CIDR")
    void forwardedFromDockerBridgeNetwork_MatchesCidr_UsesXRealIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.20.0.5"); // IP container Nginx trên bridge network
        request.addHeader("X-Real-IP", "42.112.5.10");

        String clientIp = resolver.resolveClientIp(request);

        assertThat(clientIp).isEqualTo("42.112.5.10");
    }

    @Test
    @DisplayName("IP-04: X-Forwarded-For có IP giả mạo ở đầu chuỗi -> Không lấy phần tử đầu tiên một cách mù quáng")
    void xff_ContainsSpoofedIpAtFront_DoesNotUseFirstElement() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1"); // Trusted proxy
        // Không có X-Real-IP, chỉ có X-Forwarded-For: attacker gửi 1.2.3.4, proxy nối thêm 198.51.100.80
        request.addHeader("X-Forwarded-For", "1.2.3.4, 198.51.100.80");

        String clientIp = resolver.resolveClientIp(request);

        // Phải lấy IP cuối cùng hợp lệ không thuộc trusted proxy (198.51.100.80), không lấy 1.2.3.4
        assertThat(clientIp).isEqualTo("198.51.100.80");
    }

    @Test
    @DisplayName("IP-05: IP chứa ký tự đặc biệt nguy hiểm -> Từ chối header giả mạo/chứa mã độc, an toàn tuyệt đối")
    void maliciousIpString_SanitizedSafely() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Real-IP", "192.168.1.10; FLUSHALL\r\n{}");

        String clientIp = resolver.resolveClientIp(request);

        // Header chứa ký tự độc hại không phải IP hợp lệ -> bị từ chối và fallback về remoteAddr an toàn
        assertThat(clientIp).doesNotContain(";").doesNotContain("FLUSHALL").doesNotContain("{").doesNotContain("}");
        assertThat(clientIp).isEqualTo("127.0.0.1");

        // Đồng thời kiểm tra sanitizeIp trực tiếp loại bỏ mọi ký tự lạ
        assertThat(resolver.sanitizeIp("192.168.1.1; bad{}")).doesNotContain(";").doesNotContain("{").doesNotContain("}");
    }

    @Test
    @DisplayName("IP-06: Request null hoặc IP rỗng -> Trả về 'unknown' an toàn")
    void nullOrBlankRequest_ReturnsUnknown() {
        assertThat(resolver.resolveClientIp(null)).isEqualTo("unknown");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("   ");
        assertThat(resolver.resolveClientIp(request)).isEqualTo("unknown");
    }
}
