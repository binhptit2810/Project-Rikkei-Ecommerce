package com.example.ecommerce.util;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class VNPayUtilTest {

    private static final String TEST_SECRET = "RAIQUIBGCARJHNLKZCPGUCUMRHQKGZDF";
    private static final String TEST_PAY_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";

    @Test
    @DisplayName("hmacSHA512 trả về chuỗi băm hex 128 ký tự chữ thường chuẩn RFC")
    void hmacSHA512_success() {
        String data = "vnp_Amount=10000000&vnp_Command=pay&vnp_TmnCode=2QXUI4J4";
        String hash = VNPayUtil.hmacSHA512(TEST_SECRET, data);

        assertThat(hash).isNotNull().isNotEmpty();
        assertThat(hash).hasSize(128);
        assertThat(hash).matches("^[a-f0-9]+$");
    }

    @Test
    @DisplayName("hashAllFields tự động sắp xếp tham số alphabet và băm chính xác")
    void hashAllFields_sortsAlphabetically() {
        Map<String, String> unorderedParams = new HashMap<>();
        unorderedParams.put("vnp_TmnCode", "2QXUI4J4");
        unorderedParams.put("vnp_Command", "pay");
        unorderedParams.put("vnp_Amount", "10000000");

        String hash1 = VNPayUtil.hashAllFields(unorderedParams, TEST_SECRET);

        // Map có thứ tự khác
        Map<String, String> orderedParams = new HashMap<>();
        orderedParams.put("vnp_Amount", "10000000");
        orderedParams.put("vnp_Command", "pay");
        orderedParams.put("vnp_TmnCode", "2QXUI4J4");

        String hash2 = VNPayUtil.hashAllFields(orderedParams, TEST_SECRET);

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("buildPaymentUrl tạo URL chuẩn chứa đủ các tham số và vnp_SecureHash")
    void buildPaymentUrl_success() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", "2QXUI4J4");
        params.put("vnp_Amount", "10000000");
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", "PAY-20261109-TEST0001");
        params.put("vnp_OrderInfo", "Thanh toan don hang #1001");

        String url = VNPayUtil.buildPaymentUrl(params, TEST_PAY_URL, TEST_SECRET);

        assertThat(url).startsWith(TEST_PAY_URL + "?");
        assertThat(url).contains("vnp_TmnCode=2QXUI4J4");
        assertThat(url).contains("vnp_Amount=10000000");
        assertThat(url).contains("vnp_TxnRef=PAY-20261109-TEST0001");
        assertThat(url).contains("vnp_SecureHash=");
    }

    @Test
    @DisplayName("verifyCallback trả về true khi chữ ký khớp và false khi dữ liệu bị sửa đổi")
    void verifyCallback_validation() {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("vnp_Amount", "10000000");
        queryParams.put("vnp_BankCode", "NCB");
        queryParams.put("vnp_ResponseCode", "00");
        queryParams.put("vnp_TxnRef", "PAY-20261109-TEST0001");

        String validHash = VNPayUtil.hashAllFields(queryParams, TEST_SECRET);
        queryParams.put("vnp_SecureHash", validHash);

        assertThat(VNPayUtil.verifyCallback(queryParams, TEST_SECRET)).isTrue();

        // Thử sửa đổi số tiền
        queryParams.put("vnp_Amount", "20000000");
        assertThat(VNPayUtil.verifyCallback(queryParams, TEST_SECRET)).isFalse();
    }

    @Test
    @DisplayName("verifyCallback: Thiếu vnp_SecureHash trả về false")
    void verifyCallback_missingHash_returnsFalse() {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("vnp_Amount", "10000000");
        queryParams.put("vnp_ResponseCode", "00");

        assertThat(VNPayUtil.verifyCallback(queryParams, TEST_SECRET)).isFalse();

        queryParams.put("vnp_SecureHash", "   ");
        assertThat(VNPayUtil.verifyCallback(queryParams, TEST_SECRET)).isFalse();
    }

    @Test
    @DisplayName("verifyCallback: Chữ ký sai định dạng hex 128 ký tự trả về false")
    void verifyCallback_malformedHash_returnsFalse() {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("vnp_Amount", "10000000");
        queryParams.put("vnp_ResponseCode", "00");
        queryParams.put("vnp_SecureHash", "short-invalid-hash");

        assertThat(VNPayUtil.verifyCallback(queryParams, TEST_SECRET)).isFalse();
    }

    @Test
    @DisplayName("verifyCallback: Chữ ký viết hoa vẫn được xác thực hợp lệ qua MessageDigest.isEqual")
    void verifyCallback_uppercaseHash_returnsTrue() {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("vnp_Amount", "10000000");
        queryParams.put("vnp_BankCode", "NCB");
        queryParams.put("vnp_ResponseCode", "00");
        queryParams.put("vnp_TxnRef", "PAY-20261109-TEST0001");

        String validHash = VNPayUtil.hashAllFields(queryParams, TEST_SECRET);
        queryParams.put("vnp_SecureHash", validHash.toUpperCase());

        assertThat(VNPayUtil.verifyCallback(queryParams, TEST_SECRET)).isTrue();
    }

    @Test
    @DisplayName("verifyCallback: Bỏ qua tham số rỗng và null trong canonicalization")
    void verifyCallback_ignoresEmptyParams() {
        Map<String, String> queryParamsWithEmpty = new HashMap<>();
        queryParamsWithEmpty.put("vnp_Amount", "10000000");
        queryParamsWithEmpty.put("vnp_BankCode", "");
        queryParamsWithEmpty.put("vnp_BankTranNo", "   ");
        queryParamsWithEmpty.put("vnp_ResponseCode", "00");

        Map<String, String> queryParamsClean = new HashMap<>();
        queryParamsClean.put("vnp_Amount", "10000000");
        queryParamsClean.put("vnp_ResponseCode", "00");

        String hash1 = VNPayUtil.hashAllFields(queryParamsWithEmpty, TEST_SECRET);
        String hash2 = VNPayUtil.hashAllFields(queryParamsClean, TEST_SECRET);

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("hasDuplicateVnpParams: Phát hiện tham số trùng tên (Parameter Pollution - HPP)")
    void hasDuplicateVnpParams_detection() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        // Trường hợp không có tham số trùng
        Map<String, String[]> normalParams = new HashMap<>();
        normalParams.put("vnp_Amount", new String[]{"10000000"});
        normalParams.put("vnp_TxnRef", new String[]{"PAY-001"});
        when(request.getParameterMap()).thenReturn(normalParams);
        assertThat(VNPayUtil.hasDuplicateVnpParams(request)).isFalse();

        // Trường hợp có 2 tham số vnp_Amount
        Map<String, String[]> pollutedParams = new HashMap<>();
        pollutedParams.put("vnp_Amount", new String[]{"10000000", "20000000"});
        pollutedParams.put("vnp_TxnRef", new String[]{"PAY-001"});
        when(request.getParameterMap()).thenReturn(pollutedParams);
        assertThat(VNPayUtil.hasDuplicateVnpParams(request)).isTrue();
    }

    @Test
    @DisplayName("verifyCallback: Khóa bí mật null hoặc queryParams rỗng trả về false")
    void verifyCallback_nullSafety() {
        assertThat(VNPayUtil.verifyCallback(null, TEST_SECRET)).isFalse();
        assertThat(VNPayUtil.verifyCallback(Map.of(), TEST_SECRET)).isFalse();
        assertThat(VNPayUtil.verifyCallback(Map.of("vnp_SecureHash", "abc"), null)).isFalse();
        assertThat(VNPayUtil.verifyCallback(Map.of("vnp_SecureHash", "abc"), "  ")).isFalse();
    }

    @Test
    @DisplayName("getIpAddress xử lý đúng localhost và X-Forwarded-For")
    void getIpAddress_handling() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        when(request.getHeader("X-Forwarded-For")).thenReturn("203.113.152.1, 10.0.0.1");
        assertThat(VNPayUtil.getIpAddress(request)).isEqualTo("203.113.152.1");

        when(request.getHeader("X-Forwarded-For")).thenReturn("0:0:0:0:0:0:0:1");
        assertThat(VNPayUtil.getIpAddress(request)).isEqualTo("127.0.0.1");
    }
}
