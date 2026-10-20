package com.example.ecommerce.controller;

import com.example.ecommerce.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health check endpoint — dùng để kiểm tra backend đang hoạt động.
 * Hỗ trợ cả /api/health và /api/v1/health.
 */
@RestController
public class HealthController {

    @GetMapping({"/api/health", "/api/v1/health"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkHealth() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("status", "UP");
        details.put("service", "E-commerce Platform Backend");
        details.put("version", "1.0.0");
        details.put("message", "Backend is healthy and running!");

        return ResponseEntity.ok(ApiResponse.success("Hệ thống hoạt động bình thường", details));
    }
}
