package com.example.ecommerce.config;

import com.example.ecommerce.entity.Category;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.repository.CategoryRepository;
import com.example.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/**
 * Tự động khởi tạo dữ liệu mẫu khi hệ thống có dưới 20 sản phẩm.
 * Phục vụ kiểm thử phân trang dữ liệu lớn (Large Dataset Pagination Test).
 * Đồng thời tự động rà soát và khắc phục hình ảnh sản phẩm bị gán nhầm ảnh đồng hồ.
 */
@Component
@Profile("!test") // Không chạy trong môi trường Unit Test để tránh ảnh hưởng test context
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    private static final String WATCH_IMAGE_FRAGMENT = "photo-1523275335684-37898b6baf30";

    @Override
    public void run(String... args) {
        // 1. Tự động rà soát và sửa ảnh các sản phẩm đã có nếu bị gán nhầm ảnh đồng hồ
        fixExistingMismatchedImages();

        long count = productRepository.count();
        if (count >= 20) {
            log.info("Cơ sở dữ liệu đã có {} sản phẩm, bỏ qua DataInitializer seed data mới.", count);
            return;
        }

        log.info("Phát hiện CSDL chỉ có {} sản phẩm. Đang tự động sinh dữ liệu mẫu kiểm thử phân trang...", count);

        // Đảm bảo có ít nhất các danh mục cơ bản
        Category catFashion = getOrCreateCategory("Thời trang", "Quần áo, giày dép và phụ kiện thời trang");
        Category catElectronics = getOrCreateCategory("Điện tử", "Điện thoại, máy tính, phụ kiện công nghệ");
        Category catHome = getOrCreateCategory("Gia dụng", "Đồ gia dụng, thiết bị tiện ích trong nhà");
        Category catBooks = getOrCreateCategory("Sách", "Sách công nghệ, phát triển bản thân và văn học");
        Category catSports = getOrCreateCategory("Thể thao", "Dụng cụ thể thao, đồ tập gym và ngoài trời");

        List<Product> seedProducts = List.of(
            // Thời trang
            buildProduct("Áo sơ mi Oxford nam", "Vải Oxford cao cấp chống nhăn", "380000",
                    "https://images.unsplash.com/photo-1596755094514-f87e34085b2c?w=600&auto=format&fit=crop&q=80", catFashion, 45),
            buildProduct("Quần tây nam âu Hàn Quốc", "Ống đứng ôm vừa vặn, vải tuyết mưa", "420000",
                    "https://images.unsplash.com/photo-1473966968600-fa801b869a1a?w=600&auto=format&fit=crop&q=80", catFashion, 30),
            buildProduct("Áo khoác Bomber lót gió", "Unisex phong cách streetwear trẻ trung", "550000",
                    "https://images.unsplash.com/photo-1551028719-00167b16eac5?w=600&auto=format&fit=crop&q=80", catFashion, 20),
            buildProduct("Chân váy xếp ly nữ công sở", "Xếp ly tinh tế, cạp cao co giãn nhẹ", "320000",
                    "https://images.unsplash.com/photo-1583496661160-fb5886a0aaaa?w=600&auto=format&fit=crop&q=80", catFashion, 35),
            buildProduct("Áo len cổ lọ dệt kim", "Chất len mềm mịn không xù lông", "290000",
                    "https://images.unsplash.com/photo-1576566588028-4147f3842f27?w=600&auto=format&fit=crop&q=80", catFashion, 50),
            buildProduct("Đầm suông lụa satin", "Lụa bóng sang trọng tôn dáng", "680000",
                    "https://images.unsplash.com/photo-1595777457583-95e059d581b8?w=600&auto=format&fit=crop&q=80", catFashion, 15),
            buildProduct("Quần short kaki nam", "Kaki dày dặn thoáng mát dạo phố", "250000",
                    "https://images.unsplash.com/photo-1591195853828-11db59a44f6b?w=600&auto=format&fit=crop&q=80", catFashion, 60),
            buildProduct("Giày lười da bò Slip-on", "Da bò thật 100% đế cao su đúc", "950000",
                    "https://images.unsplash.com/photo-1560343090-f0409e92791a?w=600&auto=format&fit=crop&q=80", catFashion, 18),
            buildProduct("Thắt lưng da nam khóa kim", "Khóa hợp kim mạ crom sáng bóng", "220000",
                    "https://images.unsplash.com/photo-1624222247344-550fb60583dc?w=600&auto=format&fit=crop&q=80", catFashion, 40),
            buildProduct("Áo polo thể thao tổ ong", "Thấm hút mồ hôi siêu tốc", "270000",
                    "https://images.unsplash.com/photo-1581655353564-df123a1eb820?w=600&auto=format&fit=crop&q=80", catFashion, 75),

            // Điện tử
            buildProduct("Màn hình Gaming 27 inch 165Hz", "2K QHD, Fast IPS 1ms phản hồi cực nhạy", "5200000",
                    "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=600&auto=format&fit=crop&q=80", catElectronics, 14),
            buildProduct("Loa Bluetooth di động IPX7", "Công suất 30W bass mạnh mẽ, pin 12h", "990000",
                    "https://images.unsplash.com/photo-1545454675-3531b543be5d?w=600&auto=format&fit=crop&q=80", catElectronics, 28),
            buildProduct("Ổ cứng SSD di động 1TB Type-C", "Tốc độ đọc ghi 1050MB/s chống sốc", "2150000",
                    "https://images.unsplash.com/photo-1597872200969-2b65d56bd16b?w=600&auto=format&fit=crop&q=80", catElectronics, 22),
            buildProduct("Đồng hồ thông minh theo dõi nhịp tim", "Màn hình AMOLED, đo SpO2 kháng nước 5ATM", "1450000",
                    "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600&auto=format&fit=crop&q=80", catElectronics, 35),
            buildProduct("Củ sạc nhanh GaN 65W 3 cổng", "Công nghệ GaN nhỏ gọn sạc cho cả laptop", "450000",
                    "https://images.unsplash.com/photo-1583863788434-e58a36330cf0?w=600&auto=format&fit=crop&q=80", catElectronics, 60),
            buildProduct("Cáp sạc Type-C to Lightning bọc dù", "Chuẩn Apple MFi sạc nhanh PD 27W", "180000",
                    "https://images.unsplash.com/photo-1583863788434-e58a36330cf0?w=600&auto=format&fit=crop&q=80", catElectronics, 90),
            buildProduct("Giá đỡ kiêm sạc không dây MagSafe", "Nam châm hít chắc chắn sạc nhanh 15W", "380000",
                    "https://images.unsplash.com/photo-1586953208448-b95a79798f07?w=600&auto=format&fit=crop&q=80", catElectronics, 45),
            buildProduct("Hub chuyển đổi USB-C 8 trong 1", "HDMI 4K, thẻ SD, USB 3.0, PD 100W", "580000",
                    "https://images.unsplash.com/photo-1544652478-6653e09f18a2?w=600&auto=format&fit=crop&q=80", catElectronics, 32),
            buildProduct("Micro thu âm Podcast lọc ồn USB", "Màng loa Cardioid kiểm âm 3.5mm", "850000",
                    "https://images.unsplash.com/photo-1590602847861-f357a9332bbc?w=600&auto=format&fit=crop&q=80", catElectronics, 19),
            buildProduct("Đèn LED kẹp màn hình bảo vệ mắt", "Không phản chiếu màn hình chống mỏi mắt", "490000",
                    "https://images.unsplash.com/photo-1513506003901-1e6a229e2d15?w=600&auto=format&fit=crop&q=80", catElectronics, 50),

            // Gia dụng
            buildProduct("Nồi chiên không dầu điện tử 6.5L", "Rapid Air giảm 85% dầu mỡ thừa", "1650000",
                    "https://images.unsplash.com/photo-1584269600464-37b1b58a9fe7?w=600&auto=format&fit=crop&q=80", catHome, 25),
            buildProduct("Máy xay sinh tố cầm tay đa năng", "Lưỡi dao inox 304 công suất 600W", "590000",
                    "https://images.unsplash.com/photo-1570222094114-d054a817e56b?w=600&auto=format&fit=crop&q=80", catHome, 30),
            buildProduct("Máy hút bụi mini cầm tay không dây", "Lực hút 12000Pa dọn dẹp nội thất ô tô", "450000",
                    "https://images.unsplash.com/photo-1558317374-067fb5f30001?w=600&auto=format&fit=crop&q=80", catHome, 40),
            buildProduct("Bàn là hơi nước cầm tay gấp gọn", "Áp suất cao ủi phẳng quần áo trong 20s", "390000",
                    "https://images.unsplash.com/photo-1517677208171-0bc6725a3e60?w=600&auto=format&fit=crop&q=80", catHome, 35),
            buildProduct("Bộ hộp thủy tinh Borosilicate (Set 3)", "Dùng an toàn cho lò vi sóng và máy rửa bát", "280000",
                    "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=600&auto=format&fit=crop&q=80", catHome, 50),
            buildProduct("Ấm đun nước siêu tốc inox 1.7L", "Công suất 1800W tự ngắt an toàn", "220000",
                    "https://images.unsplash.com/photo-1594212699903-ec8a3eca50f5?w=600&auto=format&fit=crop&q=80", catHome, 60),
            buildProduct("Quạt tích điện để bàn 4000mAh", "Pin 8 tiếng êm ái xoay tự động", "310000",
                    "https://images.unsplash.com/photo-1618941716939-553df3c6c278?w=600&auto=format&fit=crop&q=80", catHome, 45),
            buildProduct("Đèn bàn học chống cận thị LED", "Ánh sáng tự nhiên không nhấp nháy", "260000",
                    "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=600&auto=format&fit=crop&q=80", catHome, 40),

            // Sách
            buildProduct("Refactoring - Cải tiến mã nguồn", "Tác phẩm kinh điển của Martin Fowler", "210000",
                    "https://images.unsplash.com/photo-1532012164546-f432f2e3edd4?w=600&auto=format&fit=crop&q=80", catBooks, 30),
            buildProduct("Designing Data-Intensive Applications", "Kiến trúc hệ thống dữ liệu phân tán quy mô lớn", "450000",
                    "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=600&auto=format&fit=crop&q=80", catBooks, 18),
            buildProduct("Hành trình về Phương Đông", "Triết học cổ xưa về văn hóa phương Đông", "125000",
                    "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=600&auto=format&fit=crop&q=80", catBooks, 60),
            buildProduct("Đắc Nhân Tâm", "Nghệ thuật giao tiếp và thuyết phục đỉnh cao", "98000",
                    "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=600&auto=format&fit=crop&q=80", catBooks, 90),
            buildProduct("Nhà Giả Kim", "Truyền cảm hứng theo đuổi ước mơ chân chính", "89000",
                    "https://images.unsplash.com/photo-1543002588-bfa74002ed7e?w=600&auto=format&fit=crop&q=80", catBooks, 110),
            buildProduct("Tư Duy Nhanh Và Chậm", "Hai hệ thống tư duy chi phối quyết định", "195000",
                    "https://images.unsplash.com/photo-1589829085413-56de8ae18c73?w=600&auto=format&fit=crop&q=80", catBooks, 25),

            // Thể thao
            buildProduct("Dây nhảy thể lực đếm số tự động", "Màn hình LCD đếm vòng nhảy và calo", "150000",
                    "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=600&auto=format&fit=crop&q=80", catSports, 60),
            buildProduct("Bóng rổ tiêu chuẩn thi đấu Size 7", "Da PU bám dính tốt chống trơn trượt", "320000",
                    "https://images.unsplash.com/photo-1519861531473-9200262188bf?w=600&auto=format&fit=crop&q=80", catSports, 25),
            buildProduct("Quả bóng đá số 5 may tay", "Da nhân tạo siêu bền bay chuẩn quỹ đạo", "280000",
                    "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=600&auto=format&fit=crop&q=80", catSports, 30),
            buildProduct("Kính bơi chống đọng sương tráng gương", "Chống tia UV viền silicon ôm sát", "180000",
                    "https://images.unsplash.com/photo-1530549387789-4c1017266635?w=600&auto=format&fit=crop&q=80", catSports, 45),
            buildProduct("Con lăn tập cơ bụng 4 bánh", "Khung kim loại chịu lực 200kg chống lật", "210000",
                    "https://images.unsplash.com/photo-1584735935682-2f2b69dff9d2?w=600&auto=format&fit=crop&q=80", catSports, 35),
            buildProduct("Bộ dây kháng lực ngũ sắc tập gym", "5 mức kháng lực từ 10lbs đến 50lbs", "199000",
                    "https://images.unsplash.com/photo-1598289431512-b97b0917affc?w=600&auto=format&fit=crop&q=80", catSports, 50)
        );

        productRepository.saveAll(seedProducts);
        log.info("Đã khởi tạo thành công {} sản phẩm mẫu vào CSDL với ảnh chính xác!", seedProducts.size());
    }

    /**
     * Tự động quét và sửa các sản phẩm đang lưu trong DB có ảnh là đồng hồ nhưng tên không phải đồng hồ.
     */
    private void fixExistingMismatchedImages() {
        try {
            List<Product> allProducts = productRepository.findAll();
            int updatedCount = 0;

            for (Product p : allProducts) {
                if (p.getImageUrl() != null && p.getImageUrl().contains(WATCH_IMAGE_FRAGMENT)) {
                    String nameLower = p.getName().toLowerCase(Locale.ROOT);
                    if (!nameLower.contains("đồng hồ") && !nameLower.contains("dong ho") && !nameLower.contains("watch")) {
                        String accurateUrl = resolveAccurateImageUrl(p.getName(), p.getCategory() != null ? p.getCategory().getName() : "");
                        p.setImageUrl(accurateUrl);
                        updatedCount++;
                    }
                }
            }

            if (updatedCount > 0) {
                productRepository.saveAll(allProducts);
                log.info("Đã tự động khắc phục hình ảnh cho {} sản phẩm bị nhầm ảnh đồng hồ trong CSDL.", updatedCount);
            }
        } catch (Exception ex) {
            log.warn("Lỗi khi quét và sửa ảnh sản phẩm: {}", ex.getMessage());
        }
    }

    /**
     * Xác định URL ảnh chính xác dựa trên tên sản phẩm và tên danh mục.
     */
    public static String resolveAccurateImageUrl(String productName, String categoryName) {
        String name = productName != null ? productName.toLowerCase(Locale.ROOT) : "";
        String cat = categoryName != null ? categoryName.toLowerCase(Locale.ROOT) : "";

        // Thời trang
        if (name.contains("áo sơ mi") || name.contains("oxford")) {
            return "https://images.unsplash.com/photo-1596755094514-f87e34085b2c?w=600&auto=format&fit=crop&q=80";
        }
        if (name.contains("quần tây") || name.contains("quần jean") || name.contains("quần short")) {
            return "https://images.unsplash.com/photo-1473966968600-fa801b869a1a?w=600&auto=format&fit=crop&q=80";
        }
        if (name.contains("áo khoác") || name.contains("bomber") || name.contains("hoodie")) {
            return "https://images.unsplash.com/photo-1551028719-00167b16eac5?w=600&auto=format&fit=crop&q=80";
        }
        if (name.contains("chân váy") || name.contains("váy") || name.contains("đầm")) {
            return "https://images.unsplash.com/photo-1583496661160-fb5886a0aaaa?w=600&auto=format&fit=crop&q=80";
        }
        if (name.contains("áo len")) {
            return "https://images.unsplash.com/photo-1576566588028-4147f3842f27?w=600&auto=format&fit=crop&q=80";
        }
        if (name.contains("giày")) {
            return "https://images.unsplash.com/photo-1560343090-f0409e92791a?w=600&auto=format&fit=crop&q=80";
        }
        if (name.contains("thắt lưng")) {
            return "https://images.unsplash.com/photo-1624222247344-550fb60583dc?w=600&auto=format&fit=crop&q=80";
        }
        if (name.contains("áo polo") || name.contains("áo thun")) {
            return "https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=600&auto=format&fit=crop&q=80";
        }

        // Điện tử
        if (name.contains("màn hình") || name.contains("monitor")) {
            return "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=600&auto=format&fit=crop&q=80";
        }
        if (name.contains("loa") || name.contains("speaker")) {
            return "https://images.unsplash.com/photo-1545454675-3531b543be5d?w=600&auto=format&fit=crop&q=80";
        }
        if (name.contains("ổ cứng") || name.contains("ssd")) {
            return "https://images.unsplash.com/photo-1597872200969-2b65d56bd16b?w=600&auto=format&fit=crop&q=80";
        }
        if (name.contains("sạc") || name.contains("cáp")) {
            return "https://images.unsplash.com/photo-1583863788434-e58a36330cf0?w=600&auto=format&fit=crop&q=80";
        }
        if (name.contains("giá đỡ") || name.contains("magsafe")) {
            return "https://images.unsplash.com/photo-1586953208448-b95a79798f07?w=600&auto=format&fit=crop&q=80";
        }
        if (name.contains("hub") || name.contains("chuyển đổi")) {
            return "https://images.unsplash.com/photo-1544652478-6653e09f18a2?w=600&auto=format&fit=crop&q=80";
        }
        if (name.contains("micro") || name.contains("mic")) {
            return "https://images.unsplash.com/photo-1590602847861-f357a9332bbc?w=600&auto=format&fit=crop&q=80";
        }
        if (name.contains("đèn led") || name.contains("đèn")) {
            return "https://images.unsplash.com/photo-1513506003901-1e6a229e2d15?w=600&auto=format&fit=crop&q=80";
        }
        if (name.contains("tai nghe")) {
            return "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600&auto=format&fit=crop&q=80";
        }
        if (name.contains("bàn phím")) {
            return "https://images.unsplash.com/photo-1618384887929-16ec33fab9ef?w=600&auto=format&fit=crop&q=80";
        }
        if (name.contains("chuột")) {
            return "https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7?w=600&auto=format&fit=crop&q=80";
        }

        // Gia dụng
        if (name.contains("nồi chiên") || name.contains("nồi cơm") || name.contains("nồi")) {
            return "https://images.unsplash.com/photo-1584269600464-37b1b58a9fe7?w=600&auto=format&fit=crop&q=80";
        }
        if (name.contains("xay") || name.contains("sinh tố")) {
            return "https://images.unsplash.com/photo-1570222094114-d054a817e56b?w=600&auto=format&fit=crop&q=80";
        }
        if (name.contains("hút bụi")) {
            return "https://images.unsplash.com/photo-1558317374-067fb5f30001?w=600&auto=format&fit=crop&q=80";
        }
        if (name.contains("bàn là") || name.contains("bàn ủi")) {
            return "https://images.unsplash.com/photo-1517677208171-0bc6725a3e60?w=600&auto=format&fit=crop&q=80";
        }
        if (name.contains("thủy tinh") || name.contains("hộp")) {
            return "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=600&auto=format&fit=crop&q=80";
        }
        if (name.contains("ấm") || name.contains("siêu tốc") || name.contains("bình")) {
            return "https://images.unsplash.com/photo-1594212699903-ec8a3eca50f5?w=600&auto=format&fit=crop&q=80";
        }
        if (name.contains("quạt")) {
            return "https://images.unsplash.com/photo-1618941716939-553df3c6c278?w=600&auto=format&fit=crop&q=80";
        }

        // Sách
        if (cat.contains("sách") || name.contains("refactoring") || name.contains("thiết kế") || name.contains("tâm") || name.contains("sách")) {
            return "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=600&auto=format&fit=crop&q=80";
        }

        // Thể thao
        if (name.contains("bóng rổ")) {
            return "https://images.unsplash.com/photo-1519861531473-9200262188bf?w=600&auto=format&fit=crop&q=80";
        }
        if (name.contains("bóng đá") || name.contains("bóng")) {
            return "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=600&auto=format&fit=crop&q=80";
        }
        if (name.contains("bơi") || name.contains("kính bơi")) {
            return "https://images.unsplash.com/photo-1530549387789-4c1017266635?w=600&auto=format&fit=crop&q=80";
        }
        if (name.contains("bụng") || name.contains("tạ") || name.contains("dây kháng lực") || name.contains("thảm")) {
            return "https://images.unsplash.com/photo-1584735935682-2f2b69dff9d2?w=600&auto=format&fit=crop&q=80";
        }

        // Fallback theo danh mục
        if (cat.contains("thời trang")) {
            return "https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=600&auto=format&fit=crop&q=80";
        }
        if (cat.contains("điện tử")) {
            return "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600&auto=format&fit=crop&q=80";
        }
        if (cat.contains("gia dụng")) {
            return "https://images.unsplash.com/photo-1556911220-e15b29be8c8f?w=600&auto=format&fit=crop&q=80";
        }
        if (cat.contains("sách")) {
            return "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=600&auto=format&fit=crop&q=80";
        }
        if (cat.contains("thể thao")) {
            return "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=600&auto=format&fit=crop&q=80";
        }

        return "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600&auto=format&fit=crop&q=80";
    }

    private Category getOrCreateCategory(String name, String description) {
        return categoryRepository.findAll().stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> categoryRepository.save(
                        Category.builder()
                                .name(name)
                                .description(description)
                                .active(true)
                                .build()
                ));
    }

    private Product buildProduct(String name, String description, String priceStr, String imageUrl, Category category, int stock) {
        return Product.builder()
                .name(name)
                .description(description)
                .price(new BigDecimal(priceStr))
                .imageUrl(imageUrl)
                .category(category)
                .stockQuantity(stock)
                .active(true)
                .build();
    }
}
