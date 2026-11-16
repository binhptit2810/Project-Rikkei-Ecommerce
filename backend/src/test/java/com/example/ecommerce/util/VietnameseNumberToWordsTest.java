package com.example.ecommerce.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit Test kiểm thử tiện ích chuyển đổi số tiền sang chữ tiếng Việt (VietnameseNumberToWords).
 * Phục vụ nghiệp vụ xuất hóa đơn PDF chuyên nghiệp [TSK-D16-DHA].
 */
class VietnameseNumberToWordsTest {

    @Test
    @DisplayName("toVietnameseWords: null hoặc 0 trả về Không đồng chẵn")
    void testNullOrZero() {
        assertThat(VietnameseNumberToWords.toVietnameseWords(null)).isEqualTo("Không đồng chẵn");
        assertThat(VietnameseNumberToWords.toVietnameseWords(BigDecimal.ZERO)).isEqualTo("Không đồng chẵn");
        assertThat(VietnameseNumberToWords.toVietnameseWords(BigDecimal.valueOf(0))).isEqualTo("Không đồng chẵn");
    }

    @Test
    @DisplayName("toVietnameseWords: Số âm được chuyển thành giá trị dương tuyệt đối")
    void testNegativeNumbers() {
        assertThat(VietnameseNumberToWords.toVietnameseWords(BigDecimal.valueOf(-50000)))
                .isEqualTo("Năm mươi nghìn đồng chẵn");
    }

    @Test
    @DisplayName("toVietnameseWords: Đọc các trường hợp đặc biệt của hàng đơn vị (mốt, tư, lăm, lẻ)")
    void testSpecialUnitsAndTens() {
        // units == 1 kết hợp tens > 1 (mốt)
        assertThat(VietnameseNumberToWords.toVietnameseWords(BigDecimal.valueOf(21)))
                .isEqualTo("Hai mươi mốt đồng chẵn");
        assertThat(VietnameseNumberToWords.toVietnameseWords(BigDecimal.valueOf(121)))
                .isEqualTo("Một trăm hai mươi mốt đồng chẵn");

        // units == 4 kết hợp tens > 1 (tư)
        assertThat(VietnameseNumberToWords.toVietnameseWords(BigDecimal.valueOf(24)))
                .isEqualTo("Hai mươi tư đồng chẵn");
        assertThat(VietnameseNumberToWords.toVietnameseWords(BigDecimal.valueOf(124)))
                .isEqualTo("Một trăm hai mươi tư đồng chẵn");

        // units == 5 kết hợp tens > 1 (lăm)
        assertThat(VietnameseNumberToWords.toVietnameseWords(BigDecimal.valueOf(25)))
                .isEqualTo("Hai mươi lăm đồng chẵn");
        assertThat(VietnameseNumberToWords.toVietnameseWords(BigDecimal.valueOf(125)))
                .isEqualTo("Một trăm hai mươi lăm đồng chẵn");

        // units == 5 kết hợp tens == 1 (mười lăm)
        assertThat(VietnameseNumberToWords.toVietnameseWords(BigDecimal.valueOf(15)))
                .isEqualTo("Mười lăm đồng chẵn");
        assertThat(VietnameseNumberToWords.toVietnameseWords(BigDecimal.valueOf(115)))
                .isEqualTo("Một trăm mười lăm đồng chẵn");

        // units == 4 kết hợp tens == 1 (mười bốn)
        assertThat(VietnameseNumberToWords.toVietnameseWords(BigDecimal.valueOf(14)))
                .isEqualTo("Mười bốn đồng chẵn");

        // units == 1 kết hợp tens == 1 (mười một)
        assertThat(VietnameseNumberToWords.toVietnameseWords(BigDecimal.valueOf(11)))
                .isEqualTo("Mười một đồng chẵn");

        // tens == 0, units > 0 (lẻ)
        assertThat(VietnameseNumberToWords.toVietnameseWords(BigDecimal.valueOf(105)))
                .isEqualTo("Một trăm lẻ năm đồng chẵn");
        assertThat(VietnameseNumberToWords.toVietnameseWords(BigDecimal.valueOf(101)))
                .isEqualTo("Một trăm lẻ một đồng chẵn");
        assertThat(VietnameseNumberToWords.toVietnameseWords(BigDecimal.valueOf(107)))
                .isEqualTo("Một trăm lẻ bảy đồng chẵn");
        assertThat(VietnameseNumberToWords.toVietnameseWords(BigDecimal.valueOf(5)))
                .isEqualTo("Năm đồng chẵn");
    }

    @Test
    @DisplayName("toVietnameseWords: Đọc các mệnh giá lớn nghìn, triệu, tỷ, nghìn tỷ")
    void testLargeUnits() {
        assertThat(VietnameseNumberToWords.toVietnameseWords(BigDecimal.valueOf(1000)))
                .isEqualTo("Một nghìn đồng chẵn");
        assertThat(VietnameseNumberToWords.toVietnameseWords(BigDecimal.valueOf(1000000)))
                .isEqualTo("Một triệu đồng chẵn");
        assertThat(VietnameseNumberToWords.toVietnameseWords(BigDecimal.valueOf(1500000)))
                .isEqualTo("Một triệu năm trăm nghìn đồng chẵn");
        assertThat(VietnameseNumberToWords.toVietnameseWords(BigDecimal.valueOf(1000000000L)))
                .isEqualTo("Một tỷ đồng chẵn");
        assertThat(VietnameseNumberToWords.toVietnameseWords(BigDecimal.valueOf(2500000000L)))
                .isEqualTo("Hai tỷ năm trăm triệu đồng chẵn");
        assertThat(VietnameseNumberToWords.toVietnameseWords(BigDecimal.valueOf(1000000000000L)))
                .isEqualTo("Một nghìn tỷ đồng chẵn");
    }

    @Test
    @DisplayName("Constructor invocation coverage")
    void testConstructor() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Constructor<VietnameseNumberToWords> constructor = VietnameseNumberToWords.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        VietnameseNumberToWords instance = constructor.newInstance();
        assertThat(instance).isNotNull();
    }
}
