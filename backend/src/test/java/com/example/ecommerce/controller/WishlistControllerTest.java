package com.example.ecommerce.controller;

import com.example.ecommerce.dto.response.WishlistItemResponse;
import com.example.ecommerce.dto.response.WishlistToggleResponse;
import com.example.ecommerce.service.WishlistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller Test cho WishlistController.
 * Phụ trách: Đỗ Đức Mạnh (mnhd1)
 * Task ID: [TSK-D15-DDM] Xây Dựng Phân Hệ Sản Phẩm Yêu Thích (Wishlist Backend & Frontend)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb_wishlist_ctrl;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=test-secret-key-for-unit-tests-must-be-at-least-32-characters-long",
    "jwt.expiration=3600000"
})
class WishlistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WishlistService wishlistService;

    @Test
    @DisplayName("GET /api/v1/wishlist - Yêu cầu xác thực (Chưa đăng nhập -> 401)")
    void getWishlist_unauthorized_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/wishlist")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    @DisplayName("GET /api/v1/wishlist - Trả về danh sách sản phẩm yêu thích (200 OK)")
    void getWishlist_authenticated_shouldReturnWishlist() throws Exception {
        WishlistItemResponse item = WishlistItemResponse.builder()
                .id(100L)
                .name("Tai nghe Sony WH-1000XM5")
                .price(new BigDecimal("7490000"))
                .discountPrice(new BigDecimal("7490000"))
                .imageUrl("https://example.com/sony.jpg")
                .ratingAverage(4.9)
                .stockQuantity(15)
                .addedAt(LocalDateTime.now())
                .build();

        when(wishlistService.getWishlist()).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/wishlist")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(100))
                .andExpect(jsonPath("$.data[0].name").value("Tai nghe Sony WH-1000XM5"))
                .andExpect(jsonPath("$.data[0].price").value(7490000));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    @DisplayName("GET /api/v1/wishlist/ids - Trả về danh sách productIds đã thích (200 OK)")
    void getWishlistProductIds_authenticated_shouldReturnIds() throws Exception {
        when(wishlistService.getWishlistProductIds()).thenReturn(List.of(100L, 200L, 300L));

        mockMvc.perform(get("/api/v1/wishlist/ids")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0]").value(100))
                .andExpect(jsonPath("$.data[1]").value(200))
                .andExpect(jsonPath("$.data[2]").value(300));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    @DisplayName("POST /api/v1/wishlist/toggle/{productId} - Toggle thích sản phẩm (200 OK)")
    void toggleWishlist_authenticated_shouldToggle() throws Exception {
        WishlistToggleResponse toggleResponse = WishlistToggleResponse.builder()
                .liked(true)
                .message("Đã thêm vào danh sách yêu thích")
                .build();

        when(wishlistService.toggleWishlist(100L)).thenReturn(toggleResponse);

        mockMvc.perform(post("/api/v1/wishlist/toggle/100")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.message").value("Đã thêm vào danh sách yêu thích"));
    }
}

