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

/**
 * Tự động khởi tạo dữ liệu mẫu khi hệ thống có dưới 20 sản phẩm.
 * Phục vụ kiểm thử phân trang dữ liệu lớn (Large Dataset Pagination Test).
 * Phụ trách: Đỗ Đức Mạnh (DDM)
 * Task ID: [TSK-D10-DDM] Loại bỏ Mock Data Module Sản phẩm & Kiểm thử Phân trang Dữ liệu lớn
 */
@Component
@Profile("!test") // Không chạy trong môi trường Unit Test để tránh ảnh hưởng test context
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public void run(String... args) {
        long count = productRepository.count();
        if (count >= 20) {
            log.info("Cơ sở dữ liệu đã có {} sản phẩm, bỏ qua DataInitializer seed data.", count);
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
            buildProduct("Áo sơ mi Oxford nam", "Vải Oxford cao cấp chống nhăn", "380000", catFashion, 45),
            buildProduct("Quần tây nam âu Hàn Quốc", "Ống đứng ôm vừa vặn, vải tuyết mưa", "420000", catFashion, 30),
            buildProduct("Áo khoác Bomber lót gió", "Unisex phong cách streetwear trẻ trung", "550000", catFashion, 20),
            buildProduct("Chân váy xếp ly nữ công sở", "Xếp ly tinh tế, cạp cao co giãn nhẹ", "320000", catFashion, 35),
            buildProduct("Áo len cổ lọ dệt kim", "Chất len mềm mịn không xù lông", "290000", catFashion, 50),
            buildProduct("Đầm suông lụa satin", "Lụa bóng sang trọng tôn dáng", "680000", catFashion, 15),
            buildProduct("Quần short kaki nam", "Kaki dày dặn thoáng mát dạo phố", "250000", catFashion, 60),
            buildProduct("Giày lười da bò Slip-on", "Da bò thật 100% đế cao su đúc", "950000", catFashion, 18),
            buildProduct("Thắt lưng da nam khóa kim", "Khóa hợp kim mạ crom sáng bóng", "220000", catFashion, 40),
            buildProduct("Áo polo thể thao tổ ong", "Thấm hút mồ hôi siêu tốc", "270000", catFashion, 75),

            // Điện tử
            buildProduct("Màn hình Gaming 27 inch 165Hz", "2K QHD, Fast IPS 1ms phản hồi cực nhạy", "5200000", catElectronics, 14),
            buildProduct("Loa Bluetooth di động IPX7", "Công suất 30W bass mạnh mẽ, pin 12h", "990000", catElectronics, 28),
            buildProduct("Ổ cứng SSD di động 1TB Type-C", "Tốc độ đọc ghi 1050MB/s chống sốc", "2150000", catElectronics, 22),
            buildProduct("Đồng hồ thông minh theo dõi nhịp tim", "Màn hình AMOLED, đo SpO2 kháng nước 5ATM", "1450000", catElectronics, 35),
            buildProduct("Củ sạc nhanh GaN 65W 3 cổng", "Công nghệ GaN nhỏ gọn sạc cho cả laptop", "450000", catElectronics, 60),
            buildProduct("Cáp sạc Type-C to Lightning bọc dù", "Chuẩn Apple MFi sạc nhanh PD 27W", "180000", catElectronics, 90),
            buildProduct("Giá đỡ kiêm sạc không dây MagSafe", "Nam châm hít chắc chắn sạc nhanh 15W", "380000", catElectronics, 45),
            buildProduct("Hub chuyển đổi USB-C 8 trong 1", "HDMI 4K, thẻ SD, USB 3.0, PD 100W", "580000", catElectronics, 32),
            buildProduct("Micro thu âm Podcast lọc ồn USB", "Màng loa Cardioid kiểm âm 3.5mm", "850000", catElectronics, 19),
            buildProduct("Đèn LED kẹp màn hình bảo vệ mắt", "Không phản chiếu màn hình chống mỏi mắt", "490000", catElectronics, 50),

            // Gia dụng
            buildProduct("Nồi chiên không dầu điện tử 6.5L", "Rapid Air giảm 85% dầu mỡ thừa", "1650000", catHome, 25),
            buildProduct("Máy xay sinh tố cầm tay đa năng", "Lưỡi dao inox 304 công suất 600W", "590000", catHome, 30),
            buildProduct("Máy hút bụi mini cầm tay không dây", "Lực hút 12000Pa dọn dẹp nội thất ô tô", "450000", catHome, 40),
            buildProduct("Bàn là hơi nước cầm tay gấp gọn", "Áp suất cao ủi phẳng quần áo trong 20s", "390000", catHome, 35),
            buildProduct("Bộ hộp thủy tinh Borosilicate (Set 3)", "Dùng an toàn cho lò vi sóng và máy rửa bát", "280000", catHome, 50),
            buildProduct("Ấm đun nước siêu tốc inox 1.7L", "Công suất 1800W tự ngắt an toàn", "220000", catHome, 60),
            buildProduct("Quạt tích điện để bàn 4000mAh", "Pin 8 tiếng êm ái xoay tự động", "310000", catHome, 45),
            buildProduct("Đèn bàn học chống cận thị LED", "Ánh sáng tự nhiên không nhấp nháy", "260000", catHome, 40),

            // Sách
            buildProduct("Refactoring - Cải tiến mã nguồn", "Tác phẩm kinh điển của Martin Fowler", "210000", catBooks, 30),
            buildProduct("Designing Data-Intensive Applications", "Kiến trúc hệ thống dữ liệu phân tán quy mô lớn", "450000", catBooks, 18),
            buildProduct("Hành trình về Phương Đông", "Triết học cổ xưa về văn hóa phương Đông", "125000", catBooks, 60),
            buildProduct("Đắc Nhân Tâm", "Nghệ thuật giao tiếp và thuyết phục đỉnh cao", "98000", catBooks, 90),
            buildProduct("Nhà Giả Kim", "Truyền cảm hứng theo đuổi ước mơ chân chính", "89000", catBooks, 110),
            buildProduct("Tư Duy Nhanh Và Chậm", "Hai hệ thống tư duy chi phối quyết định", "195000", catBooks, 25),

            // Thể thao
            buildProduct("Dây nhảy thể lực đếm số tự động", "Màn hình LCD đếm vòng nhảy và calo", "150000", catSports, 60),
            buildProduct("Bóng rổ tiêu chuẩn thi đấu Size 7", "Da PU bám dính tốt chống trơn trượt", "320000", catSports, 25),
            buildProduct("Quả bóng đá số 5 may tay", "Da nhân tạo siêu bền bay chuẩn quỹ đạo", "280000", catSports, 30),
            buildProduct("Kính bơi chống đọng sương tráng gương", "Chống tia UV viền silicon ôm sát", "180000", catSports, 45),
            buildProduct("Con lăn tập cơ bụng 4 bánh", "Khung kim loại chịu lực 200kg chống lật", "210000", catSports, 35),
            buildProduct("Bộ dây kháng lực ngũ sắc tập gym", "5 mức kháng lực từ 10lbs đến 50lbs", "199000", catSports, 50)
        );

        productRepository.saveAll(seedProducts);
        log.info("Đã khởi tạo thành công {} sản phẩm mẫu vào CSDL để phục vụ kiểm thử phân trang dữ liệu lớn!", seedProducts.size());
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

    private Product buildProduct(String name, String description, String priceStr, Category category, int stock) {
        return Product.builder()
                .name(name)
                .description(description)
                .price(new BigDecimal(priceStr))
                .imageUrl("https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600&auto=format&fit=crop&q=80")
                .category(category)
                .stockQuantity(stock)
                .active(true)
                .build();
    }
}

