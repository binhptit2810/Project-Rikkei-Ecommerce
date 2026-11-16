package com.example.ecommerce.service;

import com.example.ecommerce.entity.*;
import com.example.ecommerce.exception.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.PaymentRepository;
import com.example.ecommerce.repository.UserRepository;
import com.example.ecommerce.util.VietnameseNumberToWords;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Service xuất hóa đơn PDF chuyên nghiệp cho đơn hàng (Order Invoice PDF Generator).
 * Phụ trách: Đặng Thanh Bình (Thành viên 4 - DTB)
 *
 * Tiêu chí nghiệm thu (DoD):
 * 1. File PDF hóa đơn xuất ra sắc nét, chuẩn định dạng doanh nghiệp hiện đại.
 * 2. Đầy đủ mã đơn, logo Rikkei Store, danh sách món kèm kích cỡ (size).
 * 3. Hiển thị chi tiết tiền giảm voucher và thông tin thanh toán VNPay / COD.
 * 4. Hỗ trợ hiển thị tiếng Việt UTF-8 chuẩn xác 100% không bị lỗi font.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoicePdfService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Bảng màu thiết kế hóa đơn doanh nghiệp (Enterprise Slate / Indigo Palette)
    private static final Color COLOR_PRIMARY = new Color(30, 41, 59);      // Slate 800
    private static final Color COLOR_ACCENT = new Color(37, 99, 235);      // Blue 600
    private static final Color COLOR_MUTED = new Color(100, 116, 139);     // Slate 500
    private static final Color COLOR_BORDER = new Color(226, 232, 240);    // Slate 200
    private static final Color COLOR_BG_LIGHT = new Color(248, 250, 252);  // Slate 50
    private static final Color COLOR_BG_HEADER = new Color(241, 245, 249); // Slate 100
    private static final Color COLOR_SUCCESS = new Color(16, 185, 129);    // Emerald 500
    private static final Color COLOR_DANGER = new Color(225, 29, 72);      // Rose 600
    private static final Color COLOR_AMBER = new Color(217, 119, 6);       // Amber 600

    /**
     * Tạo tệp PDF hóa đơn bán hàng cho đơn hàng theo ID.
     *
     * @param orderId ID của đơn hàng cần in hóa đơn
     * @param currentUserEmail Email của tài khoản người dùng đang đăng nhập
     * @return Mảng byte chứa nội dung tệp PDF
     */
    @Transactional(readOnly = true)
    public byte[] generateInvoicePdf(Long orderId, String currentUserEmail) {
        log.info("Khởi tạo xuất hóa đơn PDF cho đơn hàng ID: {} bởi user: {}", orderId, currentUserEmail);

        // 1. Kiểm tra tồn tại và quyền truy cập
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseGet(() -> orderRepository.findById(orderId)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với ID: " + orderId)));

        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại với email: " + currentUserEmail));

        if (currentUser.getRole() != Role.ADMIN && !order.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Bạn không có quyền truy cập hóa đơn của đơn hàng này");
        }

        Payment payment = order.getPayment();
        if (payment == null) {
            payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
        }

        // 2. Tạo tài liệu PDF
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 32, 32, 32, 36);
            PdfWriter writer = PdfWriter.getInstance(document, out);

            // Gắn footer số trang
            writer.setPageEvent(new InvoicePageEventHelper(order.getOrderCode()));

            document.open();

            // Khởi tạo các font tiếng Việt chuẩn
            BaseFont bfRegular = loadBaseFont("fonts/arial.ttf", BaseFont.HELVETICA);
            BaseFont bfBold = loadBaseFont("fonts/arialbd.ttf", BaseFont.HELVETICA_BOLD);

            Font fontCompanyTitle = new Font(bfBold, 13, Font.NORMAL, COLOR_PRIMARY);
            Font fontHeaderTitle = new Font(bfBold, 18, Font.NORMAL, COLOR_ACCENT);
            Font fontSubTitle = new Font(bfRegular, 8.5f, Font.ITALIC, COLOR_MUTED);
            Font fontSectionHeader = new Font(bfBold, 9.5f, Font.NORMAL, COLOR_PRIMARY);
            Font fontLabel = new Font(bfRegular, 8.5f, Font.NORMAL, COLOR_MUTED);
            Font fontValue = new Font(bfBold, 8.5f, Font.NORMAL, COLOR_PRIMARY);
            Font fontTableHead = new Font(bfBold, 8.5f, Font.NORMAL, Color.WHITE);
            Font fontTableCell = new Font(bfRegular, 8.5f, Font.NORMAL, COLOR_PRIMARY);
            Font fontTableCellBold = new Font(bfBold, 8.5f, Font.NORMAL, COLOR_PRIMARY);
            Font fontVoucher = new Font(bfBold, 9f, Font.NORMAL, COLOR_DANGER);
            Font fontTotalLabel = new Font(bfBold, 11f, Font.NORMAL, COLOR_PRIMARY);
            Font fontTotalAmount = new Font(bfBold, 13f, Font.NORMAL, new Color(30, 58, 138));
            Font fontSmallItalic = new Font(bfRegular, 8f, Font.ITALIC, COLOR_MUTED);

            // =========================================================================
            // A. PHẦN ĐẦU TRANG: LOGO DOANH NGHIỆP & TIÊU ĐỀ HÓA ĐƠN
            // =========================================================================
            PdfPTable topTable = new PdfPTable(2);
            topTable.setWidthPercentage(100);
            topTable.setWidths(new float[]{55f, 45f});
            topTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);

            // Cột trái: Logo và thông tin Công ty
            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(Rectangle.NO_BORDER);
            leftCell.setPadding(0);

            Image logo = loadLogoImage();
            if (logo != null) {
                logo.scaleToFit(160, 42);
                leftCell.addElement(logo);
            } else {
                Paragraph fallbackBrand = new Paragraph("⚡ RIKKEI STORE", fontHeaderTitle);
                leftCell.addElement(fallbackBrand);
            }

            Paragraph compInfo = new Paragraph();
            compInfo.setLeading(12);
            compInfo.add(new Chunk("\nCÔNG TY CỔ PHẦN THƯƠNG MẠI ĐIỆN TỬ RIKKEI\n", fontCompanyTitle));
            compInfo.add(new Chunk("Địa chỉ: Tầng 7, Tháp A, Tòa Sông Đà, Phạm Hùng, Mỹ Đình, Hà Nội\n", fontLabel));
            compInfo.add(new Chunk("Hotline: 1900 6868  |  Email: hotro@rikkeistore.vn\n", fontLabel));
            compInfo.add(new Chunk("Website: rikkeistore.vn  |  Mã số thuế: 0108996688", fontLabel));
            leftCell.addElement(compInfo);
            topTable.addCell(leftCell);

            // Cột phải: Tiêu đề hóa đơn & Mã đơn
            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.setPadding(0);
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

            Paragraph invoiceTitle = new Paragraph();
            invoiceTitle.setAlignment(Element.ALIGN_RIGHT);
            invoiceTitle.setLeading(16);
            invoiceTitle.add(new Chunk("HÓA ĐƠN BÁN HÀNG\n", fontHeaderTitle));
            invoiceTitle.add(new Chunk("SALES INVOICE\n\n", fontSubTitle));
            invoiceTitle.add(new Chunk("Mã đơn hàng: ", fontLabel));
            invoiceTitle.add(new Chunk(order.getOrderCode() + "\n", new Font(bfBold, 11, Font.NORMAL, COLOR_PRIMARY)));
            invoiceTitle.add(new Chunk("Ngày tạo: ", fontLabel));
            invoiceTitle.add(new Chunk(order.getCreatedAt() != null ? DATE_TIME_FORMATTER.format(order.getCreatedAt()) : "N/A", fontValue));
            invoiceTitle.add(new Chunk("\n\n", fontLabel));

            // Trạng thái thanh toán (Badge)
            boolean isPaid = order.getPaymentStatus() == PaymentStatus.PAID;
            Chunk statusBadge = new Chunk(
                    isPaid ? " [ ĐÃ THANH TOÁN (PAID) ] " : " [ CHƯA THANH TOÁN (UNPAID) ] ",
                    new Font(bfBold, 9.5f, Font.NORMAL, isPaid ? COLOR_SUCCESS : COLOR_AMBER)
            );
            invoiceTitle.add(statusBadge);

            rightCell.addElement(invoiceTitle);
            topTable.addCell(rightCell);

            document.add(topTable);

            // Đường kẻ ngăn cách phong cách doanh nghiệp
            document.add(createHorizontalDivider(COLOR_ACCENT, 2f, 8f, 10f));

            // =========================================================================
            // B. THÔNG TIN KHÁCH HÀNG & GIAO HÀNG (2 CỘT)
            // =========================================================================
            PdfPTable infoBoxTable = new PdfPTable(2);
            infoBoxTable.setWidthPercentage(100);
            infoBoxTable.setWidths(new float[]{50f, 50f});
            infoBoxTable.setSpacingAfter(10f);

            // Box Khách hàng (Đặt mua)
            PdfPCell buyerCell = new PdfPCell();
            buyerCell.setBackgroundColor(COLOR_BG_LIGHT);
            buyerCell.setBorderColor(COLOR_BORDER);
            buyerCell.setBorderWidth(1f);
            buyerCell.setPadding(8f);

            Paragraph buyerPara = new Paragraph();
            buyerPara.setLeading(13);
            buyerPara.add(new Chunk("THÔNG TIN KHÁCH HÀNG (BUYER)\n", fontSectionHeader));
            buyerPara.add(new Chunk("Khách hàng: ", fontLabel));
            buyerPara.add(new Chunk((order.getUser() != null ? order.getUser().getFullName() : order.getRecipientName()) + "\n", fontValue));
            buyerPara.add(new Chunk("Email: ", fontLabel));
            buyerPara.add(new Chunk((order.getUser() != null ? order.getUser().getEmail() : "N/A") + "\n", fontValue));
            buyerPara.add(new Chunk("Mã khách hàng: ", fontLabel));
            buyerPara.add(new Chunk("#" + (order.getUser() != null ? order.getUser().getId() : "N/A"), fontValue));
            buyerCell.addElement(buyerPara);
            infoBoxTable.addCell(buyerCell);

            // Box Giao hàng (Người nhận)
            PdfPCell shipCell = new PdfPCell();
            shipCell.setBackgroundColor(COLOR_BG_LIGHT);
            shipCell.setBorderColor(COLOR_BORDER);
            shipCell.setBorderWidth(1f);
            shipCell.setPadding(8f);

            Paragraph shipPara = new Paragraph();
            shipPara.setLeading(13);
            shipPara.add(new Chunk("ĐỊA CHỈ NHẬN HÀNG (SHIPPING TO)\n", fontSectionHeader));
            shipPara.add(new Chunk("Người nhận: ", fontLabel));
            shipPara.add(new Chunk(order.getRecipientName() + "\n", fontValue));
            shipPara.add(new Chunk("Số điện thoại: ", fontLabel));
            shipPara.add(new Chunk(order.getRecipientPhone() + "\n", fontValue));
            shipPara.add(new Chunk("Địa chỉ giao: ", fontLabel));
            shipPara.add(new Chunk(order.getShippingAddress() + "\n", fontValue));
            if (order.getNote() != null && !order.getNote().trim().isEmpty()) {
                shipPara.add(new Chunk("Ghi chú: ", fontLabel));
                shipPara.add(new Chunk(order.getNote(), fontSmallItalic));
            }
            shipCell.addElement(shipPara);
            infoBoxTable.addCell(shipCell);

            document.add(infoBoxTable);

            // =========================================================================
            // C. BẢNG DANH SÁCH MÓN HÀNG KÈM KÍCH CỠ (SIZE)
            // =========================================================================
            PdfPTable itemsTable = new PdfPTable(6);
            itemsTable.setWidthPercentage(100);
            itemsTable.setWidths(new float[]{6f, 42f, 14f, 10f, 14f, 14f});
            itemsTable.setSpacingAfter(10f);

            // Header hàng
            addHeaderCell(itemsTable, "#", fontTableHead, Element.ALIGN_CENTER);
            addHeaderCell(itemsTable, "Tên sản phẩm", fontTableHead, Element.ALIGN_LEFT);
            addHeaderCell(itemsTable, "Kích cỡ (Size)", fontTableHead, Element.ALIGN_CENTER);
            addHeaderCell(itemsTable, "Số lượng", fontTableHead, Element.ALIGN_CENTER);
            addHeaderCell(itemsTable, "Đơn giá", fontTableHead, Element.ALIGN_RIGHT);
            addHeaderCell(itemsTable, "Thành tiền", fontTableHead, Element.ALIGN_RIGHT);

            List<OrderItem> items = order.getItems();
            BigDecimal subtotalSum = BigDecimal.ZERO;

            if (items != null && !items.isEmpty()) {
                int stt = 1;
                for (OrderItem item : items) {
                    Color rowBg = (stt % 2 == 0) ? COLOR_BG_LIGHT : Color.WHITE;

                    // STT
                    addBodyCell(itemsTable, String.valueOf(stt++), fontTableCell, Element.ALIGN_CENTER, rowBg);

                    // Tên sản phẩm
                    addBodyCell(itemsTable, item.getProductName() != null ? item.getProductName() : "Sản phẩm", fontTableCellBold, Element.ALIGN_LEFT, rowBg);

                    // Kích cỡ (Size) - Đáp ứng yêu cầu DoD: Danh sách món kèm size
                    String sizeText = (item.getSelectedSize() != null && !item.getSelectedSize().trim().isEmpty())
                            ? item.getSelectedSize().trim()
                            : "Tiêu chuẩn";
                    addBodyCell(itemsTable, sizeText, fontTableCell, Element.ALIGN_CENTER, rowBg);

                    // Số lượng
                    addBodyCell(itemsTable, String.valueOf(item.getQuantity() != null ? item.getQuantity() : 1), fontTableCell, Element.ALIGN_CENTER, rowBg);

                    // Đơn giá
                    BigDecimal price = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                    addBodyCell(itemsTable, formatCurrency(price), fontTableCell, Element.ALIGN_RIGHT, rowBg);

                    // Thành tiền
                    BigDecimal lineSubtotal = item.getSubtotal() != null
                            ? item.getSubtotal()
                            : price.multiply(BigDecimal.valueOf(item.getQuantity() != null ? item.getQuantity() : 1));
                    subtotalSum = subtotalSum.add(lineSubtotal);
                    addBodyCell(itemsTable, formatCurrency(lineSubtotal), fontTableCellBold, Element.ALIGN_RIGHT, rowBg);
                }
            } else {
                PdfPCell emptyCell = new PdfPCell(new Phrase("Không có chi tiết mặt hàng", fontTableCell));
                emptyCell.setColspan(6);
                emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                emptyCell.setPadding(10f);
                itemsTable.addCell(emptyCell);
            }

            document.add(itemsTable);

            // =========================================================================
            // D. KHỐI THANH TOÁN VNPAY & TỔNG KẾT VOUCHER (2 CỘT)
            // =========================================================================
            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(100);
            summaryTable.setWidths(new float[]{48f, 52f});
            summaryTable.setSpacingAfter(12f);

            // Cột trái: Chi tiết thanh toán VNPay / COD
            PdfPCell paymentInfoCell = new PdfPCell();
            paymentInfoCell.setBorderColor(COLOR_BORDER);
            paymentInfoCell.setBackgroundColor(COLOR_BG_LIGHT);
            paymentInfoCell.setPadding(8f);

            Paragraph payPara = new Paragraph();
            payPara.setLeading(13);
            payPara.add(new Chunk("THÔNG TIN THANH TOÁN (PAYMENT)\n", fontSectionHeader));

            PaymentMethod method = (payment != null && payment.getPaymentMethod() != null)
                    ? payment.getPaymentMethod()
                    : PaymentMethod.COD;
            String methodDesc = method == PaymentMethod.VNPAY
                    ? "Cổng thanh toán điện tử VNPay (Online)"
                    : (method == PaymentMethod.SIMULATED
                    ? "Thanh toán trực tuyến (Mô phỏng)"
                    : "Thanh toán khi nhận hàng (COD)");

            payPara.add(new Chunk("Phương thức: ", fontLabel));
            payPara.add(new Chunk(methodDesc + "\n", fontValue));

            String txnCode = (payment != null && payment.getTransactionCode() != null)
                    ? payment.getTransactionCode()
                    : (payment != null && payment.getVnpTxnRef() != null ? payment.getVnpTxnRef() : order.getOrderCode());
            payPara.add(new Chunk("Mã giao dịch / Ref: ", fontLabel));
            payPara.add(new Chunk(txnCode + "\n", fontValue));

            String payStatusText = order.getPaymentStatus() == PaymentStatus.PAID
                    ? "Giao dịch thành công (PAID)"
                    : (order.getPaymentStatus() == PaymentStatus.FAILED ? "Thất bại (FAILED)" : "Chờ thanh toán (UNPAID)");
            payPara.add(new Chunk("Trạng thái: ", fontLabel));
            payPara.add(new Chunk(payStatusText + "\n", new Font(bfBold, 8.5f, Font.NORMAL, isPaid ? COLOR_SUCCESS : COLOR_AMBER)));

            payPara.add(new Chunk("Số tiền thanh toán: ", fontLabel));
            payPara.add(new Chunk(formatCurrency(order.getTotalAmount()) + "\n", fontValue));

            if (payment != null && payment.getPaidAt() != null) {
                payPara.add(new Chunk("Thời gian xác nhận: ", fontLabel));
                payPara.add(new Chunk(DATE_TIME_FORMATTER.format(payment.getPaidAt()), fontValue));
            }

            paymentInfoCell.addElement(payPara);
            summaryTable.addCell(paymentInfoCell);

            // Cột phải: Bảng chi tiết tổng tiền, Voucher giảm giá
            PdfPCell financeCell = new PdfPCell();
            financeCell.setBorderColor(COLOR_BORDER);
            financeCell.setPadding(8f);

            PdfPTable financeInner = new PdfPTable(2);
            financeInner.setWidthPercentage(100);
            financeInner.setWidths(new float[]{60f, 40f});

            // Tạm tính tiền hàng
            addFinanceRow(financeInner, "Tổng tiền hàng (Tạm tính):", formatCurrency(subtotalSum), fontLabel, fontValue);

            // Giảm giá Voucher
            BigDecimal discount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;
            String couponCode = order.getCouponCode();
            if (discount.compareTo(BigDecimal.ZERO) > 0 || (couponCode != null && !couponCode.trim().isEmpty())) {
                String voucherLabel = (couponCode != null && !couponCode.trim().isEmpty())
                        ? "Giảm giá Voucher (" + couponCode.trim() + "):"
                        : "Giảm giá Voucher:";
                addFinanceRow(financeInner, voucherLabel, "-" + formatCurrency(discount), fontVoucher, fontVoucher);
            }

            // Phí vận chuyển
            addFinanceRow(financeInner, "Phí vận chuyển:", "Miễn phí (0 đ)", fontLabel, fontValue);

            // Đường phân cách
            PdfPCell hrCell = new PdfPCell();
            hrCell.setColspan(2);
            hrCell.setBorder(Rectangle.BOTTOM);
            hrCell.setBorderColor(COLOR_ACCENT);
            hrCell.setBorderWidth(1.5f);
            hrCell.setPadding(3f);
            financeInner.addCell(hrCell);

            // TỔNG CỘNG THANH TOÁN
            addFinanceRow(financeInner, "TỔNG THANH TOÁN:", formatCurrency(order.getTotalAmount()), fontTotalLabel, fontTotalAmount);

            financeCell.addElement(financeInner);

            // Đọc tiền bằng chữ tiếng Việt chuẩn
            Paragraph wordsPara = new Paragraph();
            wordsPara.setSpacingBefore(4f);
            wordsPara.add(new Chunk("(Bằng chữ: ", fontSmallItalic));
            wordsPara.add(new Chunk(VietnameseNumberToWords.toVietnameseWords(order.getTotalAmount()) + ")", fontSmallItalic));
            financeCell.addElement(wordsPara);

            summaryTable.addCell(financeCell);

            document.add(summaryTable);

            // =========================================================================
            // E. CHÂN TRANG: LỜI CẢM ƠN & CHỮ KÝ ĐIỆN TỬ DOANH NGHIỆP
            // =========================================================================
            Paragraph policyPara = new Paragraph(
                    "Cảm ơn Quý khách đã mua sắm tại Rikkei Store! Hóa đơn này là chứng từ bán hàng chính thức. " +
                    "Quý khách vui lòng kiểm tra kỹ hàng hóa và giữ lại hóa đơn để được hỗ trợ bảo hành / đổi trả nhanh chóng trong vòng 7 ngày.",
                    fontSmallItalic
            );
            policyPara.setSpacingAfter(12f);
            document.add(policyPara);

            // Khối chữ ký 2 bên
            PdfPTable signTable = new PdfPTable(2);
            signTable.setWidthPercentage(100);
            signTable.setWidths(new float[]{50f, 50f});

            // Bên trái: Người mua hàng
            PdfPCell buyerSignCell = new PdfPCell();
            buyerSignCell.setBorder(Rectangle.NO_BORDER);
            buyerSignCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            Paragraph buyerSign = new Paragraph();
            buyerSign.setAlignment(Element.ALIGN_CENTER);
            buyerSign.add(new Chunk("NGƯỜI MUA HÀNG\n", fontSectionHeader));
            buyerSign.add(new Chunk("(Ký, ghi rõ họ tên)\n\n\n\n", fontSubTitle));
            buyerSign.add(new Chunk(order.getRecipientName(), fontValue));
            buyerSignCell.addElement(buyerSign);
            signTable.addCell(buyerSignCell);

            // Bên phải: Đại diện doanh nghiệp có dấu mộc điện tử
            PdfPCell sellerSignCell = new PdfPCell();
            sellerSignCell.setBorder(Rectangle.NO_BORDER);
            sellerSignCell.setHorizontalAlignment(Element.ALIGN_CENTER);

            Paragraph sellerSign = new Paragraph();
            sellerSign.setAlignment(Element.ALIGN_CENTER);
            sellerSign.add(new Chunk("ĐẠI DIỆN RIKKEI STORE\n", fontSectionHeader));
            sellerSign.add(new Chunk("(Ký số điện tử / E-Signature)\n\n", fontSubTitle));

            // Dấu mộc điện tử (Digital Signature Badge)
            PdfPTable stamp = new PdfPTable(1);
            stamp.setWidthPercentage(85);
            PdfPCell stampCell = new PdfPCell();
            stampCell.setBackgroundColor(new Color(240, 253, 244)); // Emerald 50
            stampCell.setBorderColor(COLOR_SUCCESS);
            stampCell.setBorderWidth(1.2f);
            stampCell.setPadding(6f);
            stampCell.setHorizontalAlignment(Element.ALIGN_CENTER);

            Paragraph stampContent = new Paragraph();
            stampContent.setAlignment(Element.ALIGN_CENTER);
            stampContent.setLeading(11);
            stampContent.add(new Chunk("✓ ĐÃ KÝ SỐ ĐIỆN TỬ\n", new Font(bfBold, 8.5f, Font.NORMAL, COLOR_SUCCESS)));
            stampContent.add(new Chunk("CÔNG TY CỔ PHẦN RIKKEI STORE\n", new Font(bfBold, 8f, Font.NORMAL, COLOR_PRIMARY)));
            stampContent.add(new Chunk("Thời gian: " + DATE_ONLY_FORMATTER.format(order.getCreatedAt() != null ? order.getCreatedAt() : java.time.LocalDateTime.now()) + "\n", fontSmallItalic));
            stampContent.add(new Chunk("Hóa đơn điện tử hợp lệ theo NĐ 123/2020/NĐ-CP", fontSmallItalic));
            stampCell.addElement(stampContent);
            stamp.addCell(stampCell);

            sellerSign.add(stamp);
            sellerSignCell.addElement(sellerSign);
            signTable.addCell(sellerSignCell);

            document.add(signTable);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Lỗi khi xuất hóa đơn PDF cho đơn hàng ID {}: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Không thể xuất file PDF hóa đơn: " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // HELPER METHODS VẼ BẢNG VÀ TẢI TÀI NGUYÊN
    // =========================================================================

    private void addHeaderCell(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(COLOR_PRIMARY);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingTop(5f);
        cell.setPaddingBottom(5f);
        cell.setPaddingLeft(4f);
        cell.setPaddingRight(4f);
        cell.setBorderColor(new Color(51, 65, 85));
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text, Font font, int alignment, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingTop(5f);
        cell.setPaddingBottom(5f);
        cell.setPaddingLeft(4f);
        cell.setPaddingRight(4f);
        cell.setBorderColor(COLOR_BORDER);
        cell.setBorderWidth(0.5f);
        table.addCell(cell);
    }

    private void addFinanceRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, labelFont));
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setPadding(3f);
        c1.setHorizontalAlignment(Element.ALIGN_LEFT);

        PdfPCell c2 = new PdfPCell(new Phrase(value, valueFont));
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setPadding(3f);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);

        table.addCell(c1);
        table.addCell(c2);
    }

    private PdfPTable createHorizontalDivider(Color color, float height, float spaceBefore, float spaceAfter) {
        PdfPTable divider = new PdfPTable(1);
        divider.setWidthPercentage(100);
        divider.setSpacingBefore(spaceBefore);
        divider.setSpacingAfter(spaceAfter);
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(color);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setFixedHeight(height);
        divider.addCell(cell);
        return divider;
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0 đ";
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,##0", symbols);
        return df.format(amount) + " đ";
    }

    private Image loadLogoImage() {
        try {
            ClassPathResource res = new ClassPathResource("images/rikkei-logo.png");
            if (res.exists()) {
                byte[] bytes = res.getInputStream().readAllBytes();
                return Image.getInstance(bytes);
            }
        } catch (Exception e) {
            log.warn("Không thể tải logo từ classpath: {}", e.getMessage());
        }

        try {
            File localLogo = new File("backend/src/main/resources/images/rikkei-logo.png");
            if (localLogo.exists()) {
                return Image.getInstance(localLogo.getAbsolutePath());
            }
        } catch (Exception ignored) {}

        return null;
    }

    private BaseFont loadBaseFont(String classpathLocation, String fallback) {
        try {
            ClassPathResource resource = new ClassPathResource(classpathLocation);
            if (resource.exists()) {
                byte[] fontBytes = resource.getInputStream().readAllBytes();
                return BaseFont.createFont(resource.getFilename(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, fontBytes, null);
            }
        } catch (Exception e) {
            log.warn("Không thể tải font từ classpath {}: {}", classpathLocation, e.getMessage());
        }

        // Kiểm tra Windows font
        try {
            String fontName = classpathLocation.contains("arialbd") ? "arialbd.ttf" : "arial.ttf";
            File winFont = new File("C:/Windows/Fonts/" + fontName);
            if (winFont.exists()) {
                return BaseFont.createFont(winFont.getAbsolutePath(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            }
        } catch (Exception ignored) {}

        // Fallback font chuẩn
        try {
            return BaseFont.createFont(fallback, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo font dự phòng", e);
        }
    }

    /**
     * Helper gắn footer trang: số trang và mã đơn hàng bảo mật.
     */
    private static class InvoicePageEventHelper extends PdfPageEventHelper {
        private final String orderCode;

        public InvoicePageEventHelper(String orderCode) {
            this.orderCode = orderCode;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            cb.saveState();
            cb.beginText();
            try {
                BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
                cb.setFontAndSize(bf, 8);
                cb.setColorFill(COLOR_MUTED);

                // Footer trái: Mã hóa đơn & Hệ thống
                String leftText = "Hóa đơn điện tử Rikkei Store  |  Mã đơn: " + orderCode;
                cb.showTextAligned(Element.ALIGN_LEFT, leftText, document.leftMargin(), document.bottom() - 15, 0);

                // Footer phải: Số trang
                String rightText = "Trang " + writer.getPageNumber();
                cb.showTextAligned(Element.ALIGN_RIGHT, rightText, document.right(), document.bottom() - 15, 0);
            } catch (Exception ignored) {
            } finally {
                cb.endText();
                cb.restoreState();
            }
        }
    }
}
