package com.example.ecommerce.chat.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit Test cho các Model nội bộ của Chatbot: ChatMessage và ChatSessionMetadata.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Tích hợp Google Gemini Client & Xây dựng API Chatbot Tư Vấn Bán Hàng.
 *
 * Lưu ý: Package com.example.ecommerce.chat.model không nằm trong danh sách loại trừ của JaCoCo,
 * do đó toàn bộ constructor, getters, setters, builder và helper methods đều được kiểm thử đầy đủ.
 */
class ChatModelTest {

    @Test
    @DisplayName("MODEL-01: ChatMessage khởi tạo thành công qua Builder và Getter hoạt động chính xác")
    void testChatMessage_BuilderAndGetters() {
        LocalDateTime now = LocalDateTime.now();
        ChatMessage message = ChatMessage.builder()
                .role("user")
                .content("Xin chào shop")
                .timestamp(now)
                .build();

        assertThat(message).isNotNull();
        assertThat(message.getRole()).isEqualTo("user");
        assertThat(message.getContent()).isEqualTo("Xin chào shop");
        assertThat(message.getTimestamp()).isEqualTo(now);
    }

    @Test
    @DisplayName("MODEL-02: ChatMessage NoArgsConstructor và Setters hoạt động chính xác")
    void testChatMessage_NoArgsConstructorAndSetters() {
        LocalDateTime now = LocalDateTime.now();
        ChatMessage message = new ChatMessage();
        message.setRole("assistant");
        message.setContent("Chào bạn, tôi là trợ lý ảo");
        message.setTimestamp(now);

        assertThat(message.getRole()).isEqualTo("assistant");
        assertThat(message.getContent()).isEqualTo("Chào bạn, tôi là trợ lý ảo");
        assertThat(message.getTimestamp()).isEqualTo(now);
    }

    @Test
    @DisplayName("MODEL-03: ChatMessage các phương thức factory of, ofUser, ofAssistant, ofSystem tạo đúng role")
    void testChatMessage_FactoryMethods() {
        ChatMessage customMsg = ChatMessage.of("developer", "System test");
        assertThat(customMsg.getRole()).isEqualTo("developer");
        assertThat(customMsg.getContent()).isEqualTo("System test");
        assertThat(customMsg.getTimestamp()).isNotNull();

        ChatMessage userMsg = ChatMessage.ofUser("Hỏi về giá áo thun");
        assertThat(userMsg.getRole()).isEqualTo("user");
        assertThat(userMsg.getContent()).isEqualTo("Hỏi về giá áo thun");
        assertThat(userMsg.getTimestamp()).isNotNull();

        ChatMessage assistantMsg = ChatMessage.ofAssistant("Áo thun có giá 150.000 VNĐ");
        assertThat(assistantMsg.getRole()).isEqualTo("assistant");
        assertThat(assistantMsg.getContent()).isEqualTo("Áo thun có giá 150.000 VNĐ");
        assertThat(assistantMsg.getTimestamp()).isNotNull();

        ChatMessage systemMsg = ChatMessage.ofSystem("Bạn là tư vấn viên bán hàng Rikkei Store");
        assertThat(systemMsg.getRole()).isEqualTo("system");
        assertThat(systemMsg.getContent()).isEqualTo("Bạn là tư vấn viên bán hàng Rikkei Store");
        assertThat(systemMsg.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("MODEL-04: ChatMessage AllArgsConstructor, equals, hashCode và toString hoạt động đúng")
    void testChatMessage_EqualsHashCodeToString() {
        LocalDateTime now = LocalDateTime.now();
        ChatMessage msg1 = new ChatMessage("user", "Hello", now);
        ChatMessage msg2 = new ChatMessage("user", "Hello", now);
        ChatMessage msg3 = new ChatMessage("assistant", "Hi", now);

        assertThat(msg1).isEqualTo(msg2);
        assertThat(msg1.hashCode()).isEqualTo(msg2.hashCode());
        assertThat(msg1).isNotEqualTo(msg3);
        assertThat(msg1.toString()).contains("role=user", "content=Hello");
    }

    @Test
    @DisplayName("MODEL-05: ChatSessionMetadata khởi tạo thành công qua Builder và kiểm tra kiểu dữ liệu ownerId, version")
    void testChatSessionMetadata_BuilderAndGetters() {
        LocalDateTime created = LocalDateTime.now().minusMinutes(5);
        LocalDateTime updated = LocalDateTime.now();
        LocalDateTime expires = LocalDateTime.now().plusHours(24);

        ChatSessionMetadata metadata = ChatSessionMetadata.builder()
                .sessionId("550e8400-e29b-41d4-a716-446655440000")
                .ownerId("GUEST")
                .version(1L)
                .createdAt(created)
                .updatedAt(updated)
                .expiresAt(expires)
                .build();

        assertThat(metadata).isNotNull();
        assertThat(metadata.getSessionId()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
        assertThat(metadata.getOwnerId()).isInstanceOf(String.class).isEqualTo("GUEST");
        assertThat(metadata.getVersion()).isInstanceOf(Long.class).isEqualTo(1L);
        assertThat(metadata.getCreatedAt()).isEqualTo(created);
        assertThat(metadata.getUpdatedAt()).isEqualTo(updated);
        assertThat(metadata.getExpiresAt()).isEqualTo(expires);
    }

    @Test
    @DisplayName("MODEL-06: ChatSessionMetadata NoArgsConstructor, Setters và lưu trữ userId dưới dạng String an toàn")
    void testChatSessionMetadata_UserOwnerIdAsString() {
        LocalDateTime now = LocalDateTime.now();
        ChatSessionMetadata metadata = new ChatSessionMetadata();
        metadata.setSessionId("6ba7b810-9dad-11d1-80b4-00c04fd430c8");
        metadata.setOwnerId(String.valueOf(9999999999L)); // Kiểm tra không bị tràn số như tonumber() trong Lua
        metadata.setVersion(15L);
        metadata.setCreatedAt(now);
        metadata.setUpdatedAt(now);
        metadata.setExpiresAt(now.plusDays(1));

        assertThat(metadata.getOwnerId()).isEqualTo("9999999999");
        assertThat(metadata.getVersion()).isEqualTo(15L);
    }

    @Test
    @DisplayName("MODEL-07: ChatSessionMetadata AllArgsConstructor, equals, hashCode và toString hoạt động đúng")
    void testChatSessionMetadata_EqualsHashCodeToString() {
        LocalDateTime now = LocalDateTime.now();
        ChatSessionMetadata meta1 = new ChatSessionMetadata("session-1", "GUEST", 1L, now, now, now);
        ChatSessionMetadata meta2 = new ChatSessionMetadata("session-1", "GUEST", 1L, now, now, now);
        ChatSessionMetadata meta3 = new ChatSessionMetadata("session-2", "123", 2L, now, now, now);

        assertThat(meta1).isEqualTo(meta2);
        assertThat(meta1.hashCode()).isEqualTo(meta2.hashCode());
        assertThat(meta1).isNotEqualTo(meta3);
        assertThat(meta1.toString()).contains("sessionId=session-1", "ownerId=GUEST", "version=1");
    }
}

