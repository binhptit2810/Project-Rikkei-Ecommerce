package com.example.ecommerce.util;

import java.math.BigDecimal;

/**
 * Tiện ích chuyển đổi số tiền sang chữ tiếng Việt phục vụ xuất hóa đơn bán hàng.
 * Phụ trách: Đặng Thanh Bình (Thành viên 4 - DTB)
 */
public class VietnameseNumberToWords {

    private static final String[] DIGITS = {
            "không", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín"
    };

    private static final String[] UNITS = {
            "", "nghìn", "triệu", "tỷ", "nghìn tỷ", "triệu tỷ"
    };

    /**
     * Chuyển đổi số tiền BigDecimal sang chuỗi đọc bằng chữ tiếng Việt có đơn vị "đồng chẵn".
     *
     * @param amount Số tiền cần chuyển đổi
     * @return Chuỗi chữ đọc tiếng Việt (ví dụ: "Một triệu năm trăm nghìn đồng chẵn")
     */
    public static String toVietnameseWords(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return "Không đồng chẵn";
        }

        long number = Math.abs(amount.longValue());
        if (number == 0) {
            return "Không đồng chẵn";
        }

        StringBuilder result = new StringBuilder();
        int unitIndex = 0;

        while (number > 0) {
            int group = (int) (number % 1000);
            if (group > 0) {
                String groupText = readThreeDigits(group, number >= 1000);
                String unit = UNITS[unitIndex];
                if (!unit.isEmpty()) {
                    result.insert(0, groupText + " " + unit + " ");
                } else {
                    result.insert(0, groupText + " ");
                }
            }
            number /= 1000;
            unitIndex++;
        }

        String raw = result.toString().trim().replaceAll("\\s+", " ");
        if (raw.isEmpty()) {
            return "Không đồng chẵn";
        }

        // Viết hoa chữ cái đầu tiên
        String capitalized = Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
        return capitalized + " đồng chẵn";
    }

    private static String readThreeDigits(int number, boolean hasHigherGroup) {
        int hundreds = number / 100;
        int tens = (number % 100) / 10;
        int units = number % 10;

        StringBuilder sb = new StringBuilder();

        if (hundreds > 0 || hasHigherGroup) {
            sb.append(DIGITS[hundreds]).append(" trăm ");
        }

        if (tens > 1) {
            sb.append(DIGITS[tens]).append(" mươi ");
            if (units == 1) {
                sb.append("mốt");
            } else if (units == 4) {
                sb.append("tư");
            } else if (units == 5) {
                sb.append("lăm");
            } else if (units > 0) {
                sb.append(DIGITS[units]);
            }
        } else if (tens == 1) {
            sb.append("mười ");
            if (units == 5) {
                sb.append("lăm");
            } else if (units > 0) {
                sb.append(DIGITS[units]);
            }
        } else { // tens == 0
            if (units > 0) {
                if (hundreds > 0 || hasHigherGroup) {
                    sb.append("lẻ ");
                }
                sb.append(DIGITS[units]);
            }
        }

        return sb.toString().trim();
    }
}

